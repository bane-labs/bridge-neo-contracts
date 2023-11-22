package network.bane;

import io.neow3j.devpack.*;
import io.neow3j.devpack.annotations.*;
import io.neow3j.devpack.constants.NamedCurve;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.contracts.CryptoLib;
import io.neow3j.devpack.contracts.GasToken;
import io.neow3j.devpack.events.Event1Arg;
import io.neow3j.devpack.events.Event3Args;
import io.neow3j.devpack.events.Event6Args;
import network.bane.interfaces.BridgeManagement;
import network.bane.structs.BridgeDeploymentData;
import network.bane.structs.MerkleProof;

import static io.neow3j.devpack.Helper.*;
import static io.neow3j.devpack.Runtime.*;

@DisplayName("BaneBridge")
@ManifestExtra(key = "author", value = "BaneLabs")
@ManifestExtra(key = "description", value = "Contract for bridging GAS tokens from Neo N3 to Bane.")
public class BridgeContract {

    private static final StorageContext ctx = Storage.getStorageContext();
    private static final CryptoLib cryptoLib = new CryptoLib();

    // region storage keys

    private static final byte prefix_base = 0x0a;
    private static final StorageMap baseMap = new StorageMap(ctx, prefix_base);
    private static final byte prefix_deposit_root = 0x0b; // Used to store incomplete subtree hashes
    private static final byte prefix_claimable = 0x0c;

    private static final int key_bridgeManagement = 0x01;
    private static final int key_deposit_price = 0x02;
    private static final int key_deposit_min = 0x03;
    private static final int key_deposit_max = 0x04;
//    private static final int key_max_proofs_per_withdrawal = 0x05;
    //    private static final int key_locked = 0x06;

    private static final int key_deposit_root = 0x10;
    private static final int key_deposit_nonce = 0x11;
    private static final int key_deposit_maxDepth_current = 0x12;

    private static final int key_withdrawal_root = 0x20;
    private static final int key_withdrawal_nonce = 0x21;

    // In order to arrive the same result in EVM as in NeoVM, the nonce and amount need to be padded to 8 bytes.
    private static final byte const_nonce_padding_bytes = 8;
    private static final byte const_amount_padding_bytes = 8;

    // endregion
    // region events

    /**
     * Parameters:
     * <l>
     * <li>Deposit Nonce</li>
     * <li>Depositing Address</li>
     * <li>Receiving Address</li>
     * <li>Amount</li>
     * <li>Deposit Hash</li>
     * <li>Merkle Root Hash</li>
     * </l>
     */
    @DisplayName("Deposit")
    public static Event6Args<Integer, Hash160, Hash160, Integer, ByteString, ByteString> onDeposit;

    /**
     * Parameters:
     * <l>
     * <li>Withdrawal Nonce</li>
     * <li>Receiving Address</li>
     * <li>Amount</li>
     * </l>
     */
    @DisplayName("Withdrawal")
    public static Event3Args<Integer, Hash160, Integer> onWithdrawal;

    /**
     * Parameters:
     * <l>
     * <li>Claimable Nonce</li>
     * </l>
     */
    @DisplayName("Claimable")
    public static Event1Arg<Integer> onClaimable;

    /**
     * Parameters:
     * <l>
     * <li>Claimed Nonce</li>
     * </l>
     */
    @DisplayName("Claimed")
    public static Event1Arg<Integer> onClaimed;

    // endregion
    // region deployment

    @OnDeployment
    public static void deploy(Object data, boolean isUpdate) {
        if (!isUpdate) {
            BridgeDeploymentData deploymentData = (BridgeDeploymentData) data;
            assert BridgeDeploymentData.isValid(deploymentData);

            baseMap.put(key_bridgeManagement, deploymentData.bridgeManagementContractHash);
            baseMap.put(key_deposit_price, deploymentData.depositPrice);
            baseMap.put(key_deposit_min, deploymentData.minDeposit);
            baseMap.put(key_deposit_max, deploymentData.maxDeposit);
//            baseMap.put(key_max_proofs_per_withdrawal, deploymentData.maxProofsPerWithdrawal);

            baseMap.put(key_deposit_nonce, 0);
            baseMap.put(key_deposit_maxDepth_current, 0);

            baseMap.put(key_withdrawal_nonce, 0);
        }
    }

    // endregion
    // region public deposit

    @OnNEP17Payment
    public static void onNep17Payment(Hash160 from, int amount, Object data) {
        if (getCallingScriptHash() != new GasToken().getHash()) {
            abort("Only GAS is accepted.");
        }
        Hash160 to = (Hash160) data;
        assert Hash160.isValid(to) : "Invalid recipient data.";

        assert amount >= minDeposit() : "Deposit amount is too low.";
        assert amount < maxDeposit() : "Deposit amount is too high.";

        int nonce = newNonce();
        ByteString depositHash = hashDepositOrWithdrawal(nonce, to, amount);
        ByteString root = updateDepositMerkleTree(depositHash);
        baseMap.put(key_deposit_root, root);
        onDeposit.fire(nonce, from, to, amount, depositHash, root);
    }

    // endregion
    // region private deposit helpers

    private static ByteString hashDepositOrWithdrawal(int nonce, Hash160 to, int amount) {
        return cryptoLib.sha256(concatDepositOrWithdrawal(nonce, to, amount));
    }

    private static ByteString concatDepositOrWithdrawal(int nonce, Hash160 to, int amount) {
        byte[] nonceP = padToBytes(toByteArray(nonce), const_nonce_padding_bytes);
        byte[] amountP = padToBytes(toByteArray(amount), const_amount_padding_bytes);
        byte[] concatenated = concat(concat(amountP, to.toByteString()), nonceP);
        reverse(concatenated);
        return new ByteString(concatenated);
    }

    private static byte[] padToBytes(byte[] data, int padToSize) {
        int dataSize = data.length;
        int toPad = padToSize - dataSize;
        assert toPad >= 0 : "Data is too long.";
        byte[] padding = new byte[toPad];
        return concat(data, padding);
    }

    private static ByteString updateDepositMerkleTree(ByteString depositHash) {
        StorageMap rootMap = new StorageMap(ctx, prefix_deposit_root);

        int maxDepth = baseMap.getInt(key_deposit_maxDepth_current);
        boolean carry = true;
        ByteString right = depositHash;
        for (int i = 0; i <= maxDepth; i++) {
            ByteString entryAti = rootMap.get(i);
            boolean stored = entryAti != null;
            if (stored) {
                right = computeParentHash(rootMap.get(i), right);
                if (carry) {
                    rootMap.delete(i);
                    if (i == maxDepth) {
                        int newMaxDepth = maxDepth + 1;
                        rootMap.put(newMaxDepth, right);
                        baseMap.put(key_deposit_maxDepth_current, newMaxDepth);
                    }
                }
            } else {
                if (carry) {
                    rootMap.put(i, right);
                }
                carry = false;
            }
        }
        return right;
    }

    private static int newNonce() {
        int nextNonce = baseMap.getInt(key_deposit_nonce) + 1;
        baseMap.put(key_deposit_nonce, nextNonce);
        return nextNonce;
    }

    // endregion
    // region public withdrawal

    /**
     * This function is called by the relayer to execute withdrawals from the contract. The relayer must provide a
     * list of Merkle proofs that prove that the withdrawals happened under the provided root. The relayer must also
     * provide the signatures of the validators that are required to sign the root.
     * <p>
     * The validators must sign the root together with the nonce of the last withdrawal. This way, the relayer is
     * forced to include all withdrawals that have not been processed yet, i.e., the withdrawal with the next nonce,
     * the withdrawal with the last nonce and all withdrawals inbetween.
     *
     * @param root       the withdrawal root.
     * @param lastNonce  the nonce of the last withdrawal.
     * @param signatures the validator signatures.
     * @param proofs     the withdrawal proofs.
     */
    public static void withdraw(ByteString root, int lastNonce, Map<ECPoint, ByteString> signatures,
            List<MerkleProof> proofs) {

        // Authorization Check
        if (!checkWitness(relayer())) {
            abort("Only the relayer can call this method.");
        }

        // Input Validation Checks
        assert Hash256.isValid(root) : "Invalid root provided.";
        int nrProofs = proofs.size();
        // Todo: Discuss if maxProofsPerWithdrawal should be enforced. Otherwise, if it's not needed, remove it.
//        assert nrProofs <= maxProofsPerWithdrawal() : "Too many proofs provided.";
        // Only checks if merkle proof parameters don't have invalid values, e.g., a negative number for amount.
        assert areValid(proofs) : "Invalid proofs provided.";

        // Logical Parameter Checks
        int startNonce = proofs.get(0).nonce;
        assert startNonce == currentNonce() + 1 : "Provided first nonce is not the next one.";
        assert lastNonce == currentNonce() + nrProofs : "Must provide all proofs that have not been processed under " +
                "the provided root.";
        assert subsequentNonces(proofs, startNonce) : "Provided proofs are not subsequent.";

        // Validator Signature Check
        assert verifyValidatorSignatures(signatures, root, lastNonce) : "Invalid validator signatures provided.";

        // Updating Root and Nonce
        baseMap.put(key_withdrawal_root, root);
        baseMap.put(key_withdrawal_nonce, proofs.get(nrProofs - 1).nonce);

        // Verify all Proofs and Transfer Funds
        verifyProofsAndTransfer(root, proofs);
    }

    // endregion
    // region private withdrawal helpers

    /**
     * Iterates through all proofs. Verifies each proof and transfers the funds to the respective recipient.
     * <p>
     * If a proof verification fails, the whole withdrawal is aborted.
     * <p>
     * If a transfer returns {@code false}, or if the recipient is a contract, the respective nonce is stored in the
     * claimable storage map. This way, anyone can execute it later by providing the withdrawal data and a proof
     * against the current root and pay for the execution themselves.
     *
     * @param root   the root.
     * @param proofs the proofs.
     */
    private static void verifyProofsAndTransfer(ByteString root, List<MerkleProof> proofs) {
        for (int i = 0; i < proofs.size(); i++) {
            MerkleProof merkleProof = proofs.get(i);
            if (verify(root, merkleProof)) {
                Hash160 to = merkleProof.recipient;
                if (isContract(to)) {
                    new StorageMap(ctx, prefix_claimable).put(merkleProof.nonce, true);
                    onClaimable.fire(merkleProof.nonce);
                } else {
                    int amount = merkleProof.amount;
                    if (new GasToken().transfer(getExecutingScriptHash(), to, amount, null)) {
                        onWithdrawal.fire(merkleProof.nonce, to, amount);
                    } else {
                        new StorageMap(ctx, prefix_claimable).put(merkleProof.nonce, true);
                        onClaimable.fire(merkleProof.nonce);
                    }
                }
            } else {
                abort("Invalid proof provided.");
            }
        }
    }

    private static boolean verify(ByteString root, MerkleProof merkleProof) {
        int path = merkleProof.path;
        ByteString parent = hashDepositOrWithdrawal(merkleProof.nonce, merkleProof.recipient, merkleProof.amount);
        List<ByteString> proof = merkleProof.proof;
        int height = 0;
        for (int i = 0; i < proof.size(); i++) {
            // If the bit on position `height` is 1, the i-th proof element is the right child of the next parent.
            if ((path >> height) == 1) {
                parent = computeParentHash(parent, proof.get(i));
            } else {
                parent = computeParentHash(proof.get(i), parent);
            }
            height += 1;
        }
        return parent == root;
    }

    private static boolean isContract(Hash160 scriptHash) {
        return new ContractManagement().getContract(scriptHash) != null;
    }

    private static boolean verifyValidatorSignatures(Map<ECPoint, ByteString> signatures, ByteString root,
            int lastNonce) {
        List<ECPoint> validators = validators();
        int threshold = validatorThreshold();
        assert signatures.keys().length >= threshold : "Not enough signatures provided.";

        ByteString msg = cryptoLib.sha256(new ByteString(concat(root.toByteArray(), lastNonce)));
        int covered = 0;
        for (int i = 0; i < validators.size(); i++) {
            ECPoint validator = validators.get(i);
            if (signatures.containsKey(validator)) {
                boolean verified =
                        cryptoLib.verifyWithECDsa(msg, validator, signatures.get(validator), NamedCurve.Secp256r1);
                if (verified) {
                    covered++;
                }
            }
        }
        return covered >= threshold;
    }

    // Makes sure the proofs have subsequent nonces.
    private static boolean subsequentNonces(List<MerkleProof> proofs, int startNonce) {
        for (int i = 1; i < proofs.size(); i++) {
            if (proofs.get(i - 1).nonce != startNonce + i) {
                return false;
            }
        }
        return true;
    }

    private static int currentNonce() {
        return baseMap.getInt(key_withdrawal_nonce);
    }

    private static boolean areValid(List<MerkleProof> proofs) {
        for (int i = 0; i < proofs.size(); i++) {
            if (!MerkleProof.isValid(proofs.get(i))) {
                return false;
            }
        }
        return true;
    }

    // endregion
    // region claim

    public static void claim(MerkleProof proof) {
        StorageMap claimableMap = new StorageMap(ctx, prefix_claimable);
        int nonce = proof.nonce;
        if (!claimableMap.getBoolean(nonce)) {
            abort("No claimable found for the provided nonce.");
        }
        if (!verify(depositRoot(), proof)) {
            abort("Invalid proof provided.");
        }
        claimableMap.delete(nonce);

        if (new GasToken().transfer(getExecutingScriptHash(), proof.recipient, proof.amount, null)) {
            onWithdrawal.fire(nonce, proof.recipient, proof.amount);
            onClaimed.fire(nonce);
        } else {
            abort("Transfer failed.");
        }
    }

    // endregion
    // region private general helpers

    private static ByteString computeParentHash(ByteString left, ByteString right) {
        ByteString leftRight = new ByteString(concat(left.toByteArray(), right));
        return cryptoLib.sha256(leftRight);
    }

    // endregion
    // region private management getters

    private static ECPoint relayer() {
        return managementContract().relayer();
    }

    private static ECPoint owner() {
        return managementContract().owner();
    }

    private static List<ECPoint> validators() {
        return managementContract().validators();
    }

    private static int validatorThreshold() {
        return managementContract().validatorThreshold();
    }

    private static BridgeManagement managementContract() {
        return new BridgeManagement(baseMap.getHash160(key_bridgeManagement));
    }

    // endregion
    // region public getters with static value

    @Safe
    public static Hash160 management() {
        return baseMap.getHash160(key_bridgeManagement);
    }

    @Safe
    public static int depositPrice() {
        return baseMap.getInt(key_deposit_price);
    }

    @Safe
    public static int minDeposit() {
        return baseMap.getInt(key_deposit_min);
    }

    @Safe
    public static int maxDeposit() {
        return baseMap.getInt(key_deposit_max);
    }

//    @Safe
//    public static int maxProofsPerWithdrawal() {
//        return baseMap.getInt(key_max_proofs_per_withdrawal);
//    }

    // endregion
    // region public getters with dynamic value

    @Safe
    public static ByteString depositRoot() {
        return baseMap.get(key_deposit_root);
    }

    @Safe
    public static int depositsProcessed() {
        return baseMap.getInt(key_deposit_nonce);
    }

    @Safe
    public static int withdrawalsProcessed() {
        return baseMap.getInt(key_withdrawal_nonce);
    }

    // endregion
    // region update

    // Todo: Add code for updating contract (call to contract management and storage changes)
    //  Consider allowing contract update only through Mangement contract.

    // endregion

}
