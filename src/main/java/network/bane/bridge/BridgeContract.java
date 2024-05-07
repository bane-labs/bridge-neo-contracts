package network.bane.bridge;

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
import io.neow3j.devpack.annotations.EventParameterNames;
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
import network.bane.lib.GasBridgeLib;
import network.bane.structs.BridgeDeploymentData;
import network.bane.structs.Withdrawal;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Helper.concat;
import static io.neow3j.devpack.Helper.toByteArray;
import static io.neow3j.devpack.Runtime.checkWitness;
import static io.neow3j.devpack.Runtime.getCallingScriptHash;
import static io.neow3j.devpack.Runtime.getExecutingScriptHash;
import static network.bane.TestPaddingContract.padToBytes;
import static network.bane.lib.BridgeLib.HASH160_SIZE;
import static network.bane.lib.BridgeLib.UINT256_SIZE;
import static network.bane.lib.BridgeLib.computeNewRoot;
import static network.bane.lib.BridgeLib.subsequentNonces;
import static network.bane.lib.GasBridgeLib.hashGasBridgeOp;
import static network.bane.lib.StorageConstants.KEY_BRIDGE_MANAGEMENT;
import static network.bane.lib.StorageConstants.KEY_GAS_DEPOSIT_FEE;
import static network.bane.lib.StorageConstants.KEY_GAS_DEPOSIT_MAX_AMOUNT;
import static network.bane.lib.StorageConstants.KEY_GAS_DEPOSIT_MIN_AMOUNT;
import static network.bane.lib.StorageConstants.KEY_GAS_DEPOSIT_NONCE;
import static network.bane.lib.StorageConstants.KEY_GAS_DEPOSIT_ROOT;
import static network.bane.lib.StorageConstants.KEY_GAS_WITHDRAWAL_NONCE;
import static network.bane.lib.StorageConstants.KEY_GAS_WITHDRAWAL_ROOT;
import static network.bane.lib.StorageConstants.KEY_LOCKED;
import static network.bane.lib.StorageConstants.PREFIX_BASE;
import static network.bane.lib.StorageConstants.PREFIX_GAS_CLAIMABLES;

@DisplayName("NeoXBridge")
@Permission(nativeContract = NativeContract.GasToken, methods = "transfer")
@Permission(nativeContract = NativeContract.ContractManagement, methods = "update")
@ManifestExtra(key = "Author", value = "BaneLabs")
@ManifestExtra(key = "Target", value = "Neo X TestNet T3")
@ManifestExtra(key = "Description", value = "Contract for bridging GAS and tokens between Neo N3 and Neo X.")
public class BridgeContract {
    // region initsslot setup

    private static final StorageContext ctx = Storage.getStorageContext();
    private static final CryptoLib cryptoLib = new CryptoLib();
    private static final GasToken gasToken = new GasToken();
    private static final ContractManagement contractManagement = new ContractManagement();

    // base map and keys
    private static final StorageMap baseMap = new StorageMap(ctx, PREFIX_BASE);
    // gas claim map
    private static final StorageMap gasClaimableMap = new StorageMap(ctx, PREFIX_GAS_CLAIMABLES);

    // endregion
    // region events

    @DisplayName("GasDeposit")
    @EventParameterNames({"Nonce", "Amount", "Recipient", "Depositor", "DepositHash", "NewDepositRoot"})
    public static Event6Args<Integer, Integer, Hash160, Hash160, ByteString, ByteString> onGasDeposit;

    @DisplayName("GasWithdrawal")
    @EventParameterNames({"Nonce", "Amount", "Recipient"})
    public static Event3Args<Integer, Integer, Hash160> onGasWithdrawal;

    @DisplayName("GasClaimable")
    @EventParameterNames({"Nonce", "Amount", "Recipient"})
    public static Event3Args<Integer, Integer, Hash160> onGasClaimable;

    @DisplayName("GasClaim")
    @EventParameterNames({"Nonce", "Amount", "Recipient"})
    public static Event3Args<Integer, Integer, Hash160> onGasClaim;

    @DisplayName("GasDepositFeeChange")
    @EventParameterNames({"NewFee"})
    public static Event1Arg<Integer> onGasDepositFeeChange;

    @DisplayName("MinGasDepositChange")
    @EventParameterNames({"NewMinDeposit"})
    public static Event1Arg<Integer> onMinGasDepositChange;

    @DisplayName("MaxGasDepositChange")
    @EventParameterNames({"NewMaxDeposit"})
    public static Event1Arg<Integer> onMaxGasDepositChange;

    // endregion
    // region deployment/update

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

    public static void update(ByteString nef, String manifest) {
        if (!isLocked()) abort("Contract needs to be locked to update.");
        if (!checkWitness(owner())) abort("Only the owner can update this contract.");
        contractManagement.update(nef, manifest);
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
    // region OnNEP17Payment

    @OnNEP17Payment
    public static void onNep17Payment(Hash160 from, int amountWithFee, Object data) {
        if (isLocked()) abort("Contract is locked.");
        if (getCallingScriptHash() != gasToken.getHash()) abort("Only GAS is accepted.");
        Hash160 to = (Hash160) data;
        if (!Hash160.isValid(to)) abort("Invalid recipient data.");
        if (to.isZero()) abort("Recipient must not be zero.");

        int depositFee = gasDepositFee();
        if (amountWithFee < minGasDeposit() + depositFee) abort("Deposit amount is too low.");
        if (amountWithFee > maxGasDeposit() + depositFee) abort("Deposit amount is too high.");
        int depositAmount = amountWithFee - depositFee;

        int nonce = incrementGasDepositNonce();
        ByteString depositHash = hashGasBridgeOp(cryptoLib, nonce, depositAmount, to);
        ByteString newRoot = computeNewRoot(cryptoLib, baseMap.get(KEY_GAS_DEPOSIT_ROOT), depositHash);
        baseMap.put(KEY_GAS_DEPOSIT_ROOT, newRoot);
        onGasDeposit.fire(nonce, depositAmount, to, from, depositHash, newRoot);
    }

    // endregion
    // region gas bridge
    // region gas deposit/withdrawal/claim

    public static void depositGas(Hash160 from, Hash160 to, int depositAmount) {
        Hash160 executingScriptHash = getExecutingScriptHash();
        if (executingScriptHash.equals(from)) abort("Invalid 'from' parameter.");
        if (!gasToken.transfer(from, executingScriptHash, depositAmount + gasDepositFee(), to)) {
            abort("Transfer failed.");
        }
    }

    public static void claimGas(int nonce) {
        if (isLocked()) abort("Contract is locked.");
        ByteString claimable = gasClaimableMap.get(nonce);
        if (claimable == null) abort("No claim for this nonce.");
        if (claimable.length() != HASH160_SIZE + UINT256_SIZE) abort("Invalid claimable data.");

        gasClaimableMap.delete(nonce);

        Hash160 to = new Hash160(claimable.take(HASH160_SIZE));
        int amount = claimable.last(UINT256_SIZE).toInt();
        if (gasToken.transfer(getExecutingScriptHash(), to, amount, null)) {
            onGasClaim.fire(nonce, amount, to);
        } else {
            abort("Claim transfer failed.");
        }
    }

    public static void withdrawGas(ByteString withdrawalRoot, Map<ECPoint, ByteString> signatures,
            List<Withdrawal> withdrawals) {
        if (isLocked()) abort("Contract is locked.");
        if (!checkWitness(relayer())) abort("Only the relayer can call this method.");
        if (!GasBridgeLib.computeNewTopRoot(cryptoLib, gasWithdrawalRoot(), withdrawals).equals(withdrawalRoot)) {
            abort("Invalid root.");
        }
        if (!verifyValidatorSignatures(signatures, withdrawalRoot)) abort("Invalid validator signatures provided.");

        if (withdrawals.size() <= 0) abort("At least one withdrawal is required.");
        if (!subsequentNonces(withdrawals, currentNonce())) abort("Provided withdrawals are not subsequent.");

        baseMap.put(KEY_GAS_WITHDRAWAL_NONCE, withdrawals.get(withdrawals.size() - 1).nonce);
        ByteString formerWithdrawalRoot = gasWithdrawalRoot();
        baseMap.put(KEY_GAS_WITHDRAWAL_ROOT, withdrawalRoot);
        verifyGasWithdrawalsAndTransfer(formerWithdrawalRoot, withdrawals);
    }

    // endregion
    // region gas withdrawal helpers

    private static void verifyGasWithdrawalsAndTransfer(ByteString formerWithdrawalRoot, List<Withdrawal> withdrawals) {
        // Hash Tree verification
        ByteString parent = formerWithdrawalRoot;
        for (int i = 0; i < withdrawals.size(); i++) {
            Withdrawal withdrawal = withdrawals.get(i);
            if (!Withdrawal.isValid(withdrawal)) abort("Invalid withdrawal provided.");
            ByteString withdrawalHash = hashGasBridgeOp(cryptoLib, withdrawal.nonce, withdrawal.amount, withdrawal.to);
            parent = computeNewRoot(cryptoLib, parent, withdrawalHash);
        }
        if (parent != gasWithdrawalRoot()) {
            abort("Provided withdrawals do not match the withdrawal root.");
        }

        // Once this is reached, execute the withdrawals
        for (int i = 0; i < withdrawals.size(); i++) {
            Withdrawal withdrawal = withdrawals.get(i);
            if (!isContract(withdrawal.to) &&
                    gasToken.transfer(getExecutingScriptHash(), withdrawal.to, withdrawal.amount, null)) {
                onGasWithdrawal.fire(withdrawal.nonce, withdrawal.amount, withdrawal.to);
            } else {
                // Add the withdrawal to the claim map if either the recipient was a contract, or the transfer failed.
                addGasClaimable(withdrawal);
                onGasClaimable.fire(withdrawal.nonce, withdrawal.amount, withdrawal.to);
            }
        }
    }

    private static void addGasClaimable(Withdrawal withdrawal) {
        gasClaimableMap.put(withdrawal.nonce, concat(withdrawal.to.toByteArray(),
                padToBytes(toByteArray(withdrawal.amount), UINT256_SIZE)));
    }

    private static boolean isContract(Hash160 scriptHash) {
        return contractManagement.getContract(scriptHash) != null;
    }

    // Todo: Move this verification to the bridge management contract.
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

    private static int currentNonce() {
        return baseMap.getInt(KEY_GAS_WITHDRAWAL_NONCE);
    }

    /**
     * Increments the Gas deposit nonce and returns the new value.
     *
     * @return the new nonce value.
     */
    private static int incrementGasDepositNonce() {
        int nextNonce = baseMap.getInt(KEY_GAS_DEPOSIT_NONCE) + 1;
        baseMap.put(KEY_GAS_DEPOSIT_NONCE, nextNonce);
        return nextNonce;
    }

    // endregion
    // region gas bridge configuration

    @Safe
    public static int gasDepositFee() {
        return baseMap.getInt(KEY_GAS_DEPOSIT_FEE);
    }

    public static void setGasDepositFee(int fee) {
        if (!checkWitness(governor())) abort("Only the governor can set the deposit fee.");
        if (fee < 0) abort("Deposit fee must be nonnegative.");
        baseMap.put(KEY_GAS_DEPOSIT_FEE, fee);
        onGasDepositFeeChange.fire(fee);
    }

    public static void setMinGasDeposit(int newMinDeposit) {
        if (!checkWitness(governor())) abort("Only the governor can set the minimum deposit.");
        if (newMinDeposit < 0) abort("Minimum deposit must be nonnegative.");
        if (newMinDeposit > maxGasDeposit()) abort("Minimum deposit must be less than the maximum deposit.");
        baseMap.put(KEY_GAS_DEPOSIT_MIN_AMOUNT, newMinDeposit);
        onMinGasDepositChange.fire(newMinDeposit);
    }

    public static void setMaxGasDeposit(int newMaxDeposit) {
        if (!checkWitness(governor())) abort("Only the governor can set the maximum deposit.");
        if (newMaxDeposit < minGasDeposit()) abort("Maximum deposit must be greater than the minimum deposit.");
        baseMap.put(KEY_GAS_DEPOSIT_MAX_AMOUNT, newMaxDeposit);
        onMaxGasDepositChange.fire(newMaxDeposit);
    }

    // endregion
    // endregion
    // region token bridge
    // region token register

    // Todo: Implement token register and unregister

    // endregion token register
    // region token deposit

    // Todo: Implement token deposit

    // endregion
    // region token withdrawal

    // Todo: Implement token withdrawal

    // endregion
    // region token claim

    // Todo: Implement token claim

    // endregion
    // endregion token bridge
    // region token setters

    // Todo: Implement token setters

    // endregion
    // region getters
    // region management getters

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
    // region bridge getters

    @Safe
    public static Hash160 management() {
        return baseMap.getHash160(KEY_BRIDGE_MANAGEMENT);
    }

    @Safe
    public static int minGasDeposit() {
        return baseMap.getInt(KEY_GAS_DEPOSIT_MIN_AMOUNT);
    }

    @Safe
    public static int maxGasDeposit() {
        return baseMap.getInt(KEY_GAS_DEPOSIT_MAX_AMOUNT);
    }

    @Safe
    public static ByteString gasDepositRoot() {
        return baseMap.get(KEY_GAS_DEPOSIT_ROOT);
    }

    @Safe
    public static ByteString gasWithdrawalRoot() {
        return baseMap.get(KEY_GAS_WITHDRAWAL_ROOT);
    }

    @Safe
    public static int gasDepositNonce() {
        return baseMap.getInt(KEY_GAS_DEPOSIT_NONCE);
    }

    @Safe
    public static int gasWithdrawalNonce() {
        return baseMap.getInt(KEY_GAS_WITHDRAWAL_NONCE);
    }

    // endregion
    // endregion

}
