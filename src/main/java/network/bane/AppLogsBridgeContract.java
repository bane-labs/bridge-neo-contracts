package network.bane;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.ECPoint;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Hash256;
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
import io.neow3j.devpack.events.Event4Args;
import network.bane.interfaces.BridgeManagement;
import network.bane.structs.BridgeDeploymentData;
import network.bane.structs.WithdrawalWithProof;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Helper.concat;
import static io.neow3j.devpack.Helper.reverse;
import static io.neow3j.devpack.Helper.toByteArray;
import static io.neow3j.devpack.Runtime.checkWitness;
import static io.neow3j.devpack.Runtime.getCallingScriptHash;
import static io.neow3j.devpack.Runtime.getExecutingScriptHash;

//@DisplayName("BaneBridge")
@ManifestExtra(key = "author", value = "BaneLabs")
@ManifestExtra(key = "description", value = "Contract for bridging GAS tokens from Neo N3 to Bane.")
public class AppLogsBridgeContract {

    private static final StorageContext ctx = Storage.getStorageContext();
    private static final CryptoLib cryptoLib = new CryptoLib();

    // region storage keys

    private static final byte prefix_base = 0x0a;
    private static final StorageMap baseMap = new StorageMap(ctx, prefix_base);
    private static final byte prefix_claim = 0x0b;
    private static final StorageMap claimMap = new StorageMap(ctx, prefix_claim);

    private static final int key_bridgeManagement = 0x01;
    private static final int key_deposit_price = 0x02;
    private static final int key_deposit_min = 0x03;
    private static final int key_deposit_max = 0x04;
    private static final int key_max_withdrawals_per_root_update = 0x05;
    //    private static final int key_locked = 0x06;

    private static final int key_deposit_root = 0x10;
    private static final int key_deposit_nonce = 0x11;
    private static final int key_deposit_maxDepth_current = 0x12;

    private static final int key_withdrawal_root = 0x20;
    private static final int key_withdrawal_nonce = 0x21;

    // In order to arrive the same result in EVM as in NeoVM, the nonce and amount need to be padded to 8 bytes.
    private static final byte const_nonce_padding_bytes = 8;
    private static final byte const_amount_padding_bytes = 8;

    private static final byte const_hash160_size = 20;

    private static final GasToken gasToken = new GasToken();

    // endregion
    // region events

    /**
     * Parameters:
     * <l>
     * <li>Deposit Nonce</li>
     * <li>Deposit Amount</li>
     * <li>Receiving Address</li>
     * <li>Depositing Address</li>
     * <li>Deposit Hash</li>
     * <li>New Root Hash</li>
     * </l>
     */
    @DisplayName("Deposit")
    public static Event4Args<Integer, Integer, Hash160, Hash160> onDeposit;

    /**
     * Parameters:
     * <l>
     * <li>Withdrawal Nonce</li>
     * <li>Withdrawal Amount</li>
     * <li>Receiving Address</li>
     * </l>
     */
    @DisplayName("Withdrawal")
    public static Event3Args<Integer, Integer, Hash160> onWithdrawal;

    /**
     * Parameters:
     * <l>
     * <li>Claimable Nonce</li>
     * <li>Claimable Amount</li>
     * <li>Claimable Recipient</li>
     * </l>
     */
    @DisplayName("Claimable")
    public static Event3Args<Integer, Integer, Hash160> onClaimable;

    /**
     * Parameters:
     * <l>
     * <li>Claimed Nonce</li>
     * <li>Claimable Amount</li>
     * <li>Claimable Recipient</li>
     * </l>
     */
    @DisplayName("Claimed")
    public static Event3Args<Integer, Integer, Hash160> onClaimed;

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
            baseMap.put(key_max_withdrawals_per_root_update, deploymentData.maxWithdrawalPerRootUpdate);

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
        assert !to.isZero() : "Recipient must not be zero.";

        assert amount >= minDeposit() : "Deposit amount is too low.";
        assert amount < maxDeposit() : "Deposit amount is too high.";

        int nonce = newNonce();

        // Providing the deposit hash in the notificatino is not necessarily required since the validators will compute
        // the full merkle tree anyway and can thus compute the deposit hash themselves. However, it could be provided
        // here to make it easier for the validators to compute the merkle tree.
//        ByteString depositHash = hashDepositOrWithdrawal(nonce, amount, to);

        onDeposit.fire(nonce, amount, to, from);
    }

    // endregion
    // region private deposit helpers

    private static ByteString hashDepositOrWithdrawal(int nonce, int amount, Hash160 to) {
        return cryptoLib.sha256(concatDepositOrWithdrawal(nonce, amount, to));
    }

    private static ByteString concatDepositOrWithdrawal(int nonce, int amount, Hash160 to) {
        byte[] nonceP = padToBytes(toByteArray(nonce), const_nonce_padding_bytes);
        byte[] amountP = padToBytes(toByteArray(amount), const_amount_padding_bytes);
        byte[] concatenated = concat(concat(to.toByteArray(), amountP), nonceP);
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

    private static int newNonce() {
        int nextNonce = baseMap.getInt(key_deposit_nonce) + 1;
        baseMap.put(key_deposit_nonce, nextNonce);
        return nextNonce;
    }

    // endregion
    // region public withdrawal

    /**
     * This function is called by the relayer to execute withdrawals from the contract. The relayer must provide a
     * list of proofs that prove that the withdrawals happened under the provided root. The relayer must also provide
     * the signatures of the validators that are required to sign the root.
     *
     * @param newWithdrawalRoot the withdrawal root.
     * @param signatures        the validator signatures.
     * @param withdrawals       the withdrawals with their proof.
     */
    // Todo: Currently the relayer is not enforced to bridge all withdrawals that happened under the provided root.
    //  Let's say 10 deposits happened. The relayer could bridge the root, but only provide 5 of the 10 deposits.
    //  For example, the relayer could always only bridge one withdrawal per root update and thus slow down the
    //  process. The current implementation only enforces that no deposit can be skipped.
    //  If we want to enforce that all withdrawals under the provided root are bridged, we could make the validators
    //  sign not only the root, but also the last nonce under that root, i.e., they would need to sign a
    //  concatenation of the root and the last nonce. Alternatively, instead of the last nonce, we could use the
    //  number of withdrawals, i.e., the concatenation of the root and the number of withdrawals that happened under it.
    public static void withdraw(ByteString newWithdrawalRoot, Map<ECPoint, ByteString> signatures,
            List<WithdrawalWithProof> withdrawals) {

        // Authorization Check
        if (!checkWitness(relayer())) {
            abort("Only the relayer can call this method.");
        }

        // Input Validation Checks
        assert Hash256.isValid(newWithdrawalRoot) : "Invalid root provided.";
        // Todo: Discuss if there should be a max number of withdrawals per root update. If so, uncomment the following
        //  two lines, and include the necessary logic in the validator node code.
//        int nrWithdrawals = withdrawals.size();
//        assert nrWithdrawals <= maxWithdrawalsPerRootUpdate() : "Too many withdrawals provided.";
        // Only checks if merkle proof parameters don't have invalid values, e.g., a negative number for amount.
        assert areValid(withdrawals) : "Invalid proofs provided.";

        // Check Subsequent Nonces
        int startNonce = withdrawals.get(0).nonce;
        int currentNonce = currentNonce();
        assert startNonce == currentNonce + 1 : "Provided first nonce is not the next one.";
        assert subsequentNonces(withdrawals, startNonce) : "Provided withdrawals are not subsequent.";

        // Validator Signature Check
        assert verifyValidatorSignatures(signatures, newWithdrawalRoot) : "Invalid validator signatures provided.";

        // Updating Root and Nonce before verifying and transferring funds
        baseMap.put(key_withdrawal_root, newWithdrawalRoot);
        baseMap.put(key_withdrawal_nonce, currentNonce + withdrawals.size());

        // Verify all Proofs and Transfer Funds
        verifyWithdrawalsAndTransfer(newWithdrawalRoot, withdrawals);
    }

    // endregion
    // region private withdrawal helpers

    /**
     * Iterates through all withdrawal. Verifies the proof of each and transfers the funds to the respective recipient.
     * <p>
     * If a proof verification fails, the whole withdrawal process is aborted.
     * <p>
     * If the recipient is a contract or if a transfer fails (i.e., returns {@code false}), the respective withdrawal
     * data is stored in the claimable storage map. This way, anyone can execute it later at their own cost.
     *
     * @param newRoot     the new root.
     * @param withdrawals the withdrawals with their respective proof.
     */
    private static void verifyWithdrawalsAndTransfer(ByteString newRoot, List<WithdrawalWithProof> withdrawals) {
        Hash160 from = getExecutingScriptHash();
        for (int i = 0; i < withdrawals.size(); i++) {
            WithdrawalWithProof withdrawal = withdrawals.get(i);
            if (!verify(newRoot, withdrawal)) {
                abort("Invalid proof provided.");
            }
            Hash160 to = withdrawal.to;
            int amount = withdrawal.amount;
            if (!isContract(to) && gasToken.transfer(from, to, amount, null)) {
                onWithdrawal.fire(withdrawal.nonce, amount, to);
            } else {
                addToClaim(withdrawal);
                onClaimable.fire(withdrawal.nonce, amount, to);
            }
        }
    }

    private static void addToClaim(WithdrawalWithProof withdrawal) {
        claimMap.put(withdrawal.nonce, concat(withdrawal.to.toByteArray(), withdrawal.amount));
    }

    private static boolean verify(ByteString root, WithdrawalWithProof withdrawal) {
        int path = withdrawal.path;
        ByteString parent = hashDepositOrWithdrawal(withdrawal.nonce, withdrawal.amount, withdrawal.to);
        ByteString[] proof = withdrawal.proof;
        int height = 0;
        for (ByteString proofEntry : proof) {
            // If the bit on position `height` is 1, the i-th proof element is the right child of the next parent.
            if (((path >> height) & 0x01) == 1) {
                parent = computeParentHash(parent, proofEntry);
            } else {
                parent = computeParentHash(proofEntry, parent);
            }
            height += 1;
        }
        return parent == root;
    }

    private static boolean isContract(Hash160 scriptHash) {
        return new ContractManagement().getContract(scriptHash) != null;
    }

    private static boolean verifyValidatorSignatures(Map<ECPoint, ByteString> signatures, ByteString root) {
        List<ECPoint> validators = validators();
        int threshold = validatorThreshold();
        assert signatures.keys().length >= threshold : "Not enough signatures provided.";

        ByteString msg = cryptoLib.sha256(root);
        int covered = 0;
        for (int i = 0; i < validators.size(); i++) {
            ECPoint validator = validators.get(i);
            if (signatures.containsKey(validator)) {
                if (cryptoLib.verifyWithECDsa(msg, validator, signatures.get(validator), NamedCurve.Secp256r1)) {
                    covered++;
                }
            }
        }
        return covered >= threshold;
    }

    // Makes sure the withdrawal have subsequent nonces.
    private static boolean subsequentNonces(List<WithdrawalWithProof> withdrawal, int startNonce) {
        for (int i = 1; i < withdrawal.size(); i++) {
            if (withdrawal.get(i - 1).nonce != startNonce + i) {
                return false;
            }
        }
        return true;
    }

    private static int currentNonce() {
        return baseMap.getInt(key_withdrawal_nonce);
    }

    private static boolean areValid(List<WithdrawalWithProof> withdrawals) {
        for (int i = 0; i < withdrawals.size(); i++) {
            if (!WithdrawalWithProof.isValid(withdrawals.get(i))) {
                return false;
            }
        }
        return true;
    }

    // endregion
    // region claim

    public static void claim(int nonce) {
        ByteString claimData = claimMap.get(nonce);
        if (claimData == null) {
            abort("No claimable found for the provided nonce.");
        }
        // This should always be true, but we check it anyway.
        assert claimData.length() == const_hash160_size + const_amount_padding_bytes : "Invalid claim data.";

        Hash160 to = new Hash160(claimData.range(0, const_hash160_size));
        int amount = claimData.range(const_hash160_size, const_hash160_size + const_amount_padding_bytes).toInt();

        claimMap.delete(nonce);
        if (gasToken.transfer(getExecutingScriptHash(), to, amount, null)) {
            onClaimed.fire(nonce, amount, to);
            // Todo: Should we fire the Withdrawal event here as well?
//            onWithdrawal.fire(nonce, amount, to);
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

    @Safe
    public static int maxWithdrawalsPerRootUpdate() {
        return baseMap.getInt(key_max_withdrawals_per_root_update);
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
    public static ByteString withdrawalRoot() {
        return baseMap.get(key_withdrawal_root);
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
