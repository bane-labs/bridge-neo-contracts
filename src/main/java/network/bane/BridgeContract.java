package network.bane;

import io.neow3j.devpack.*;
import io.neow3j.devpack.annotations.*;
import io.neow3j.devpack.constants.NamedCurve;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.contracts.CryptoLib;
import io.neow3j.devpack.contracts.GasToken;
import io.neow3j.devpack.events.Event3Args;
import io.neow3j.devpack.events.Event6Args;
import network.bane.interfaces.BridgeManagement;
import network.bane.structs.BridgeDeploymentData;
import network.bane.structs.Withdrawal;

import static io.neow3j.devpack.Helper.*;
import static io.neow3j.devpack.Runtime.*;

@Permission(contract = "*", methods = "transfer")
@DisplayName("NeoXBridge")
@ManifestExtra(key = "author", value = "BaneLabs")
@ManifestExtra(key = "description", value = "Contract for bridging GAS tokens from Neo N3 to Neo X.")
public class BridgeContract {

    private static final StorageContext ctx = Storage.getStorageContext();
    private static final CryptoLib cryptoLib = new CryptoLib();

    // region storage keys

    private static final byte prefix_base = 0x0a;
    private static final StorageMap baseMap = new StorageMap(ctx, prefix_base);
    private static final byte prefix_claim = 0x0b;
    private static final StorageMap claimMap = new StorageMap(ctx, prefix_claim);

    private static final int key_bridgeManagement = 0x01;
    private static final int key_deposit_fee = 0x02;
    private static final int key_deposit_min = 0x03;
    private static final int key_deposit_max = 0x04;
    private static final int key_max_withdrawal_per_root = 0x05;
    //    private static final int key_locked = 0x06;

    private static final int key_deposit_root = 0x10;
    private static final int key_deposit_nonce = 0x11;

    private static final int key_withdrawal_root = 0x20;
    private static final int key_withdrawal_nonce = 0x21;

    // In order to arrive the same result in EVM as in NeoVM, the nonce and amount both need to be padded to 8 bytes.
    private static final byte const_nonce_padding_size = 8;
    private static final byte const_amount_padding_size = 8;

    private static final byte const_hash160_size = 20;

    private static final GasToken gasToken = new GasToken();
    private static final ContractManagement contractManagement = new ContractManagement();

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
    public static Event6Args<Integer, Integer, Hash160, Hash160, ByteString, ByteString> onDeposit;

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
            baseMap.put(key_deposit_fee, deploymentData.depositFee);
            baseMap.put(key_deposit_min, deploymentData.minDeposit);
            baseMap.put(key_deposit_max, deploymentData.maxDeposit);
            baseMap.put(key_max_withdrawal_per_root, deploymentData.maxWithdrawalPerRootUpdate);

            // First deposit and withdrawal roots will be zero hashes
            baseMap.put(key_deposit_root, Hash256.zero());
            baseMap.put(key_withdrawal_root, Hash256.zero());

            baseMap.put(key_deposit_nonce, 0);
            baseMap.put(key_withdrawal_nonce, 0);
        }
    }

    // endregion
    // region public deposit

    @OnNEP17Payment
    public static void onNep17Payment(Hash160 from, int amountWithFee, Object data) {
        if (getCallingScriptHash() != gasToken.getHash()) abort("Only GAS is accepted.");
        Hash160 to = (Hash160) data;
        if (!Hash160.isValid(to)) abort("Invalid recipient data.");
        if (to.isZero()) abort("Recipient must not be zero.");

        int depositFee = depositFee();
        if (amountWithFee < minDeposit() + depositFee) abort("Deposit amount is too low.");
        if (amountWithFee >= maxDeposit() + depositFee) abort("Deposit amount is too high.");
        int depositAmount = amountWithFee - depositFee;

        int nonce = newNonce();
        ByteString depositHash = hashDepositOrWithdrawal(nonce, depositAmount, to);
        ByteString newRoot = computeNewRoot(baseMap.get(key_deposit_root), depositHash);
        baseMap.put(key_deposit_root, newRoot);
        onDeposit.fire(nonce, depositAmount, to, from, depositHash, newRoot);
    }

    public static void deposit(Hash160 from, Hash160 to, int depositAmount) {
        if (!gasToken.transfer(from, getExecutingScriptHash(), depositAmount + depositFee(), to)) {
            abort("Transfer failed.");
        }
    }

    // endregion
    // region private deposit helpers

    private static ByteString hashDepositOrWithdrawal(int nonce, int amount, Hash160 to) {
        return cryptoLib.sha256(concatDepositOrWithdrawal(nonce, amount, to));
    }

    private static ByteString concatDepositOrWithdrawal(int nonce, int amount, Hash160 to) {
        byte[] nonceP = padToBytes(toByteArray(nonce), const_nonce_padding_size);
        byte[] amountP = padToBytes(toByteArray(amount), const_amount_padding_size);
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
    // region withdrawal

    public static void withdraw(ByteString withdrawalRoot, Map<ECPoint, ByteString> signatures,
            List<Withdrawal> withdrawals) {
        if (!checkWitness(relayer())) {
            abort("Only the relayer can call this method.");
        }
        if (!verifyValidatorSignatures(signatures, withdrawalRoot)) {
            abort("Invalid validator signatures provided.");
        }

        assert withdrawals.size() > 0 : "At least one withdrawal is required.";
        int startNonce = withdrawals.get(0).nonce;
        assert startNonce == currentNonce() + 1 : "Provided first nonce is not the next one.";
        assert subsequentNonces(withdrawals, currentNonce()) : "Provided withdrawals are not subsequent.";

        baseMap.put(key_withdrawal_nonce, withdrawals.get(withdrawals.size() - 1).nonce);
        ByteString formerWithdrawalRoot = withdrawalRoot();
        baseMap.put(key_withdrawal_root, withdrawalRoot);
        verifyWithdrawalsAndTransfer(formerWithdrawalRoot, withdrawals);
    }

    // endregion
    // region public claim

    public static void claim(int nonce) {
        ByteString claimable = claimMap.get(nonce);
        if (claimable == null) {
            abort("No claim for this nonce.");
        }
        assert claimable.length() == const_hash160_size + const_amount_padding_size : "Invalid claimable data.";

        claimMap.delete(nonce);

        Hash160 to = new Hash160(claimable.take(const_hash160_size));
        int amount = claimable.last(const_amount_padding_size).toInt();
        if (!gasToken.transfer(getExecutingScriptHash(), to, amount, null)) {
            abort("Claim transfer failed.");
        } else {
            onClaimed.fire(nonce, amount, to);
        }
    }

    // endregion
    // region private withdrawal helpers

    private static void verifyWithdrawalsAndTransfer(ByteString formerWithdrawalRoot, List<Withdrawal> withdrawals) {
        // Hash Tree verification
        ByteString parent = formerWithdrawalRoot;
        for (int i = 0; i < withdrawals.size(); i++) {
            Withdrawal withdrawal = withdrawals.get(i);
            // Assertion might not be necessary - just another verification
            assert Withdrawal.isValid(withdrawal) : "Invalid withdrawal provided.";
            ByteString withdrawalHash = hashDepositOrWithdrawal(withdrawal.nonce, withdrawal.amount, withdrawal.to);
            parent = computeNewRoot(parent, withdrawalHash);
        }
        if (parent != withdrawalRoot()) {
            abort("Provided withdrawals do not match the withdrawal root.");
        }

        // Once this is reached, execute the withdrawals
        for (int i = 0; i < withdrawals.size(); i++) {
            Withdrawal withdrawal = withdrawals.get(i);
            if (!isContract(withdrawal.to) &&
                    gasToken.transfer(getExecutingScriptHash(), withdrawal.to, withdrawal.amount, null)) {
                onWithdrawal.fire(withdrawal.nonce, withdrawal.amount, withdrawal.to);
            } else {
                // Add the withdrawal to the claim map if either the recipient was a contract, or the transfer failed.
                addToClaim(withdrawal);
                onClaimable.fire(withdrawal.nonce, withdrawal.amount, withdrawal.to);
            }
        }
    }

    private static void addToClaim(Withdrawal withdrawal) {
        claimMap.put(withdrawal.nonce, concat(withdrawal.to.toByteArray(), padToBytes(toByteArray(withdrawal.amount), const_amount_padding_size)));
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
                boolean verified =
                        cryptoLib.verifyWithECDsa(msg, validator, signatures.get(validator), NamedCurve.Secp256r1);
                if (verified) {
                    covered++;
                }
            }
        }
        return covered >= threshold;
    }

    // Makes sure the withdrawals have subsequent nonces.
    private static boolean subsequentNonces(List<Withdrawal> withdrawals, int startNonce) {
        for (int i = 1; i <= withdrawals.size(); i++) {
            if (withdrawals.get(i - 1).nonce != startNonce + i) {
                return false;
            }
        }
        return true;
    }

    private static int currentNonce() {
        return baseMap.getInt(key_withdrawal_nonce);
    }

    // endregion
    // region private general helpers

    private static ByteString computeNewRoot(ByteString left, ByteString right) {
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
    public static int depositFee() {
        return baseMap.getInt(key_deposit_fee);
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
    public static int maxWithdrawalPerRoot() {
        return baseMap.getInt(key_max_withdrawal_per_root);
    }

    // endregion
    // region public getters with dynamic value

    @Safe
    public static ByteString depositRoot() {
        return baseMap.get(key_deposit_root);
    }

    @Safe
    public static ByteString withdrawalRoot() {
        return baseMap.get(key_withdrawal_root);
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

    // region setters
    
    public static void setDepositFee(int fee) {
        if(!checkWitness(owner())) abort("Only owner can set deposit fee.");
        if(fee < 0) abort("Deposit fee must be nonnegative.");
        baseMap.put(key_deposit_fee, fee);
    }
    
    //endregion
    
    // region update
    // Todo: Add code for updating contract (call to contract management and storage changes)
    //  Consider allowing contract update only through Mangement contract.
    // endregion

}
