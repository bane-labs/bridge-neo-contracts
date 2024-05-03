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
import io.neow3j.devpack.annotations.Permission;
import io.neow3j.devpack.annotations.Safe;
import io.neow3j.devpack.constants.NamedCurve;
import io.neow3j.devpack.constants.NativeContract;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.contracts.CryptoLib;
import io.neow3j.devpack.contracts.GasToken;
import io.neow3j.devpack.events.Event1Arg;
import io.neow3j.devpack.events.Event3Args;
import io.neow3j.devpack.events.Event6Args;
import network.bane.interfaces.BridgeManagement;
import network.bane.structs.BridgeDeploymentData;
import network.bane.structs.Withdrawal;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Helper.concat;
import static io.neow3j.devpack.Helper.reverse;
import static io.neow3j.devpack.Helper.toByteArray;
import static io.neow3j.devpack.Runtime.checkWitness;
import static io.neow3j.devpack.Runtime.getCallingScriptHash;
import static io.neow3j.devpack.Runtime.getExecutingScriptHash;

@DisplayName("NeoXBridge")
@Permission(nativeContract = NativeContract.GasToken, methods = "transfer")
@Permission(nativeContract = NativeContract.ContractManagement, methods = "update")
@ManifestExtra(key = "Author", value = "BaneLabs")
@ManifestExtra(key = "Target", value = "Neo X TestNet T3")
@ManifestExtra(key = "Description", value = "Contract for bridging GAS tokens between Neo N3 and Neo X.")
public class BridgeContract {
    // region initsslot setup
    private static final byte UINT256_SIZE = 32;
    private static final byte HASH160_SIZE = 20;

    private static final StorageContext ctx = Storage.getStorageContext();
    private static final CryptoLib cryptoLib = new CryptoLib();
    private static final GasToken gasToken = new GasToken();
    private static final ContractManagement contractManagement = new ContractManagement();

    // map prefixes
    private static final byte PREFIX_BASE = 0x0a;
    private static final byte PREFIX_GAS_CLAIMABLES = 0x0b;

    // base map and keys
    private static final StorageMap baseMap = new StorageMap(ctx, PREFIX_BASE);

    private static final int KEY_BRIDGE_MANAGEMENT = 0x01;

    private static final int KEY_GAS_DEPOSIT_FEE = 0x02;
    private static final int KEY_GAS_DEPOSIT_MIN_AMOUNT = 0x03;
    private static final int KEY_GAS_DEPOSIT_MAX_AMOUNT = 0x04;

    private static final int KEY_LOCKED = 0x05;

    private static final int KEY_GAS_DEPOSIT_ROOT = 0x10;
    private static final int KEY_GAS_DEPOSIT_NONCE = 0x11;

    private static final int KEY_GAS_WITHDRAWAL_ROOT = 0x20;
    private static final int KEY_GAS_WITHDRAWAL_NONCE = 0x21;

    // gas claim map
    private static final StorageMap gasClaimableMap = new StorageMap(ctx, PREFIX_GAS_CLAIMABLES);

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

    /**
     * Parameters:
     * <l>
     * <li>New Deposit Fee</li>
     * </l>
     */
    @DisplayName("DepositFeeChanged")
    public static Event1Arg<Integer> onDepositFeeSet;

    /**
     * Parameters:
     * <l>
     * <li>New Min Deposit</li>
     * </l>
     */
    @DisplayName("MinDepositChanged")
    public static Event1Arg<Integer> onMinDepositSet;

    /**
     * Parameters:
     * <l>
     * <li>New Max Deposit</li>
     * </l>
     */
    @DisplayName("MaxDepositChanged")
    public static Event1Arg<Integer> onMaxDepositSet;

    // endregion
    // region deployment

    @OnDeployment
    public static void deploy(Object data, boolean isUpdate) {
        if (!isUpdate) {
            BridgeDeploymentData deploymentData = (BridgeDeploymentData) data;

            if (!Hash160.isValid(deploymentData.bridgeManagementContractHash))
                abort("Invalid bridge management contract hash.");
            if (deploymentData.depositFee < 0) abort("Deposit fee must be nonnegative.");
            if (deploymentData.minDeposit < 0) abort("Minimum deposit must be nonnegative.");
            if (deploymentData.maxDeposit < deploymentData.minDeposit)
                abort("Maximum deposit must be greater than the minimum deposit.");

            baseMap.put(KEY_BRIDGE_MANAGEMENT, deploymentData.bridgeManagementContractHash);
            baseMap.put(KEY_GAS_DEPOSIT_FEE, deploymentData.depositFee);
            baseMap.put(KEY_GAS_DEPOSIT_MIN_AMOUNT, deploymentData.minDeposit);
            baseMap.put(KEY_GAS_DEPOSIT_MAX_AMOUNT, deploymentData.maxDeposit);

            // Initial deposit and withdrawal roots will be zero hashes
            baseMap.put(KEY_GAS_DEPOSIT_ROOT, Hash256.zero());
            baseMap.put(KEY_GAS_WITHDRAWAL_ROOT, Hash256.zero());

            baseMap.put(KEY_GAS_DEPOSIT_NONCE, 0);
            baseMap.put(KEY_GAS_WITHDRAWAL_NONCE, 0);
            baseMap.put(KEY_LOCKED, false);
        }
    }

    // endregion
    // region public deposit

    @OnNEP17Payment
    public static void onNep17Payment(Hash160 from, int amountWithFee, Object data) {
        if (isLocked()) abort("Contract is locked.");
        if (getCallingScriptHash() != gasToken.getHash()) abort("Only GAS is accepted.");
        Hash160 to = (Hash160) data;
        if (!Hash160.isValid(to)) abort("Invalid recipient data.");
        if (to.isZero()) abort("Recipient must not be zero.");

        int depositFee = depositFee();
        if (amountWithFee < minDeposit() + depositFee) abort("Deposit amount is too low.");
        if (amountWithFee > maxDeposit() + depositFee) abort("Deposit amount is too high.");
        int depositAmount = amountWithFee - depositFee;

        int nonce = newNonce();
        ByteString depositHash = hashDepositOrWithdrawal(nonce, depositAmount, to);
        ByteString newRoot = computeNewRoot(baseMap.get(KEY_GAS_DEPOSIT_ROOT), depositHash);
        baseMap.put(KEY_GAS_DEPOSIT_ROOT, newRoot);
        onDeposit.fire(nonce, depositAmount, to, from, depositHash, newRoot);
    }

    public static void deposit(Hash160 from, Hash160 to, int depositAmount) {
        Hash160 executingScriptHash = getExecutingScriptHash();
        if (executingScriptHash.equals(from)) abort("Invalid 'from' parameter.");
        if (!gasToken.transfer(from, executingScriptHash, depositAmount + depositFee(), to)) {
            abort("Transfer failed.");
        }
    }

    // endregion
    // region private deposit helpers

    private static ByteString hashDepositOrWithdrawal(int nonce, int amount, Hash160 to) {
        return cryptoLib.sha256(concatDepositOrWithdrawal(nonce, amount, to));
    }

    private static ByteString concatDepositOrWithdrawal(int nonce, int amount, Hash160 to) {
        byte[] nonceP = padToBytes(toByteArray(nonce), UINT256_SIZE);
        byte[] amountP = padToBytes(toByteArray(amount), UINT256_SIZE);
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
        int nextNonce = baseMap.getInt(KEY_GAS_DEPOSIT_NONCE) + 1;
        baseMap.put(KEY_GAS_DEPOSIT_NONCE, nextNonce);
        return nextNonce;
    }

    // endregion
    // region withdrawal

    public static void withdraw(ByteString withdrawalRoot, Map<ECPoint, ByteString> signatures,
            List<Withdrawal> withdrawals) {
        if (isLocked()) abort("Contract is locked.");
        if (!checkWitness(relayer())) abort("Only the relayer can call this method.");
        if (!verifyValidatorSignatures(signatures, withdrawalRoot)) abort("Invalid validator signatures provided.");

        if (withdrawals.size() <= 0) abort("At least one withdrawal is required.");
        int startNonce = withdrawals.get(0).nonce;
        if (startNonce != currentNonce() + 1) abort("Provided first nonce is not the next one.");
        if (!subsequentNonces(withdrawals, currentNonce())) abort("Provided withdrawals are not subsequent.");

        baseMap.put(KEY_GAS_WITHDRAWAL_NONCE, withdrawals.get(withdrawals.size() - 1).nonce);
        ByteString formerWithdrawalRoot = withdrawalRoot();
        baseMap.put(KEY_GAS_WITHDRAWAL_ROOT, withdrawalRoot);
        verifyWithdrawalsAndTransfer(formerWithdrawalRoot, withdrawals);
    }

    // endregion
    // region public claim

    public static void claim(int nonce) {
        if (isLocked()) abort("Contract is locked.");
        ByteString claimable = gasClaimableMap.get(nonce);
        if (claimable == null) abort("No claim for this nonce.");
        if (claimable.length() != HASH160_SIZE + UINT256_SIZE) abort("Invalid claimable data.");

        gasClaimableMap.delete(nonce);

        Hash160 to = new Hash160(claimable.take(HASH160_SIZE));
        int amount = claimable.last(UINT256_SIZE).toInt();
        if (gasToken.transfer(getExecutingScriptHash(), to, amount, null)) {
            onClaimed.fire(nonce, amount, to);
        } else {
            abort("Claim transfer failed.");
        }
    }

    // endregion
    // region locking

    public static void lock() {
        if (isLocked()) abort("Contract is already locked.");
        if (!checkWitness(securityGuard())) abort("Only the security guard can lock the contract.");
        baseMap.put(KEY_LOCKED, true);
    }

    public static void unlock() {
        if (!isLocked()) abort("Contract is already unlocked.");
        if (!checkWitness(governor())) abort("Only the governor can unlock the contract.");
        baseMap.put(KEY_LOCKED, false);
    }

    @Safe
    public static boolean isLocked() {
        return baseMap.getBoolean(KEY_LOCKED);
    }

    // endregion
    // region private withdrawal helpers

    private static void verifyWithdrawalsAndTransfer(ByteString formerWithdrawalRoot, List<Withdrawal> withdrawals) {
        // Hash Tree verification
        ByteString parent = formerWithdrawalRoot;
        for (int i = 0; i < withdrawals.size(); i++) {
            Withdrawal withdrawal = withdrawals.get(i);
            if (!Withdrawal.isValid(withdrawal)) abort("Invalid withdrawal provided.");
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
        gasClaimableMap.put(withdrawal.nonce, concat(withdrawal.to.toByteArray(),
                padToBytes(toByteArray(withdrawal.amount), UINT256_SIZE)));
    }

    private static boolean isContract(Hash160 scriptHash) {
        return contractManagement.getContract(scriptHash) != null;
    }

    private static boolean verifyValidatorSignatures(Map<ECPoint, ByteString> signatures, ByteString root) {
        List<ECPoint> validators = validators();
        int threshold = validatorThreshold();
        if (signatures.keys().length < threshold) abort("Not enough signatures provided.");

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
        return baseMap.getInt(KEY_GAS_WITHDRAWAL_NONCE);
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

    private static ECPoint governor() {
        return managementContract().governor();
    }

    private static ECPoint securityGuard() {
        return managementContract().securityGuard();
    }

    private static BridgeManagement managementContract() {
        return new BridgeManagement(baseMap.getHash160(KEY_BRIDGE_MANAGEMENT));
    }

    // endregion
    // region public getters with static value

    @Safe
    public static Hash160 management() {
        return baseMap.getHash160(KEY_BRIDGE_MANAGEMENT);
    }

    @Safe
    public static int depositFee() {
        return baseMap.getInt(KEY_GAS_DEPOSIT_FEE);
    }

    @Safe
    public static int minDeposit() {
        return baseMap.getInt(KEY_GAS_DEPOSIT_MIN_AMOUNT);
    }

    @Safe
    public static int maxDeposit() {
        return baseMap.getInt(KEY_GAS_DEPOSIT_MAX_AMOUNT);
    }

    // endregion
    // region public getters with dynamic value

    @Safe
    public static ByteString depositRoot() {
        return baseMap.get(KEY_GAS_DEPOSIT_ROOT);
    }

    @Safe
    public static ByteString withdrawalRoot() {
        return baseMap.get(KEY_GAS_WITHDRAWAL_ROOT);
    }

    @Safe
    public static int depositsProcessed() {
        return baseMap.getInt(KEY_GAS_DEPOSIT_NONCE);
    }

    @Safe
    public static int withdrawalsProcessed() {
        return baseMap.getInt(KEY_GAS_WITHDRAWAL_NONCE);
    }

    // endregion
    // region setters

    public static void setDepositFee(int fee) {
        if (!checkWitness(governor())) abort("Only the governor can set the deposit fee.");
        if (fee < 0) abort("Deposit fee must be nonnegative.");
        baseMap.put(KEY_GAS_DEPOSIT_FEE, fee);
        onDepositFeeSet.fire(fee);
    }

    public static void setMinDeposit(int newMinDeposit) {
        if (!checkWitness(governor())) abort("Only the governor can set the minimum deposit.");
        if (newMinDeposit < 0) abort("Minimum deposit must be nonnegative.");
        if (newMinDeposit > maxDeposit()) abort("Minimum deposit must be less than the maximum deposit.");
        baseMap.put(KEY_GAS_DEPOSIT_MIN_AMOUNT, newMinDeposit);
        onMinDepositSet.fire(newMinDeposit);
    }

    public static void setMaxDeposit(int newMaxDeposit) {
        if (!checkWitness(governor())) abort("Only the governor can set the maximum deposit.");
        if (newMaxDeposit < minDeposit()) abort("Maximum deposit must be greater than the minimum deposit.");
        baseMap.put(KEY_GAS_DEPOSIT_MAX_AMOUNT, newMaxDeposit);
        onMaxDepositSet.fire(newMaxDeposit);
    }

    //endregion
    // region update

    public static void update(ByteString nef, String manifest) {
        if (!isLocked()) abort("Contract needs to be locked to update.");
        if (!checkWitness(owner())) abort("Only the owner can update this contract.");
        contractManagement.update(nef, manifest);
    }

    // endregion

}
