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
import io.neow3j.devpack.constants.NativeContract;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.contracts.CryptoLib;
import io.neow3j.devpack.contracts.GasToken;
import io.neow3j.devpack.contracts.StdLib;
import io.neow3j.devpack.events.Event1Arg;
import io.neow3j.devpack.events.Event2Args;
import io.neow3j.devpack.events.Event3Args;
import io.neow3j.devpack.events.Event4Args;
import io.neow3j.devpack.events.Event6Args;
import io.neow3j.devpack.events.Event7Args;
import network.bane.structs.BridgeDeploymentData;
import network.bane.structs.GasBridgePaymentData;
import network.bane.structs.TokenBridge;
import network.bane.structs.Withdrawal;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Runtime.checkWitness;
import static io.neow3j.devpack.Runtime.getCallingScriptHash;
import static network.bane.bridge.BridgeHelper.managementContract;
import static network.bane.bridge.BridgeHelper.onlyGovernor;
import static network.bane.bridge.BridgeHelper.onlyPaused;
import static network.bane.bridge.BridgeHelper.onlyRelayer;
import static network.bane.bridge.BridgeHelper.onlySecurityGuard;
import static network.bane.bridge.BridgeHelper.onlyUnpaused;
import static network.bane.bridge.StorageConstants.PREFIX_TOKEN_BRIDGES;
import static network.bane.bridge.TokenBridgeImpl.onlyTokenBridgePaused;
import static network.bane.bridge.TokenBridgeImpl.onlyTokenBridgeUnpaused;
import static network.bane.bridge.StorageConstants.KEY_BRIDGE_MANAGEMENT;
import static network.bane.bridge.StorageConstants.KEY_GAS_DEPOSIT_FEE;
import static network.bane.bridge.StorageConstants.KEY_GAS_DEPOSIT_MAX_AMOUNT;
import static network.bane.bridge.StorageConstants.KEY_GAS_DEPOSIT_MIN_AMOUNT;
import static network.bane.bridge.StorageConstants.KEY_GAS_DEPOSIT_NONCE;
import static network.bane.bridge.StorageConstants.KEY_GAS_DEPOSIT_ROOT;
import static network.bane.bridge.StorageConstants.KEY_GAS_WITHDRAWAL_NONCE;
import static network.bane.bridge.StorageConstants.KEY_GAS_WITHDRAWAL_ROOT;
import static network.bane.bridge.StorageConstants.KEY_PAUSED;
import static network.bane.bridge.StorageConstants.PREFIX_BASE;

@DisplayName("NeoXBridge")
@Permission(nativeContract = NativeContract.GasToken, methods = "transfer")
@Permission(nativeContract = NativeContract.ContractManagement, methods = "update")
@ManifestExtra(key = "Author", value = "BaneLabs")
@ManifestExtra(key = "Target", value = "Neo X TestNet T3")
@ManifestExtra(key = "Description", value = "Contract for bridging GAS and tokens between Neo N3 and Neo X.")
public class BridgeContract {
    static final StorageContext ctx = Storage.getStorageContext();
    static final CryptoLib cryptoLib = new CryptoLib();
    static final GasToken gasToken = new GasToken();

    // baseMap is used to store gas-related state and general contract information, i.e., management contract and
    // pause status.
    static final StorageMap baseMap = new StorageMap(ctx, PREFIX_BASE);

    // This needs to be here due to the neow3j compiler. Technically, the Java compiler does not see this as a
    // constant value during compile time because it uses an instantiation with new. Static fields that are not
    // considered final must be in the main contract file.
    static final byte[] PREFIX_TOKEN_CLAIMABLES = new byte[]{StorageConstants.PREFIX_TOKEN_CLAIMABLES};

    // region events
    // region gas bridge events

    @DisplayName("GasDeposit")
    @EventParameterNames({"Nonce", "Amount", "Recipient", "Depositor", "DepositHash", "NewDepositRoot"})
    static Event6Args<Integer, Integer, Hash160, Hash160, ByteString, ByteString> onGasDeposit;

    @DisplayName("GasClaimable")
    @EventParameterNames({"Nonce", "Amount", "Recipient"})
    static Event3Args<Integer, Integer, Hash160> onGasClaimable;

    @DisplayName("GasClaim")
    @EventParameterNames({"Nonce", "Amount", "Recipient"})
    static Event3Args<Integer, Integer, Hash160> onGasClaim;

    @DisplayName("GasWithdrawal")
    @EventParameterNames({"Nonce", "Amount", "Recipient"})
    static Event3Args<Integer, Integer, Hash160> onGasWithdrawal;

    @DisplayName("GasDepositFeeChange")
    @EventParameterNames({"NewFee"})
    static Event1Arg<Integer> onGasDepositFeeChange;

    @DisplayName("MinGasDepositChange")
    @EventParameterNames({"NewMinDeposit"})
    static Event1Arg<Integer> onMinGasDepositChange;

    @DisplayName("MaxGasDepositChange")
    @EventParameterNames({"NewMaxDeposit"})
    static Event1Arg<Integer> onMaxGasDepositChange;

    // endregion
    // region token bridge events

    @DisplayName("TokenRegister")
    @EventParameterNames({"TokenHash", "TokenConfig"})
    static Event2Args<Hash160, TokenBridge.TokenConfig> onTokenRegister;

    @DisplayName("TokenUnregister")
    @EventParameterNames({"TokenHash"})
    static Event1Arg<Hash160> onTokenUnregister;

    @DisplayName("TokenBridgePause")
    @EventParameterNames({"TokenHash"})
    static Event1Arg<Hash160> onTokenBridgePause;

    @DisplayName("TokenBridgeUnpause")
    @EventParameterNames({"TokenHash"})
    static Event1Arg<Hash160> onTokenBridgeUnpause;

    @DisplayName("TokenDeposit")
    @EventParameterNames({"TokenHash", "Nonce", "To", "Value", "Depositor", "DepositHash", "NewDepositRoot"})
    static Event7Args<Hash160, Integer, Hash160, Integer, Hash160, ByteString, ByteString> onTokenDeposit;

    @DisplayName("TokenClaimable")
    @EventParameterNames({"TokenHash", "Nonce", "To", "Value"})
    static Event4Args<Hash160, Integer, Hash160, Integer> onTokenClaimable;

    @DisplayName("TokenClaim")
    @EventParameterNames({"TokenHash", "Nonce", "To", "Value"})
    static Event4Args<Hash160, Integer, Integer, Hash160> onTokenClaim;

    @DisplayName("TokenWithdrawal")
    @EventParameterNames({"TokenHash", "Nonce", "To", "Value"})
    static Event4Args<Hash160, Integer, Hash160, Integer> onTokenWithdrawal;

    @DisplayName("TokenDepositFeeChange")
    @EventParameterNames({"TokenHash", "NewFee"})
    static Event2Args<Hash160, Integer> onTokenDepositFeeChange;

    @DisplayName("MinTokenDepositChange")
    @EventParameterNames({"TokenHash", "NewMinDeposit"})
    static Event2Args<Hash160, Integer> onMinTokenDepositChange;

    @DisplayName("MaxTokenDepositChange")
    @EventParameterNames({"TokenHash", "NewMaxDeposit"})
    static Event2Args<Hash160, Integer> onMaxTokenDepositChange;

    @DisplayName("MaxTokenWithdrawalsChange")
    @EventParameterNames({"TokenHash", "NewMaxWithdrawals"})
    static Event2Args<Hash160, Integer> onMaxTokenWithdrawalsChange;

    // endregion
    // endregion
    // region deployment/update

    @OnDeployment
    public static void deploy(Object data, boolean isUpdate) {
        if (!isUpdate) {
            BridgeDeploymentData deploymentData = (BridgeDeploymentData) data;
            if (deploymentData.bridgeManagementContract == null ||
                    !Hash160.isValid(deploymentData.bridgeManagementContract))
                abort("Invalid bridge management contract hash.");
            if (deploymentData.gasDepositFee < 0) abort("Deposit fee must be nonnegative.");
            if (deploymentData.minGasDeposit < 0) abort("Minimum deposit must be nonnegative.");
            if (deploymentData.maxGasDeposit < deploymentData.minGasDeposit)
                abort("Maximum deposit must be greater than the minimum deposit.");

            baseMap.put(KEY_BRIDGE_MANAGEMENT, deploymentData.bridgeManagementContract);
            baseMap.put(KEY_GAS_DEPOSIT_FEE, deploymentData.gasDepositFee);
            baseMap.put(KEY_GAS_DEPOSIT_MIN_AMOUNT, deploymentData.minGasDeposit);
            baseMap.put(KEY_GAS_DEPOSIT_MAX_AMOUNT, deploymentData.maxGasDeposit);

            // Initial deposit and withdrawal roots will be zero hashes
            baseMap.put(KEY_GAS_DEPOSIT_ROOT, Hash256.zero());
            baseMap.put(KEY_GAS_WITHDRAWAL_ROOT, Hash256.zero());

            baseMap.put(KEY_GAS_DEPOSIT_NONCE, 0);
            baseMap.put(KEY_GAS_WITHDRAWAL_NONCE, 0);
            baseMap.put(KEY_PAUSED, false);

            if (!checkWitness(managementContract().owner())) {
                abort("Owner must witness the deployment.");
            }
        }
    }

    public static void update(ByteString nef, String manifest) {
        onlyPaused();
        if (!checkWitness(managementContract().owner())) abort("Only the owner can update this contract.");
        new ContractManagement().update(nef, manifest);
    }

    // endregion
    // region pause/unpause

    public static void pause() {
        onlyUnpaused();
        onlySecurityGuard();
        baseMap.put(KEY_PAUSED, true);
    }

    public static void unpause() {
        onlyPaused();
        onlyGovernor();
        baseMap.put(KEY_PAUSED, false);
    }

    @Safe
    public static boolean isPaused() {
        return baseMap.getBoolean(KEY_PAUSED);
    }

    // endregion
    // region OnNEP17Payment

    /**
     * Generally any token payment of a registered token is accepted regardless of the provided sender, amount or data.
     * <p>
     * If the token sent is the GAS token, the payment is always accepted if the {@code from} parameter is null. If
     * this is not the case it is only accepted if either the {@code data} parameter is null, or the {@code data}
     * parameter is an array of a valid non-zero {@link Hash160} value and an integer. In the latter case, a bridge
     * deposit is initiated, resulting in an updated deposit state of the GasBridge. This is an advanced case. Usual
     * bridge deposits should happen using the depositGas method.
     *
     * @param from   the sender.
     * @param amount the amount.
     * @param data   the data provided. If the transfer should be used to directly initiate a bridge deposit, make
     *               sure to provide the data in the format of a {@link GasBridgePaymentData} object, i.e., an array
     *               contract parameter. Its minBridgeAmount should be set to the amount sent minus the current
     *               deposit fee. It ensures, that this minBridgeAmount will be the amount that is actually bridged,
     *               and prevents any eventual front-running issues if the deposit fee would be manipulated by a
     *               malicious governor.
     */
    @OnNEP17Payment
    public static void onNep17Payment(Hash160 from, int amount, Object data) {
        onlyUnpaused();
        Hash160 callingScriptHash = getCallingScriptHash();
        if (callingScriptHash.equals(gasToken.getHash())) {
            // Accept GAS rewards from holding NEO
            if (from == null) return;
            if (data != null) {
                // If there's data provided in a GAS transfer, it is handled as a bridge deposit.
                GasBridgePaymentData paymentData = (GasBridgePaymentData) data;
                if (!GasBridgePaymentData.isValid(paymentData)) abort("Invalid payment data.");
                int bridgeAmount = amount - gasDepositFee();
                if (bridgeAmount < paymentData.minBridgeAmount) abort("Amount below defined minimum.");
                GasBridgeImpl.updateGasDepositState(from, paymentData.to, bridgeAmount);
            }
            return;
        } else if (new StorageMap(BridgeContract.ctx, PREFIX_TOKEN_BRIDGES).get(callingScriptHash) != null) {
            if (data != null) {
                abort("No data accepted.");
            }
            return;
        } else {
            abort("Unregistered token.");
        }
    }

    // endregion
    // region gas bridge
    // region gas deposit/claim/withdrawal

    /**
     * Deposit GAS to Neo X.
     *
     * @param from   the sender.
     * @param to     the recipient on Neo X.
     * @param amount the amount of GAS to deposit to Neo X.
     */
    public static void depositGas(Hash160 from, Hash160 to, int amount) {
        onlyUnpaused();
        GasBridgeImpl.depositGas(from, to, amount);
    }

    /**
     * Claim GAS that has been withdrawn from Neo X.
     * <p>
     * Only withdrawals that have a contract as recipient or for which the transfer has failed (should never happen
     * as long as the deposited GAS funds are held in this contract) are available to claim.
     * <p>
     * In order to claim a withdrawal, provide the nonce of the withdrawal and the amount will be transferred to the
     * already specified recipient.
     *
     * @param nonce the nonce of the withdrawal that is claimable.
     */
    public static void claimGas(int nonce) {
        onlyUnpaused();
        GasBridgeImpl.claimGas(nonce);
    }

    /**
     * Withdraws GAS from the contract (i.e., from Neo X) to the provided recipients.
     * <p>
     * Requires the signatures of the validators. The signatures must sign the provided withdrawal root, while the
     * provided withdrawal root must be the computed root based on the current root in storage and the provided
     * withdrawals.
     * <p>
     * Withdrawals to contracts are made available for claiming and are not directly transferred due to uncertain
     * computation costs. Additionaly, if a transfer fails, the withdrawal is also made available for claiming. This
     * should never happen as long as the deposited GAS funds are held in this contract.
     *
     * @param withdrawalRoot the new withdrawal root.
     * @param signatures     the signatures of the validators.
     * @param withdrawals    the withdrawals to execute.
     */
    public static void withdrawGas(ByteString withdrawalRoot, Map<ECPoint, ByteString> signatures,
            List<Withdrawal> withdrawals) {
        onlyRelayer();
        onlyUnpaused();
        GasBridgeImpl.withdrawGas(withdrawalRoot, signatures, withdrawals);
    }

    // endregion
    // region gas bridge configuration

    @Safe
    public static int gasDepositFee() {
        return baseMap.getInt(KEY_GAS_DEPOSIT_FEE);
    }

    public static void setGasDepositFee(int fee) {
        onlyGovernor();
        if (fee < 0) abort("Deposit fee must be nonnegative.");
        baseMap.put(KEY_GAS_DEPOSIT_FEE, fee);
        onGasDepositFeeChange.fire(fee);
    }

    public static void setMinGasDeposit(int newMinDeposit) {
        onlyGovernor();
        if (newMinDeposit < 0) abort("Minimum deposit must be nonnegative.");
        if (newMinDeposit > maxGasDeposit()) abort("Minimum deposit must be less than the maximum deposit.");
        baseMap.put(KEY_GAS_DEPOSIT_MIN_AMOUNT, newMinDeposit);
        onMinGasDepositChange.fire(newMinDeposit);
    }

    public static void setMaxGasDeposit(int newMaxDeposit) {
        onlyGovernor();
        if (newMaxDeposit < minGasDeposit()) abort("Maximum deposit must be greater than the minimum deposit.");
        baseMap.put(KEY_GAS_DEPOSIT_MAX_AMOUNT, newMaxDeposit);
        onMaxGasDepositChange.fire(newMaxDeposit);
    }

    // endregion
    // endregion
    // region token bridge
    // region token register

    public static void registerToken(Hash160 token, TokenBridge.TokenConfig tokenConfig) {
        onlyGovernor();
        if (!TokenBridge.TokenConfig.isValid(tokenConfig)) abort("Invalid token configuration.");
        TokenBridgeImpl.registerToken(token, tokenConfig);
        onTokenRegister.fire(token, tokenConfig);
    }

    public static void unregisterToken(Hash160 token) {
        onlyGovernor();
        onlyTokenBridgePaused(token);
        TokenBridgeImpl.unregisterToken(token);
        onTokenUnregister.fire(token);
    }

    @Safe
    public static TokenBridge getTokenBridge(Hash160 token) {
        return TokenBridgeImpl.checkRegisteredAndGetTokenBridge(token);
    }

    // endregion token register
    // region token pausing

    public static void pauseTokenBridge(Hash160 token) {
        onlySecurityGuard();
        onlyTokenBridgeUnpaused(token);
        TokenBridgeImpl.pauseTokenBridge(token);
        onTokenBridgePause.fire(token);
    }

    public static void unpauseTokenBridge(Hash160 token) {
        onlyGovernor();
        onlyTokenBridgePaused(token);
        TokenBridgeImpl.unpauseTokenBridge(token);
        onTokenBridgeUnpause.fire(token);
    }

    // endregion
    // region token deposit

    public static void depositToken(Hash160 token, Hash160 from, Hash160 to, int amount) {
        onlyUnpaused();
        onlyTokenBridgeUnpaused(token);
        TokenBridgeImpl.depositToken(token, from, to, amount);
    }

    // endregion
    // region token withdrawal

    public static void withdrawToken(Hash160 token, ByteString withdrawalRoot, Map<ECPoint, ByteString> signatures,
            List<Withdrawal> withdrawals) {
        onlyUnpaused();
        onlyTokenBridgeUnpaused(token);
        TokenBridgeImpl.withdrawToken(token, withdrawalRoot, signatures, withdrawals);
    }

    // endregion
    // region token claim

    public static void claimToken(Hash160 token, int nonce) {
        onlyUnpaused();
        onlyTokenBridgeUnpaused(token);
        TokenBridgeImpl.claimToken(token, nonce);
    }

    // endregion
    // endregion token bridge
    // region token setters

    public static void setTokenDepositFee(List<Hash160> tokens, List<Integer> newDepositFees) {
        onlyGovernor();
        int nrTokens = tokens.size();
        if (nrTokens != newDepositFees.size()) abort("Length mismatch.");
        StorageMap tokenBridgesMap = new StorageMap(BridgeContract.ctx, PREFIX_TOKEN_BRIDGES);
        for (int i = 0; i < nrTokens; i++) {
            Hash160 token = tokens.get(i);
            int newFee = newDepositFees.get(i);
            TokenBridge tokenBridge = TokenBridgeImpl.checkRegisteredAndGetTokenBridge(token);
            tokenBridge.config.fee = newFee;
            tokenBridgesMap.put(token, new StdLib().serialize(tokenBridge));
            onTokenDepositFeeChange.fire(token, newFee);
        }
    }

    public static void setTokenMinAmount(List<Hash160> tokens, List<Integer> newMinAmounts) {
        onlyGovernor();
        int nrTokens = tokens.size();
        if (nrTokens != newMinAmounts.size()) abort("Length mismatch.");
        StorageMap tokenBridgesMap = new StorageMap(BridgeContract.ctx, PREFIX_TOKEN_BRIDGES);
        for (int i = 0; i < nrTokens; i++) {
            Hash160 token = tokens.get(i);
            int newMinAmount = newMinAmounts.get(i);
            TokenBridge tokenBridge = TokenBridgeImpl.checkRegisteredAndGetTokenBridge(token);
            tokenBridge.config.minAmount = newMinAmount;
            tokenBridgesMap.put(token, new StdLib().serialize(tokenBridge));
            onMinTokenDepositChange.fire(token, newMinAmount);
        }
    }

    public static void setTokenMaxAmount(List<Hash160> tokens, List<Integer> newMaxAmounts) {
        onlyGovernor();
        int nrTokens = tokens.size();
        if (nrTokens != newMaxAmounts.size()) abort("Length mismatch.");
        StorageMap tokenBridgesMap = new StorageMap(BridgeContract.ctx, PREFIX_TOKEN_BRIDGES);
        for (int i = 0; i < nrTokens; i++) {
            Hash160 token = tokens.get(i);
            int newMaxAmount = newMaxAmounts.get(i);
            TokenBridge tokenBridge = TokenBridgeImpl.checkRegisteredAndGetTokenBridge(token);
            tokenBridge.config.maxAmount = newMaxAmount;
            tokenBridgesMap.put(token, new StdLib().serialize(tokenBridge));
            onMaxTokenDepositChange.fire(token, newMaxAmount);
        }
    }

    public static void setTokenMaxWithdrawals(List<Hash160> tokens, List<Integer> newMaxWithdrawals) {
        onlyGovernor();
        int nrTokens = tokens.size();
        if (nrTokens != newMaxWithdrawals.size()) abort("Length mismatch.");
        StorageMap tokenBridgesMap = new StorageMap(BridgeContract.ctx, PREFIX_TOKEN_BRIDGES);
        for (int i = 0; i < nrTokens; i++) {
            Hash160 token = tokens.get(i);
            int newMaxWithdrawal = newMaxWithdrawals.get(i);
            TokenBridge tokenBridge = TokenBridgeImpl.checkRegisteredAndGetTokenBridge(token);
            tokenBridge.config.maxWithdrawals = newMaxWithdrawal;
            tokenBridgesMap.put(token, new StdLib().serialize(tokenBridge));
            onMaxTokenWithdrawalsChange.fire(token, newMaxWithdrawal);
        }
    }

    // endregion
    // region getters
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
