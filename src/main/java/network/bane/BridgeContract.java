package network.bane;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.ECPoint;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.List;
import io.neow3j.devpack.Map;
import io.neow3j.devpack.Storage;
import io.neow3j.devpack.StorageContext;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.annotations.DisplayName;
import io.neow3j.devpack.annotations.ManifestExtra;
import io.neow3j.devpack.annotations.OnDeployment;
import io.neow3j.devpack.annotations.OnNEP17Payment;
import io.neow3j.devpack.annotations.Safe;
import io.neow3j.devpack.constants.NamedCurve;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.contracts.CryptoLib;
import io.neow3j.devpack.contracts.GasToken;
import io.neow3j.devpack.events.Event3Args;
import io.neow3j.devpack.events.Event6Args;
import network.bane.interfaces.BridgeManagement;
import network.bane.structs.BridgeDeploymentData;
import network.bane.structs.MerkleProof;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Helper.concat;
import static io.neow3j.devpack.Helper.toByteArray;
import static io.neow3j.devpack.Runtime.checkWitness;
import static io.neow3j.devpack.Runtime.getCallingScriptHash;
import static io.neow3j.devpack.Runtime.getExecutingScriptHash;

@DisplayName("BaneBridge")
@ManifestExtra(key = "author", value = "BaneLabs")
@ManifestExtra(key = "description", value = "Contract for bridging GAS tokens from Neo N3 to Bane.")
public class BridgeContract {

    private static final StorageContext ctx = Storage.getStorageContext();

    // region storage keys

    private static final byte prefix_base = 0x0a;
    private static final StorageMap baseMap = new StorageMap(ctx, prefix_base);

    private static final int key_bridgeManagement = 0x01;
    private static final int key_deposit_price = 0x02;
    private static final int key_deposit_min = 0x03;
    private static final int key_deposit_max = 0x04;
    private static final int key_max_proofs_per_withdrawal = 0x05;
    //    private static final int key_locked = 0x06;

    private static final int key_deposit_root = 0x10;
    private static final int key_deposit_nonce = 0x11;
    private static final int key_deposit_maxDepth = 0x12;

    private static final int key_withdrawal_nonce = 0x21;

    // Used to store incomplete subtree hashes
    private static final byte prefix_deposit_root = 0x0b;

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
    @DisplayName("OnDeposit")
    public static Event6Args<Integer, Hash160, Hash160, Integer, ByteString, ByteString> onDeposit;

    /**
     * Parameters:
     * <l>
     * <li>Withdrawal Nonce</li>
     * <li>Receiving Address</li>
     * <li>Amount</li>
     * </l>
     */
    @DisplayName("OnWithdrawal")
    public static Event3Args<Integer, Hash160, Integer> onWithdrawal;

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
            baseMap.put(key_max_proofs_per_withdrawal, deploymentData.maxProofsPerWithdrawal);

            baseMap.put(key_deposit_nonce, 0);
            baseMap.put(key_deposit_maxDepth, 0);

            baseMap.put(key_withdrawal_nonce, 0);
        }
    }

    // endregion
    // region public deposit

    @OnNEP17Payment
    public static void deposit(Hash160 from, int amount, Object data) {
        if (getCallingScriptHash() != new GasToken().getHash()) {
            abort("Only GAS is accepted.");
        }
        Hash160 to = (Hash160) data;
        assert Hash160.isValid(to) : "Invalid recipient data.";

        int nonce = newNonce();
        ByteString depositHash = hashDepositOrWithdrawal(nonce, to, amount);
        ByteString root = updateDepositMerkleTree(depositHash);
        baseMap.put(key_deposit_root, root);
        onDeposit.fire(nonce, from, to, amount, depositHash, root);
    }

    // endregion
    // region private deposit helpers

    private static ByteString hashDepositOrWithdrawal(int nonce, Hash160 to, int amount) {
        byte[] concatenatedData = concat(concat(toByteArray(nonce), to.toByteString()), amount);
        return new CryptoLib().sha256(new ByteString(concatenatedData));
    }

    private static ByteString updateDepositMerkleTree(ByteString depositHash) {
        StorageMap rootMap = new StorageMap(ctx, prefix_deposit_root);

        int maxDepth = baseMap.getInt(key_deposit_maxDepth);
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
                        baseMap.put(key_deposit_maxDepth, newMaxDepth);
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

    public static void withdraw(List<MerkleProof> proofs, Map<ECPoint, ByteString> signatures) {
        if (!checkWitness(relayer())) {
            abort("Only the relayer can call this method.");
        }
        assert proofs.size() <= maxProofsPerWithdrawal() : "Too many proofs provided.";
        assert areValid(proofs) : "Invalid proofs provided.";

        int startNonce = proofs.get(0).nonce;
        assert startNonce == currentNonce() + 1 : "Provided first nonce is not the next one.";
        assert subsequentNonces(proofs, startNonce) : "Provided proofs are not subsequent.";

        assert verifyValidatorSignatures(signatures, proofs) : "Invalid validator signatures provided.";

        verifyProofsAndTransfer(proofs);
        baseMap.put(key_withdrawal_nonce, proofs.get(proofs.size() - 1).nonce);
    }

    // endregion
    // region private withdrawal helpers

    private static void verifyProofsAndTransfer(List<MerkleProof> proofs) {
        for (int i = 0; i < proofs.size(); i++) {
            MerkleProof merkleProof = proofs.get(i);
            if (verify(merkleProof)) {
                Hash160 to = merkleProof.recipient;
                if (!isContract(to)) {
                    int amount = merkleProof.amount;
                    assert new GasToken().transfer(getExecutingScriptHash(), to, amount, null) : "Transfer failed.";
                    onWithdrawal.fire(merkleProof.nonce, to, amount);
                }
                // In case the recipient is a contract, no funds are sent. However, the Merkle Tree computation must
                // withstand.
            } else {
                abort("Invalid proof provided.");
            }
        }

    }

    private static boolean verify(MerkleProof merkleProof) {
        ByteString right = hashDepositOrWithdrawal(merkleProof.nonce, merkleProof.recipient, merkleProof.amount);
        List<ByteString> proof = merkleProof.proof;
        for (int i = 0; i < proof.size(); i++) {
            right = computeParentHash(proof.get(i), right);
        }
        return right == merkleProof.root;
    }

    private static boolean isContract(Hash160 scriptHash) {
        return new ContractManagement().getContract(scriptHash) != null;
    }

    private static boolean verifyValidatorSignatures(Map<ECPoint, ByteString> signatures, List<MerkleProof> proofs) {
        List<ECPoint> validators = validators();
        int threshold = validatorThreshold();
        assert signatures.keys().length >= threshold : "Not enough signatures provided.";

        ByteString rootsHashed = concatAndSha256(proofs);
        int covered = 0;
        CryptoLib cryptoLib = new CryptoLib();
        for (int i = 0; i < validators.size(); i++) {
            ECPoint validator = validators.get(i);
            if (signatures.containsKey(validator)) {
                boolean verified =
                        cryptoLib.verifyWithECDsa(rootsHashed, validator, signatures.get(validator),
                                NamedCurve.Secp256r1);
                if (verified) {
                    covered++;
                }
            }
        }
        return covered >= threshold;
    }

    private static ByteString concatAndSha256(List<MerkleProof> proofs) {
        byte[] concatRoots = proofs.get(0).root.toByteArray();
        for (int i = 1; i < proofs.size(); i++) {
            concatRoots = concat(concatRoots, proofs.get(i).root);
        }
        return new CryptoLib().sha256(new ByteString(concatRoots));
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
    // region private general helpers

    private static ByteString computeParentHash(ByteString left, ByteString right) {
        ByteString leftRight = new ByteString(concat(left.toByteArray(), right));
        return new CryptoLib().sha256(leftRight);
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

    @Safe
    public static int maxProofsPerWithdrawal() {
        return baseMap.getInt(key_max_proofs_per_withdrawal);
    }

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
