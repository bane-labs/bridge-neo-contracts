package network.bane.bridge;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.ECPoint;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Hash256;
import io.neow3j.devpack.Iterator;
import io.neow3j.devpack.List;
import io.neow3j.devpack.Map;
import io.neow3j.devpack.Storage;
import io.neow3j.devpack.StorageContext;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.annotations.DisplayName;
import io.neow3j.devpack.annotations.EventParameterNames;
import io.neow3j.devpack.annotations.ManifestExtra.ManifestExtras;
import io.neow3j.devpack.annotations.ManifestExtra;
import io.neow3j.devpack.annotations.OnDeployment;
import io.neow3j.devpack.annotations.OnNEP17Payment;
import io.neow3j.devpack.annotations.Permission;
import io.neow3j.devpack.annotations.Safe;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.contracts.CryptoLib;
import io.neow3j.devpack.contracts.GasToken;
import io.neow3j.devpack.contracts.StdLib;
import io.neow3j.devpack.events.Event;
import io.neow3j.devpack.events.Event1Arg;
import io.neow3j.devpack.events.Event2Args;
import io.neow3j.devpack.events.Event3Args;
import io.neow3j.devpack.events.Event4Args;
import io.neow3j.devpack.events.Event6Args;
import io.neow3j.devpack.events.Event8Args;
import network.bane.structs.BridgeDeploymentData;
import network.bane.structs.GasBridge;
import network.bane.structs.State;
import network.bane.structs.TokenBridge;
import network.bane.structs.Withdrawal;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Runtime.checkWitness;
import static io.neow3j.devpack.Runtime.getCallingScriptHash;
import static network.bane.bridge.BridgeHelper.managementContract;
import static network.bane.bridge.BridgeHelper.onlyGovernor;
import static network.bane.bridge.BridgeHelper.onlyGovernorOrSecurityGuard;
import static network.bane.bridge.BridgeHelper.onlyWhenPaused;
import static network.bane.bridge.BridgeHelper.onlyRelayer;
import static network.bane.bridge.BridgeHelper.onlyWhenNotPaused;
import static network.bane.bridge.BridgeImpl.enteringNonReentrant;
import static network.bane.bridge.BridgeImpl.exitingNonReentrant;
import static network.bane.bridge.GasBridgeImpl.onlyWhenDepositsNotPaused;
import static network.bane.bridge.GasBridgeImpl.onlyWhenDepositsPaused;
import static network.bane.bridge.GasBridgeImpl.onlyWhenGasBridgePaused;
import static network.bane.bridge.GasBridgeImpl.onlyWhenGasBridgeNotPaused;
import static network.bane.bridge.StorageConstants.KEY_DEPOSIT_PAUSE;
import static network.bane.bridge.StorageConstants.KEY_BRIDGE_PAUSE;
import static network.bane.bridge.StorageConstants.KEY_ENTERED;
import static network.bane.bridge.StorageConstants.KEY_GAS_BRIDGE;
import static network.bane.bridge.StorageConstants.KEY_NEO_HOLDING_GAS_REWARDS;
import static network.bane.bridge.StorageConstants.KEY_VERSION;
import static network.bane.bridge.StorageConstants.KEY_UNCLAIMED_REWARDS;
import static network.bane.bridge.StorageConstants.PREFIX_TOKEN_BRIDGES;
import static network.bane.bridge.TokenBridgeImpl.onlyWhenTokenBridgePaused;
import static network.bane.bridge.TokenBridgeImpl.onlyWhenTokenBridgeNotPaused;
import static network.bane.bridge.StorageConstants.KEY_BRIDGE_MANAGEMENT;
import static network.bane.bridge.StorageConstants.PREFIX_BASE;

@DisplayName("NeoXBridge")
@Permission(contract = "*")
@ManifestExtras({
        @ManifestExtra(key = "Author", value = "BaneLabs"),
        @ManifestExtra(key = "Description", value = "Contract for bridging GAS and tokens between Neo N3 and Neo X."),
        @ManifestExtra(key = "Source", value = "https://github.com/bane-labs/bridge-neo-contracts")
})
public class BridgeContract {

    // This needs to be here due to the neow3j compiler. Technically, the Java compiler does not see this as a
    // constant value during compile time because it uses an instantiation with new. Static fields that are not
    // considered final must be in the main contract file.
    static final byte[] PREFIX_TOKEN_CLAIMABLES = new byte[]{StorageConstants.PREFIX_TOKEN_CLAIMABLES};

    // region static contract values

    static final StorageContext ctx = Storage.getStorageContext();
    static final CryptoLib cryptoLib = new CryptoLib();
    static final GasToken gasToken = new GasToken();

    // baseMap is used to store gas-related state and general contract information, i.e., management contract and
    // pause status.
    static final StorageMap baseMap = new StorageMap(ctx, PREFIX_BASE);

    // endregion
    // region events
    // region bridge events

    @DisplayName("BridgePause")
    static Event onBridgePause;

    @DisplayName("BridgeUnpause")
    static Event onBridgeUnpause;

    @DisplayName("DepositPause")
    static Event onDepositPause;

    @DisplayName("DepositUnpause")
    static Event onDepositUnpause;

    // endregion
    // region gas bridge events

    @DisplayName("GasBridgePause")
    static Event onGasBridgePause;

    @DisplayName("GasBridgeUnpause")
    static Event onGasBridgeUnpause;

    @DisplayName("GasDeposit")
    @EventParameterNames({"Nonce", "Recipient", "Amount", "Depositor", "DepositHash", "NewDepositRoot"})
    static Event6Args<Integer, Hash160, Integer, Hash160, ByteString, ByteString> onGasDeposit;

    @DisplayName("GasClaimable")
    @EventParameterNames({"Nonce", "Recipient", "Amount"})
    static Event3Args<Integer, Hash160, Integer> onGasClaimable;

    @DisplayName("GasClaim")
    @EventParameterNames({"Nonce", "Recipient", "Amount"})
    static Event3Args<Integer, Hash160, Integer> onGasClaim;

    @DisplayName("GasWithdrawal")
    @EventParameterNames({"Nonce", "Recipient", "Amount"})
    static Event3Args<Integer, Hash160, Integer> onGasWithdrawal;

    @DisplayName("GasWithdrawalRootUpdate")
    @EventParameterNames({"Nonce", "WithdrawalRoot"})
    static Event2Args<Integer, ByteString> onGasWithdrawalRootUpdate;

    @DisplayName("GasDepositFeeChange")
    @EventParameterNames({"NewFee"})
    static Event1Arg<Integer> onGasDepositFeeChange;

    @DisplayName("MinGasDepositChange")
    @EventParameterNames({"NewMinDeposit"})
    static Event1Arg<Integer> onMinGasDepositChange;

    @DisplayName("MaxGasDepositChange")
    @EventParameterNames({"NewMaxDeposit"})
    static Event1Arg<Integer> onMaxGasDepositChange;

    @DisplayName("MaxTotalDepositedGasChange")
    @EventParameterNames({"NewMaxTotalDepositedGas"})
    static Event1Arg<Integer> onMaxTotalDepositedGasChange;

    // endregion
    // region token bridge events

    @DisplayName("TokenRegister")
    @EventParameterNames({"NeoN3Token", "TokenConfig"})
    static Event2Args<Hash160, TokenBridge.TokenConfig> onTokenRegister;


    @DisplayName("TokenBridgePause")
    @EventParameterNames({"NeoN3Token", "NeoXToken"})
    static Event2Args<Hash160, Hash160> onTokenBridgePause;

    @DisplayName("TokenBridgeUnpause")
    @EventParameterNames({"NeoN3Token", "NeoXToken"})
    static Event2Args<Hash160, Hash160> onTokenBridgeUnpause;

    @DisplayName("TokenDeposit")
    @EventParameterNames({"NeoN3Token", "NeoXToken", "Nonce", "Recipient", "Value", "Depositor", "DepositHash",
            "NewDepositRoot"})
    static Event8Args<Hash160, Hash160, Integer, Hash160, Integer, Hash160, ByteString, ByteString> onTokenDeposit;

    @DisplayName("TokenClaimable")
    @EventParameterNames({"NeoN3Token", "Nonce", "Recipient", "Value"})
    static Event4Args<Hash160, Integer, Hash160, Integer> onTokenClaimable;

    @DisplayName("TokenClaim")
    @EventParameterNames({"NeoN3Token", "Nonce", "Recipient", "Value"})
    static Event4Args<Hash160, Integer, Hash160, Integer> onTokenClaim;

    @DisplayName("TokenWithdrawal")
    @EventParameterNames({"NeoN3Token", "Nonce", "Recipient", "Value"})
    static Event4Args<Hash160, Integer, Hash160, Integer> onTokenWithdrawal;

    @DisplayName("TokenWithdrawalRootUpdate")
    @EventParameterNames({"NeoN3Token", "NeoXToken", "Nonce", "WithdrawalRoot"})
    static Event4Args<Hash160, Hash160, Integer, ByteString> onTokenWithdrawalRootUpdate;

    @DisplayName("TokenDepositFeeChange")
    @EventParameterNames({"NeoN3Token", "NewFee"})
    static Event2Args<Hash160, Integer> onTokenDepositFeeChange;

    @DisplayName("MinTokenDepositChange")
    @EventParameterNames({"NeoN3Token", "NewMinDeposit"})
    static Event2Args<Hash160, Integer> onMinTokenDepositChange;

    @DisplayName("MaxTokenDepositChange")
    @EventParameterNames({"NeoN3Token", "NewMaxDeposit"})
    static Event2Args<Hash160, Integer> onMaxTokenDepositChange;

    @DisplayName("MaxTokenWithdrawalsChange")
    @EventParameterNames({"NeoN3Token", "NewMaxWithdrawals"})
    static Event2Args<Hash160, Integer> onMaxTokenWithdrawalsChange;

    // endregion
    // endregion
    // region deployment/update

    @OnDeployment
    public static void deploy(Object data, boolean isUpdate) {
        if (isUpdate) {
            if (baseMap.getInt(KEY_VERSION) != 0) abort("Invalid version.");
            // Update internal versioning.
            baseMap.put(KEY_VERSION, 1);
            baseMap.put(KEY_DEPOSIT_PAUSE, false);
            baseMap.put(KEY_NEO_HOLDING_GAS_REWARDS, 0);
            Map<Hash160, Integer> decimalScalingFactors = (Map<Hash160, Integer>) data;
            BridgeMigrationV1ToV2.migrateV1ToV2(decimalScalingFactors);
        } else {
            BridgeDeploymentData deploymentData = (BridgeDeploymentData) data;
            if (deploymentData.bridgeManagementContract == null ||
                    !Hash160.isValid(deploymentData.bridgeManagementContract))
                abort("Invalid bridge management contract hash.");

            baseMap.put(KEY_BRIDGE_MANAGEMENT, deploymentData.bridgeManagementContract);
            baseMap.put(KEY_BRIDGE_PAUSE, false);

            ByteString zeroHash = Hash256.zero().toByteString();
            State newDepositState = new State(0, zeroHash);
            State newWithdrawalState = new State(0, zeroHash);
            GasBridge gasBridge = new GasBridge(false, 0, newDepositState, newWithdrawalState,
                    deploymentData.gasConfig);
            if (!GasBridge.isValid(gasBridge)) abort("Invalid gas bridge.");
            ByteString serialize = new StdLib().serialize(gasBridge);
            baseMap.put(KEY_GAS_BRIDGE, serialize);
            baseMap.put(KEY_UNCLAIMED_REWARDS, 0);
            baseMap.put(KEY_DEPOSIT_PAUSE, false);
            baseMap.put(KEY_NEO_HOLDING_GAS_REWARDS, 0);

            BridgeContract.baseMap.put(KEY_ENTERED, false);
            baseMap.put(KEY_VERSION, 0);

            // Make sure the owner witnesses the deployment.
            if (!checkWitness(managementContract().owner())) {
                abort("Owner must witness the deployment.");
            }
        }
    }

    public static void update(ByteString nef, String manifest, Object data) {
        onlyWhenPaused();
        if (!checkWitness(managementContract().owner())) abort("Only the owner can update this contract.");
        new ContractManagement().update(nef, manifest, data);
    }

    // endregion
    // region pause/unpause

    /**
     * Pausing the bridge will reject any deposits and withdrawals.
     * <p>
     * This feature is useful to halt any interaction with the contract besides governor actions, such as updating
     * parameters or registering new token bridges, or contract updates.
     */
    public static void pauseBridge() {
        onlyWhenNotPaused();
        onlyGovernorOrSecurityGuard();
        baseMap.put(KEY_BRIDGE_PAUSE, true);
        onBridgePause.fire();
    }

    public static void unpauseBridge() {
        onlyWhenPaused();
        onlyGovernor();
        baseMap.put(KEY_BRIDGE_PAUSE, false);
        onBridgeUnpause.fire();
    }

    /**
     * @return true if the bridge is paused and no deposits nor withdrawals are accepted. False otherwise.
     */
    @Safe
    public static boolean isPaused() {
        return baseMap.getBoolean(KEY_BRIDGE_PAUSE);
    }

    /**
     * Pausing deposits will reject any incoming deposits to the bridge. This means {@code depositGas()},
     * {@code depositToken()}, and direct GAS transfers with data that would initiate a bridge request will be rejected,
     * i.e., aborted.
     * <p>
     * This feature is useful in the case of a planned contract update that involves a change in the computation of
     * the hash chain roots. By pausing the deposits, there will be no new deposits and the relayer can be given time
     * to catch-up with relaying everything that is currently in progress (i.e., the relayer can still use the
     * withdrawal functions) before the bridge is completely paused (i.e., with {@link #pauseBridge()}) and the
     * contract is updated.
     */
    public static void pauseDeposits() {
        onlyWhenDepositsNotPaused();
        onlyGovernor();
        baseMap.put(KEY_DEPOSIT_PAUSE, true);
        onDepositPause.fire();
    }

    public static void unpauseDeposits() {
        onlyWhenDepositsPaused();
        onlyGovernor();
        baseMap.put(KEY_DEPOSIT_PAUSE, false);
        onDepositUnpause.fire();
    }

    /**
     * @return true if deposits are paused, i.e., no deposits are being accepted. False otherwise.
     */
    @Safe
    public static boolean depositsArePaused() {
        return baseMap.getBoolean(KEY_DEPOSIT_PAUSE);
    }

    // endregion
    // region OnNEP17Payment

    /**
     * This function is called if a NEP-17 token is sent to this contract. The function does not contain any logic to
     * create a bridge operation, i.e., a deposit. The payments are merely accepted or rejected based on some
     * requirements. If a payment is rejected, abort is called.
     * <p>
     * There are 3 cases that are handled differently:
     * <ul>
     * <li> The GAS token is sent. </li>
     * <li> A registered token is sent. </li>
     * <li> An unregistered token is sent. </li>
     * </ul>
     * The function rejects any unregistered tokens. If the contract receives a registered token, the
     * function accepts the payment only if the data parameter is null. If the contract receives GAS, the function
     * only accepts the payment if either the from or data parameter is null.
     * <p>
     * If the contract receives GAS and the from parameter is null, the function accepts the payment as reward for
     * holding NEO.
     *
     * @param from   the sender.
     * @param amount the amount.
     * @param data   the data.
     */
    @OnNEP17Payment
    public static void onNep17Payment(Hash160 from, int amount, Object data) {
        Hash160 callingScriptHash = getCallingScriptHash();
        if (callingScriptHash.equals(gasToken.getHash())) {
            // Accept GAS rewards from holding NEO. This is the only case where the from parameter can be null.
            if (from == null) {
                BridgeImpl.addNeoHoldingGasRewards(amount);
                return;
            } else if (data == null) {
                return;
            } else {
                abort("No data accepted.");
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
    // region bridge management

    @Safe
    public static Hash160 management() {
        return baseMap.getHash160(KEY_BRIDGE_MANAGEMENT);
    }

    // endregion
    // region rewards

    /**
     * @return the amount of unclaimed rewards for the bridge operators. This amount is increased by the deposit fees
     * of the gas and token bridges, as well as by rewards from holding NEO.
     */
    @Safe
    public static int unclaimedRewards() {
        return BridgeImpl.getUnclaimedRewards();
    }

    // endregion
    // region gas bridge
    // region gas bridge pausing

    public static void pauseGasBridge() {
        onlyGovernorOrSecurityGuard();
        onlyWhenGasBridgeNotPaused();
        GasBridgeImpl.pauseGasBridge();
        onGasBridgePause.fire();
    }

    public static void unpauseGasBridge() {
        onlyGovernor();
        onlyWhenGasBridgePaused();
        GasBridgeImpl.unpauseGasBridge();
        onGasBridgeUnpause.fire();
    }

    // endregion
    // region gas deposit/claim/withdrawal

    /**
     * Deposit GAS to Neo X.
     *
     * @param from   the sender.
     * @param to     the recipient on Neo X.
     * @param amount the amount of GAS to deposit to Neo X. The provided amount includes the deposit fee. The value
     *               that will be distributed on Neo X is this amount minus the deposit fee.
     * @param maxFee the maximum fee that the depositor is willing to pay for the deposit. If the actual fee is higher
     *               than this value, the deposit is aborted.
     */
    public static void depositGas(Hash160 from, Hash160 to, int amount, int maxFee) {
        onlyWhenNotPaused();
        onlyWhenGasBridgeNotPaused();
        onlyWhenDepositsNotPaused();
        GasBridgeImpl.depositGas(from, to, amount, maxFee);
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
        onlyWhenNotPaused();
        onlyWhenGasBridgeNotPaused();
        GasBridgeImpl.withdrawGas(withdrawalRoot, signatures, withdrawals);
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
        onlyWhenNotPaused();
        onlyWhenGasBridgeNotPaused();
        GasBridgeImpl.claimGas(nonce);
    }

    // endregion
    // region gas bridge configuration/state
    // region gas bridge configuration

    @Safe
    public static GasBridge getGasBridge() {
        ByteString serialized = baseMap.get(KEY_GAS_BRIDGE);
        return (GasBridge) new StdLib().deserialize(serialized);
    }

    @Safe
    public static int gasDepositFee() {
        return getGasBridge().config.depositFee;
    }

    public static void setGasDepositFee(int newFee) {
        onlyGovernor();
        if (newFee < 0) abort("New deposit fee must be nonnegative.");
        GasBridge gasBridge = getGasBridge();
        if (newFee >= gasBridge.config.minAmount) abort("Deposit fee must be less than the minimum deposit amount.");
        gasBridge.config.depositFee = newFee;
        baseMap.put(KEY_GAS_BRIDGE, new StdLib().serialize(gasBridge));
        onGasDepositFeeChange.fire(newFee);
    }

    @Safe
    public static int minGasDeposit() {
        return getGasBridge().config.minAmount;
    }

    public static void setMinGasDeposit(int newMinAmount) {
        onlyGovernor();
        GasBridge gasBridge = getGasBridge();
        if (newMinAmount <= gasBridge.config.depositFee) abort("Minimum deposit must be greater than the deposit fee.");
        if (newMinAmount > gasBridge.config.maxAmount) abort("Minimum must be less than the maximum amount.");
        gasBridge.config.minAmount = newMinAmount;
        baseMap.put(KEY_GAS_BRIDGE, new StdLib().serialize(gasBridge));
        onMinGasDepositChange.fire(newMinAmount);
    }

    @Safe
    public static int maxGasDeposit() {
        return getGasBridge().config.maxAmount;
    }

    public static void setMaxGasDeposit(int newMaxAmount) {
        onlyGovernor();
        GasBridge gasBridge = getGasBridge();
        if (newMaxAmount < gasBridge.config.minAmount) abort("Maximum must be greater than the minimum amount.");
        if (newMaxAmount >= gasBridge.config.maxTotalDeposited)
            abort("Value must be less than the maximum total deposited amount.");
        gasBridge.config.maxAmount = newMaxAmount;
        baseMap.put(KEY_GAS_BRIDGE, new StdLib().serialize(gasBridge));
        onMaxGasDepositChange.fire(newMaxAmount);
    }

    @Safe
    public static int maxTotalDepositedGas() {
        return getGasBridge().config.maxTotalDeposited;
    }

    public static void setMaxTotalDepositedGas(int newMaxTotalDeposited) {
        onlyGovernor();
        GasBridgeImpl.setMaxTotalDepositedGas(newMaxTotalDeposited);
        onMaxTotalDepositedGasChange.fire(newMaxTotalDeposited);
    }

    // endregion
    // region gas bridge state

    @Safe
    public static int gasDepositNonce() {
        return getGasBridge().depositState.nonce;
    }

    @Safe
    public static ByteString gasDepositRoot() {
        return getGasBridge().depositState.root;
    }

    @Safe
    public static int gasWithdrawalNonce() {
        return getGasBridge().withdrawalState.nonce;
    }

    @Safe
    public static ByteString gasWithdrawalRoot() {
        return getGasBridge().withdrawalState.root;
    }

    // endregion
    // endregion
    // endregion
    // region token bridge
    // region token registration

    @Safe
    public static TokenBridge getTokenBridge(Hash160 token) {
        return TokenBridgeImpl.checkRegisteredAndGetTokenBridge(token);
    }

    @Safe
    public static Iterator<Hash160> getRegisteredTokensIterator() {
        return TokenBridgeImpl.getTokenBridgeTokensIterator();
    }

    @Safe
    public static List<Hash160> getRegisteredTokens() {
        return TokenBridgeImpl.getTokenBridgeTokens();
    }

    public static void registerToken(Hash160 token, TokenBridge.TokenConfig tokenConfig) {
        onlyGovernor();
        if (!TokenBridge.TokenConfig.isValid(tokenConfig)) abort("Invalid token configuration.");
        TokenBridgeImpl.registerToken(token, tokenConfig);
        onTokenRegister.fire(token, tokenConfig);
    }

    // endregion
    // region token pausing

    public static void pauseTokenBridge(Hash160 neoN3Token) {
        onlyGovernorOrSecurityGuard();
        onlyWhenTokenBridgeNotPaused(neoN3Token);
        Hash160 neoXToken = getTokenBridge(neoN3Token).config.neoXToken;
        TokenBridgeImpl.pauseTokenBridge(neoN3Token);
        onTokenBridgePause.fire(neoN3Token, neoXToken);
    }

    public static void unpauseTokenBridge(Hash160 neoN3Token) {
        onlyGovernor();
        onlyWhenTokenBridgePaused(neoN3Token);
        Hash160 neoXToken = getTokenBridge(neoN3Token).config.neoXToken;
        TokenBridgeImpl.unpauseTokenBridge(neoN3Token);
        onTokenBridgeUnpause.fire(neoN3Token, neoXToken);
    }

    // endregion
    // region token deposit/withdrawal/claim

    /**
     * Deposit a token to Neo X.
     *
     * @param token  the token to deposit.
     * @param from   the sender.
     * @param to     the recipient on Neo X.
     * @param amount the amount of the token to deposit to Neo X.
     * @param maxFee the maximum fee (GAS) that the depositor is willing to pay for the deposit. If the actual fee is
     *               higher than this value, the deposit is aborted.
     */
    public static void depositToken(Hash160 token, Hash160 from, Hash160 to, int amount, int maxFee) {
        enteringNonReentrant();
        onlyWhenNotPaused();
        onlyWhenDepositsNotPaused();
        onlyWhenTokenBridgeNotPaused(token);
        TokenBridgeImpl.depositToken(token, from, to, amount, maxFee);
        exitingNonReentrant();
    }

    /**
     * Withdraws tokens from the contract (i.e., from Neo X) to the provided recipients.
     * <p>
     * Requires the signatures of the validators. The signatures must sign the provided withdrawal root, while the
     * provided withdrawal root must be the computed root based on the current root in storage and the provided
     * withdrawals, all corresponding to the bridge of the specified token.
     * <p>
     * Withdrawals to contracts are made available for claiming and are not directly transferred due to uncertain
     * computation costs. Additionaly, if a transfer fails, the withdrawal is also made available for claiming. This
     * should never happen as long as the deposited token funds are held in this contract.
     *
     * @param token          the token to withdraw.
     * @param withdrawalRoot the new withdrawal root.
     * @param signatures     the signatures of the validators.
     * @param withdrawals    the withdrawals to execute.
     */
    public static void withdrawToken(Hash160 token, ByteString withdrawalRoot, Map<ECPoint, ByteString> signatures,
            List<Withdrawal> withdrawals) {
        onlyRelayer();
        onlyWhenNotPaused();
        onlyWhenTokenBridgeNotPaused(token);
        TokenBridgeImpl.withdrawToken(token, withdrawalRoot, signatures, withdrawals);
    }

    /**
     * Claim tokens that have been withdrawn from Neo X.
     * <p>
     * Only withdrawals that have a contract as recipient or for which the transfer has failed (should never happen
     * as long as the deposited token funds are held in this contract) are available to claim.
     * <p>
     * In order to claim a withdrawal, provide the nonce of the withdrawal and the amount will be transferred to the
     * already specified recipient.
     *
     * @param nonce the nonce of the withdrawal that is claimable.
     */
    public static void claimToken(Hash160 token, int nonce) {
        onlyWhenNotPaused();
        onlyWhenTokenBridgeNotPaused(token);
        TokenBridgeImpl.claimToken(token, nonce);
    }

    // endregion
    // region token bridge configuration/state
    // region token bridge configuration

    @Safe
    public static int tokenDepositFee(Hash160 token) {
        return getTokenBridge(token).config.fee;
    }

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

    @Safe
    public static int minTokenDeposit(Hash160 token) {
        return getTokenBridge(token).config.minAmount;
    }

    public static void setMinTokenDeposit(List<Hash160> tokens, List<Integer> newMinDeposits) {
        onlyGovernor();
        int nrTokens = tokens.size();
        if (nrTokens != newMinDeposits.size()) abort("Length mismatch.");
        StorageMap tokenBridgesMap = new StorageMap(BridgeContract.ctx, PREFIX_TOKEN_BRIDGES);
        for (int i = 0; i < nrTokens; i++) {
            Hash160 token = tokens.get(i);
            int newMinAmount = newMinDeposits.get(i);
            TokenBridge tokenBridge = TokenBridgeImpl.checkRegisteredAndGetTokenBridge(token);
            tokenBridge.config.minAmount = newMinAmount;
            tokenBridgesMap.put(token, new StdLib().serialize(tokenBridge));
            onMinTokenDepositChange.fire(token, newMinAmount);
        }
    }

    @Safe
    public static int maxTokenDeposit(Hash160 token) {
        return getTokenBridge(token).config.maxAmount;
    }

    public static void setMaxTokenDeposit(List<Hash160> tokens, List<Integer> newMaxDeposits) {
        onlyGovernor();
        int nrTokens = tokens.size();
        if (nrTokens != newMaxDeposits.size()) abort("Length mismatch.");
        StorageMap tokenBridgesMap = new StorageMap(BridgeContract.ctx, PREFIX_TOKEN_BRIDGES);
        for (int i = 0; i < nrTokens; i++) {
            Hash160 token = tokens.get(i);
            int newMaxAmount = newMaxDeposits.get(i);
            TokenBridge tokenBridge = TokenBridgeImpl.checkRegisteredAndGetTokenBridge(token);
            tokenBridge.config.maxAmount = newMaxAmount;
            tokenBridgesMap.put(token, new StdLib().serialize(tokenBridge));
            onMaxTokenDepositChange.fire(token, newMaxAmount);
        }
    }

    @Safe
    public static int maxTokenWithdrawals(Hash160 token) {
        return getTokenBridge(token).config.maxWithdrawals;
    }

    public static void setMaxTokenWithdrawals(List<Hash160> tokens, List<Integer> newMaxWithdrawals) {
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
    // region token bridge state

    @Safe
    public static int tokenDepositNonce(Hash160 token) {
        return getTokenBridge(token).depositState.nonce;
    }

    @Safe
    public static ByteString tokenDepositRoot(Hash160 token) {
        return getTokenBridge(token).depositState.root;
    }

    @Safe
    public static int tokenWithdrawalNonce(Hash160 token) {
        return getTokenBridge(token).withdrawalState.nonce;
    }

    @Safe
    public static ByteString tokenWithdrawalRoot(Hash160 token) {
        return getTokenBridge(token).withdrawalState.root;
    }

    // endregion
    // endregion
    // endregion

}
