package network.bane.bridge;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.ECPoint;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Helper;
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
import network.bane.structs.NativeTokenBridge;
import network.bane.structs.TokenBridge;
import network.bane.structs.Withdrawal;
import network.bane.structs.message.MessageBridge;
import network.bane.structs.message.N3MessageEnvelope;
import network.bane.structs.message.N3MethodCall;

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
import static network.bane.bridge.MessageBridgeImpl.onlyWhenMessageBridgeNotPaused;
import static network.bane.bridge.MessageBridgeImpl.onlyWhenMessageBridgePaused;
import static network.bane.bridge.NativeBridgeImpl.onlyWhenDepositsNotPaused;
import static network.bane.bridge.NativeBridgeImpl.onlyWhenDepositsPaused;
import static network.bane.bridge.NativeBridgeImpl.onlyWhenNativeBridgePaused;
import static network.bane.bridge.NativeBridgeImpl.onlyWhenNativeBridgeNotPaused;
import static network.bane.bridge.StorageConstants.KEY_DEPOSIT_PAUSE;
import static network.bane.bridge.StorageConstants.KEY_BRIDGE_PAUSE;
import static network.bane.bridge.StorageConstants.KEY_LINKED_CHAIN_ID;
import static network.bane.bridge.StorageConstants.KEY_ENTERED;
import static network.bane.bridge.StorageConstants.KEY_NATIVE_BRIDGE;
import static network.bane.bridge.StorageConstants.KEY_NEO_HOLDING_GAS_REWARDS;
import static network.bane.bridge.StorageConstants.KEY_VERSION;
import static network.bane.bridge.StorageConstants.KEY_UNCLAIMED_REWARDS;
import static network.bane.bridge.StorageConstants.PREFIX_TOKEN_BRIDGES;
import static network.bane.bridge.TokenBridgeImpl.onlyWhenTokenBridgePaused;
import static network.bane.bridge.TokenBridgeImpl.onlyWhenTokenBridgeNotPaused;
import static network.bane.bridge.StorageConstants.KEY_BRIDGE_MANAGEMENT;
import static network.bane.bridge.StorageConstants.PREFIX_BASE;

@DisplayName("${BridgeName}")
@Permission(contract = "*")
@ManifestExtras({@ManifestExtra(key = "Author", value = "BaneLabs"),
        @ManifestExtra(key = "Description",
                       value = "Contract for bridging fungible tokens between Neo N3 and a linked chain."),
        @ManifestExtra(key = "Source", value = "https://github.com/bane-labs/bridge-neo-contracts")})
public class BridgeContract {

    // This needs to be here due to the neow3j compiler. Technically, the Java compiler does not see this as a
    // constant value during compile time because it uses an instantiation with new. Static fields that are not
    // considered final must be in the main contract file.
    static final byte[] PREFIX_TOKEN_CLAIMABLES = new byte[]{StorageConstants.PREFIX_TOKEN_CLAIMABLES};
    // This value is enforced as upper limit for the maxAmount in the token config. It ensures that the transfer
    // amount that is distributed on the destination chain never overflows. The maximum scaling factor on Neo X is
    // 10^36 and 10^77 is the maximum value that can be used for distribution in an EVM chain without overflowing
    // (10^41*10^36=10^77).
    public static final int MAX_TRANSFER_AMOUNT_LIMIT = Helper.pow(10, 41);

    // region static contract values

    static final StorageContext ctx = Storage.getStorageContext();
    static final CryptoLib cryptoLib = new CryptoLib();

    // baseMap is used to store native-related state and general contract information, i.e., management contract and
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
    // region native bridge events

    @DisplayName("NativeBridgeSet")
    static Event onNativeBridgeSet;

    @DisplayName("NativeBridgePause")
    static Event onNativeBridgePause;

    @DisplayName("NativeBridgeUnpause")
    static Event onNativeBridgeUnpause;

    @DisplayName("NativeDeposit")
    @EventParameterNames({"Nonce", "Recipient", "Amount", "Depositor", "DepositHash", "NewDepositRoot"})
    static Event6Args<Integer, Hash160, Integer, Hash160, ByteString, ByteString> onNativeDeposit;

    @DisplayName("NativeClaimable")
    @EventParameterNames({"Nonce", "Recipient", "Amount"})
    static Event3Args<Integer, Hash160, Integer> onNativeClaimable;

    @DisplayName("NativeClaim")
    @EventParameterNames({"Nonce", "Recipient", "Amount"})
    static Event3Args<Integer, Hash160, Integer> onNativeClaim;

    @DisplayName("NativeWithdrawal")
    @EventParameterNames({"Nonce", "Recipient", "Amount"})
    static Event3Args<Integer, Hash160, Integer> onNativeWithdrawal;

    @DisplayName("NativeWithdrawalRootUpdate")
    @EventParameterNames({"Nonce", "WithdrawalRoot"})
    static Event2Args<Integer, ByteString> onNativeWithdrawalRootUpdate;

    @DisplayName("NativeDepositFeeChange")
    @EventParameterNames({"NewFee"})
    static Event1Arg<Integer> onNativeDepositFeeChange;

    @DisplayName("MinNativeDepositChange")
    @EventParameterNames({"NewMinDeposit"})
    static Event1Arg<Integer> onMinNativeDepositChange;

    @DisplayName("MaxNativeDepositChange")
    @EventParameterNames({"NewMaxDeposit"})
    static Event1Arg<Integer> onMaxNativeDepositChange;

    @DisplayName("MaxTotalDepositedNativeChange")
    @EventParameterNames({"NewMaxTotalDepositedNative"})
    static Event1Arg<Integer> onMaxTotalDepositedNativeChange;

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
    // region message bridge events

    @DisplayName("MessageBridgeSet")
    static Event onMessageBridgeSet;

    @DisplayName("MessageBridgePause")
    static Event onMessageBridgePause;

    @DisplayName("MessageBridgeUnpause")
    static Event onMessageBridgeUnpause;

    @DisplayName("N3MessageRootUpdate")
    @EventParameterNames({"Nonce", "N3MessageRoot"})
    static Event2Args<Integer, ByteString> onEvmToN3MessageRootUpdate;

    @DisplayName("N3MessageStore")
    @EventParameterNames({"Nonce", "Metadata"})
    static Event2Args<Integer, N3MessageEnvelope.N3ExecutableMessage.N3Metadata> onMessageStore;

    @DisplayName("N3MessageExecution")
    @EventParameterNames({"Nonce", "Metadata"})
    static Event2Args<Integer, N3MessageEnvelope.N3ExecutableMessage.N3Metadata> onMessageExecution;

    @DisplayName("N3MessageExecutionResult")
    @EventParameterNames({"Nonce", "Result"})
    static Event2Args<Integer, Object> onMessageExecutionResult;

    @DisplayName("MessageSendingFeeChange")
    @EventParameterNames({"NewFee"})
    static Event1Arg<Integer> onMessageSendingFeeChange;

    @DisplayName("MessageMaxBytesForSendingChange")
    @EventParameterNames({"NewMaxBytes"})
    static Event1Arg<Integer> onMessageMaxBytesForSendingChange;

    @DisplayName("MaxNrMessagesForStoringChange")
    @EventParameterNames({"NewMaxNrMessages"})
    static Event1Arg<Integer> onMaxNrMessagesForStoringChange;

    @DisplayName("MessageExecutionManagerChange")
    @EventParameterNames({"NewExecutionManager"})
    static Event1Arg<Hash160> onMessageExecutionManagerChange;

    @DisplayName("ExecutionWindowSecondsChange")
    @EventParameterNames({"NewExecutionWindowSeconds"})
    static Event1Arg<Integer> onExecutionWindowSecondsChange;

    // endregion
    // endregion
    // region deployment/update

    @OnDeployment
    public static void deploy(Object data, boolean isUpdate) {
        if (isUpdate) {
            // Make sure that this version of the contract is only used to update a deployed contract in version 3.
            if (baseMap.getInt(KEY_VERSION) != 3) abort("Invalid version");
            // Update internal versioning.
            baseMap.put(KEY_VERSION, 4);
            // Migrate storage here if required.
        } else {
            BridgeDeploymentData deploymentData = (BridgeDeploymentData) data;
            if (deploymentData.bridgeManagementContract == null ||
                    !Hash160.isValid(deploymentData.bridgeManagementContract))
                abort("Invalid bridge management");
            if (deploymentData.linkedChainId == null || deploymentData.linkedChainId <= 0)
                abort("Invalid linked chain id");

            baseMap.put(KEY_LINKED_CHAIN_ID, deploymentData.linkedChainId);
            baseMap.put(KEY_BRIDGE_MANAGEMENT, deploymentData.bridgeManagementContract);
            baseMap.put(KEY_BRIDGE_PAUSE, false);

            baseMap.put(KEY_UNCLAIMED_REWARDS, 0);
            baseMap.put(KEY_DEPOSIT_PAUSE, false);
            baseMap.put(KEY_NEO_HOLDING_GAS_REWARDS, 0);

            BridgeContract.baseMap.put(KEY_ENTERED, false);
            baseMap.put(KEY_VERSION, 4);

            // Make sure the owner witnesses the deployment.
            if (!checkWitness(managementContract().owner())) {
                abort("Owner must witness the deployment.");
            }
        }
    }

    public static void update(ByteString nef, String manifest, Object data) {
        onlyWhenPaused();
        if (!checkWitness(managementContract().owner())) abort("No authorization - only owner");
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
     * Pausing deposits will reject any incoming deposits to the bridge. This means {@code depositNative()} and
     * {@code depositToken()} will be rejected, i.e., aborted.
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
     * <li> The native token is sent, i.e., in case of Neo X as linked chain, the GasToken, or for other linked
     * chains, the corresponding fungible token set as the native token for that linked chain. </li>
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
        // Todo mialbu 14.01.25: Accept Gas if it is not used in the native bridge but is registered as a token bridge.
        if (callingScriptHash.equals(new GasToken().getHash())) {
            // Accept GAS rewards from holding NEO. This is the only case where the from parameter can be null.
            if (from == null) {
                BridgeImpl.addNeoHoldingGasRewards(amount);
                return;
            } else if (data == null) {
                return;
            } else {
                abort("No data accepted");
            }
            return;
        } else if (new StorageMap(BridgeContract.ctx, PREFIX_TOKEN_BRIDGES).get(callingScriptHash) != null) {
            if (data != null) {
                abort("No data accepted");
            }
            return;
        } else {
            abort("Unregistered token");
        }
    }

    // endregion
    // region linked chain

    @Safe
    public static int linkedChainId() {
        return baseMap.getInt(KEY_LINKED_CHAIN_ID);
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
     * of the native and token bridges, as well as by rewards from holding NEO.
     */
    @Safe
    public static int unclaimedRewards() {
        return BridgeImpl.getUnclaimedRewards();
    }

    @Safe
    public static int neoHoldingGasRewards() {
        return BridgeImpl.getNeoHoldingGasRewards();
    }

    // endregion
    // region native bridge
    // region native bridge setting

    public static void setNativeBridge(Hash160 tokenForNativeBridge, int decimalsOnLinkedChain, int depositFee,
            int minAmount, int maxAmount, int maxWithdrawals, int maxTotalDeposited) {

        onlyGovernor();

        // Set the native bridge. Aborts if the native bridge is already set.
        NativeBridgeImpl.setNativeBridge(tokenForNativeBridge, decimalsOnLinkedChain, depositFee, minAmount,
                maxAmount, maxWithdrawals, maxTotalDeposited);

        onNativeBridgeSet.fire();
    }

    // endregion
    // region native bridge pausing

    public static void pauseNativeBridge() {
        onlyGovernorOrSecurityGuard();
        onlyWhenNativeBridgeNotPaused();
        NativeBridgeImpl.pauseNativeBridge();
        onNativeBridgePause.fire();
    }

    public static void unpauseNativeBridge() {
        onlyGovernor();
        onlyWhenNativeBridgePaused();
        NativeBridgeImpl.unpauseNativeBridge();
        onNativeBridgeUnpause.fire();
    }

    // endregion
    // region native deposit/claim/withdrawal

    /**
     * Deposits the linked chain's native token representative to the linked chain.
     *
     * @param from   the sender.
     * @param to     the recipient on the linked chain.
     * @param amount the amount to deposit to the linked chain.
     * @param maxFee the maximum fee in GAS that the depositor is willing to pay for the deposit. If the actual fee
     *               is higher than this value, the deposit is aborted.
     */
    public static void depositNative(Hash160 from, Hash160 to, int amount, int maxFee) {
        depositNative(from, to, amount, maxFee, null);
    }

    /**
     * Deposits the linked chain's native token representative to the linked chain and allows to specify a sponsor to
     * pay the bridge fee.
     *
     * @param from       the sender.
     * @param to         the recipient on the linked chain.
     * @param amount     the amount to deposit to the linked chain.
     * @param maxFee     the maximum fee in GAS that the depositor is willing to pay for the deposit. If the actual
     *                   fee is higher than this value, the deposit is aborted.
     * @param feeSponsor the address that pays the fee for the deposit. If null, the from address pays the fee.
     */
    public static void depositNative(Hash160 from, Hash160 to, int amount, int maxFee, Hash160 feeSponsor) {
        enteringNonReentrant();

        onlyWhenNotPaused();
        onlyWhenNativeBridgeNotPaused();
        onlyWhenDepositsNotPaused();

        NativeBridgeImpl.depositNative(from, to, amount, maxFee, feeSponsor);

        exitingNonReentrant();
    }

    /**
     * Withdraws the linked chain's native token representative from the contract (i.e., GAS in case of Neo X) to
     * the provided recipients.
     * <p>
     * Requires the signatures of the validators. The signatures must sign the provided withdrawal root, while the
     * provided withdrawal root must be the computed root based on the current root in storage and the provided
     * withdrawals.
     * <p>
     * Withdrawals to contracts are made available for claiming and are not directly transferred due to uncertain
     * computation costs. Additionaly, if a transfer fails, the withdrawal is also made available for claiming. This
     * should never happen as long as the deposited native token funds are held in this contract.
     *
     * @param withdrawalRoot the new withdrawal root.
     * @param signatures     the signatures of the validators.
     * @param withdrawals    the withdrawals to execute.
     */
    public static void withdrawNative(ByteString withdrawalRoot, Map<ECPoint, ByteString> signatures,
            List<Withdrawal> withdrawals) {
        enteringNonReentrant();

        onlyRelayer();
        onlyWhenNotPaused();
        onlyWhenNativeBridgeNotPaused();

        NativeBridgeImpl.withdrawNative(withdrawalRoot, signatures, withdrawals);

        exitingNonReentrant();
    }

    /**
     * Claim the linked chain's native token representative that has been withdrawn from the linked chain.
     * <p>
     * Only withdrawals that have a contract as recipient or for which the transfer has failed (this should never happen
     * as long as the deposited funds are held in this contract) are available to claim.
     * <p>
     * In order to claim a withdrawal, provide the nonce of the withdrawal and the amount will be transferred to the
     * already specified recipient.
     *
     * @param nonce the nonce of the withdrawal that is claimable.
     */
    public static void claimNative(int nonce) {
        enteringNonReentrant();

        onlyWhenNotPaused();
        onlyWhenNativeBridgeNotPaused();

        NativeBridgeImpl.claimNative(nonce);

        exitingNonReentrant();
    }

    // endregion
    // region native bridge configuration/state
    // region native bridge configuration

    @Safe
    public static boolean nativeBridgeIsSet() {
        return NativeBridgeImpl.nativeBridgeIsSet();
    }

    @Safe
    public static Hash160 nativeToken() {
        return NativeBridgeImpl.getNativeBridge().config.nativeToken;
    }

    @Safe
    public static NativeTokenBridge getNativeBridge() {
        return NativeBridgeImpl.getNativeBridge();
    }

    @Safe
    public static int nativeDepositFee() {
        return NativeBridgeImpl.getNativeBridge().config.depositFee;
    }

    public static void setNativeDepositFee(int newFee) {
        onlyGovernor();
        if (newFee < 0) abort("New deposit fee must be nonnegative.");
        NativeTokenBridge nativeBridge = NativeBridgeImpl.getNativeBridge();
        if (newFee >= nativeBridge.config.minAmount) abort("Deposit fee must be less than the minimum deposit amount.");
        nativeBridge.config.depositFee = newFee;
        baseMap.put(KEY_NATIVE_BRIDGE, new StdLib().serialize(nativeBridge));
        onNativeDepositFeeChange.fire(newFee);
    }

    @Safe
    public static int minNativeDeposit() {
        return NativeBridgeImpl.getNativeBridge().config.minAmount;
    }

    public static void setMinNativeDeposit(int newMinAmount) {
        onlyGovernor();
        NativeTokenBridge nativeBridge = NativeBridgeImpl.getNativeBridge();
        if (newMinAmount <= nativeBridge.config.depositFee)
            abort("Minimum deposit must be greater than the deposit fee.");
        if (newMinAmount > nativeBridge.config.maxAmount) abort("Minimum must be less than the maximum amount.");
        nativeBridge.config.minAmount = newMinAmount;
        baseMap.put(KEY_NATIVE_BRIDGE, new StdLib().serialize(nativeBridge));
        onMinNativeDepositChange.fire(newMinAmount);
    }

    @Safe
    public static int maxNativeDeposit() {
        return NativeBridgeImpl.getNativeBridge().config.maxAmount;
    }

    public static void setMaxNativeDeposit(int newMaxAmount) {
        onlyGovernor();
        NativeTokenBridge nativeBridge = NativeBridgeImpl.getNativeBridge();
        if (newMaxAmount < nativeBridge.config.minAmount) abort("Maximum must be greater than the minimum amount.");
        if (newMaxAmount >= nativeBridge.config.maxTotalDeposited)
            abort("Value must be less than the maximum total deposited amount.");
        nativeBridge.config.maxAmount = newMaxAmount;
        baseMap.put(KEY_NATIVE_BRIDGE, new StdLib().serialize(nativeBridge));
        onMaxNativeDepositChange.fire(newMaxAmount);
    }

    @Safe
    public static int maxTotalDepositedNative() {
        return NativeBridgeImpl.getNativeBridge().config.maxTotalDeposited;
    }

    public static void setMaxTotalDepositedNative(int newMaxTotalDeposited) {
        onlyGovernor();
        NativeBridgeImpl.setMaxTotalDepositedNative(newMaxTotalDeposited);
        onMaxTotalDepositedNativeChange.fire(newMaxTotalDeposited);
    }

    // endregion
    // region native bridge state

    @Safe
    public static int nativeDepositNonce() {
        return NativeBridgeImpl.getNativeBridge().depositState.nonce;
    }

    @Safe
    public static ByteString nativeDepositRoot() {
        return NativeBridgeImpl.getNativeBridge().depositState.root;
    }

    @Safe
    public static int nativeWithdrawalNonce() {
        return NativeBridgeImpl.getNativeBridge().withdrawalState.nonce;
    }

    @Safe
    public static ByteString nativeWithdrawalRoot() {
        return NativeBridgeImpl.getNativeBridge().withdrawalState.root;
    }

    // endregion
    // endregion
    // endregion
    // region token bridge
    // region token registration

    @Safe
    public static boolean isRegisteredToken(Hash160 token) {
        return TokenBridgeImpl.isRegisteredToken(token);
    }

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
        if (!TokenBridge.TokenConfig.isValid(tokenConfig)) abort("Invalid token configuration");
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

    // Todo by mialbu on 08.01.25: Consider adding the target chain's id to the parameters.

    /**
     * Deposits a token to the linked chain.
     *
     * @param token  the token to deposit.
     * @param from   the sender.
     * @param to     the recipient on the linked chain.
     * @param amount the amount of the token to deposit to the linked chain.
     * @param maxFee the maximum fee (GAS) that the depositor is willing to pay for the deposit. If the actual fee is
     *               higher than this value, the deposit is aborted.
     */
    public static void depositToken(Hash160 token, Hash160 from, Hash160 to, int amount, int maxFee) {
        depositToken(token, from, to, amount, maxFee, null);
    }

    /**
     * Deposits a token to the linked chain and allows to specify a sponsor to pay the bridge fee.
     *
     * @param token      the token to deposit.
     * @param from       the sender.
     * @param to         the recipient on the linked chain.
     * @param amount     the amount of the token to deposit to the linked chain.
     * @param maxFee     the maximum fee (GAS) that the depositor is willing to pay for the deposit. If the actual
     *                   fee is higher than this value, the deposit is aborted.
     * @param feeSponsor the address that pays the fee for the deposit. If null, the from address pays the fee.
     */
    public static void depositToken(Hash160 token, Hash160 from, Hash160 to, int amount, int maxFee,
            Hash160 feeSponsor) {

        enteringNonReentrant();

        onlyWhenNotPaused();
        onlyWhenDepositsNotPaused();
        onlyWhenTokenBridgeNotPaused(token);

        TokenBridgeImpl.depositToken(token, from, to, amount, maxFee, feeSponsor);

        exitingNonReentrant();
    }

    /**
     * Withdraws tokens from the contract to the provided recipients.
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
        enteringNonReentrant();

        onlyRelayer();
        onlyWhenNotPaused();
        onlyWhenTokenBridgeNotPaused(token);

        TokenBridgeImpl.withdrawToken(token, withdrawalRoot, signatures, withdrawals);

        exitingNonReentrant();
    }

    /**
     * Claim tokens that have been withdrawn from the linked chain.
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
        enteringNonReentrant();

        onlyWhenNotPaused();
        onlyWhenTokenBridgeNotPaused(token);

        TokenBridgeImpl.claimToken(token, nonce);

        exitingNonReentrant();
    }

    // endregion
    // region token bridge configuration/state
    // region token bridge configuration

    @Safe
    public static int tokenDepositFee(Hash160 token) {
        return getTokenBridge(token).config.fee;
    }

    public static void setTokenDepositFee(Map<Hash160, Integer> newDepositFees) {
        onlyGovernor();
        Hash160[] tokens = newDepositFees.keys();
        StorageMap tokenBridgesMap = new StorageMap(BridgeContract.ctx, PREFIX_TOKEN_BRIDGES);
        for (Hash160 token : tokens) {
            Integer newFee = newDepositFees.get(token);
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

    public static void setMinTokenDeposit(Map<Hash160, Integer> newMinDeposits) {
        onlyGovernor();
        Hash160[] tokens = newMinDeposits.keys();
        StorageMap tokenBridgesMap = new StorageMap(BridgeContract.ctx, PREFIX_TOKEN_BRIDGES);
        for (Hash160 token : tokens) {
            Integer newMin = newMinDeposits.get(token);
            TokenBridge tokenBridge = TokenBridgeImpl.checkRegisteredAndGetTokenBridge(token);
            if (newMin > tokenBridge.config.maxAmount) {
                abort("Minimum deposit must not be greater than the maximum deposit.");
            }
            tokenBridge.config.minAmount = newMin;
            tokenBridgesMap.put(token, new StdLib().serialize(tokenBridge));
            onMinTokenDepositChange.fire(token, newMin);
        }
    }

    @Safe
    public static int maxTokenDeposit(Hash160 token) {
        return getTokenBridge(token).config.maxAmount;
    }

    public static void setMaxTokenDeposit(Map<Hash160, Integer> newMaxDeposits) {
        onlyGovernor();
        Hash160[] tokens = newMaxDeposits.keys();
        StorageMap tokenBridgesMap = new StorageMap(BridgeContract.ctx, PREFIX_TOKEN_BRIDGES);
        for (Hash160 token : tokens) {
            Integer newMax = newMaxDeposits.get(token);
            TokenBridge tokenBridge = TokenBridgeImpl.checkRegisteredAndGetTokenBridge(token);
            if (newMax < tokenBridge.config.minAmount) {
                abort("Maximum deposit must not be less than the minimum deposit.");
            }
            tokenBridge.config.maxAmount = newMax;
            tokenBridgesMap.put(token, new StdLib().serialize(tokenBridge));
            onMaxTokenDepositChange.fire(token, newMax);
        }
    }

    @Safe
    public static int maxTokenWithdrawals(Hash160 token) {
        return getTokenBridge(token).config.maxWithdrawals;
    }

    public static void setMaxTokenWithdrawals(Map<Hash160, Integer> newMaxWithdrawals) {
        onlyGovernor();
        Hash160[] tokens = newMaxWithdrawals.keys();
        StorageMap tokenBridgesMap = new StorageMap(BridgeContract.ctx, PREFIX_TOKEN_BRIDGES);
        for (Hash160 token : tokens) {
            Integer newMax = newMaxWithdrawals.get(token);
            TokenBridge tokenBridge = TokenBridgeImpl.checkRegisteredAndGetTokenBridge(token);
            tokenBridge.config.maxWithdrawals = newMax;
            tokenBridgesMap.put(token, new StdLib().serialize(tokenBridge));
            onMaxTokenWithdrawalsChange.fire(token, newMax);
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
    // region message bridge
    // region set message bridge

    public static void setMessageBridge(int sendingFee, int maxBytesForSending, int maxNrMsgsForStoring,
            Hash160 executionManager, int executionWindowSeconds) {

        onlyGovernor();
        // Set the message bridge. Aborts if the message bridge is already set.
        MessageBridgeImpl.setMessageBridge(sendingFee, maxBytesForSending, maxNrMsgsForStoring,
                executionManager, executionWindowSeconds);
        onMessageBridgeSet.fire();
    }

    // endregion
    // region message bridge pausing

    @Safe
    public static boolean messageBridgeIsPaused() {
        return MessageBridgeImpl.getMessageBridge().paused;
    }

    public static void pauseMessageBridge() {
        onlyGovernorOrSecurityGuard();
        onlyWhenMessageBridgeNotPaused();
        MessageBridgeImpl.pauseMessageBridge();
        onMessageBridgePause.fire();
    }

    public static void unpauseMessageBridge() {
        onlyGovernor();
        onlyWhenMessageBridgePaused();
        MessageBridgeImpl.unpauseMessageBridge();
        onMessageBridgeUnpause.fire();
    }

    // endregion
    // region message sending (N3 to EVM)

    public static void sendMessage() {
        // todo: send messages from N3 to EVM.
        abort("Not implemented yet");
    }

    // endregion
    // region message storing and executing (EVM to N3)

    @Safe
    public static ByteString getSerializedN3MethodCall(Hash160 target, String method, byte callFlags, Object[] args) {
        return new StdLib().serialize(new N3MethodCall(target, method, callFlags, args));
    }

    public static void storeMessages(ByteString n3MessageRoot, Map<ECPoint, ByteString> signatures,
            List<N3MessageEnvelope> messages) {
        onlyRelayer();
        onlyWhenNotPaused();
        onlyWhenMessageBridgeNotPaused();

        MessageBridgeImpl.storeMessages(n3MessageRoot, signatures, messages);
    }

    @Safe
    public static N3MessageEnvelope.N3ExecutableMessage getMessage(int nonce) {
        return MessageBridgeImpl.getMessage(nonce);
    }

    @Safe
    public static boolean messageHasBeenExecuted(int nonce) {
        return MessageBridgeImpl.messageHasBeenExecuted(nonce);
    }

    public static void executeMessage(int nonce) {
        onlyWhenNotPaused();
        onlyWhenMessageBridgeNotPaused();

        MessageBridgeImpl.executeMessage(nonce);
    }

    // endregion
    // region message execution results from EVM

    // todo: handle message execution results from EVM.

    // endregion
    // region message bridge configuration/state
    // region message bridge configuration

    @Safe
    public static boolean messageBridgeIsSet() {
        return MessageBridgeImpl.messageBridgeIsSet();
    }

    @Safe
    public static MessageBridge getMessageBridge() {
        return MessageBridgeImpl.getMessageBridge();
    }

    @Safe
    public static int messageSendingFee() {
        return MessageBridgeImpl.getMessageBridge().config.sendingFee;
    }

    public static void setMessageSendingFee(int newFee) {
        onlyGovernor();
        MessageBridgeImpl.setMessageSendingFee(newFee);
        onMessageSendingFeeChange.fire(newFee);
    }

    @Safe
    public static int maxBytesForSending() {
        return MessageBridgeImpl.getMessageBridge().config.maxBytesForSending;
    }

    public static void setMaxBytesForSending(int newMaxBytes) {
        onlyGovernor();
        MessageBridgeImpl.setMaxBytesForSending(newMaxBytes);
        onMessageMaxBytesForSendingChange.fire(newMaxBytes);
    }

    @Safe
    public static int maxNrMessagesForStoring() {
        return MessageBridgeImpl.getMessageBridge().config.maxNrMessagesForStoring;
    }

    public static void setMaxNrMessagesForStoring(int newMaxNrMessages) {
        onlyGovernor();
        MessageBridgeImpl.setMaxNrMessagesForStoring(newMaxNrMessages);
        onMaxNrMessagesForStoringChange.fire(newMaxNrMessages);
    }

    @Safe
    public static Hash160 messageExecutionManager() {
        return MessageBridgeImpl.getMessageBridge().config.executionManager;
    }

    public static void setMessageExecutionManager(Hash160 newExecutionManager) {
        onlyGovernor();
        MessageBridgeImpl.setExecutionManager(newExecutionManager);
        onMessageExecutionManagerChange.fire(newExecutionManager);
    }

    @Safe
    public static int executionWindowSeconds() {
        return MessageBridgeImpl.getMessageBridge().config.executionWindowSeconds;
    }

    public static void setExecutionWindowSeconds(int newExecutionWindowSeconds) {
        onlyGovernor();
        MessageBridgeImpl.setExecutionWindowSeconds(newExecutionWindowSeconds);
        onExecutionWindowSecondsChange.fire(newExecutionWindowSeconds);
    }

    // endregion
    // region message bridge state

    @Safe
    public static int evmMessageNonce() {
        return MessageBridgeImpl.getMessageBridge().n3ToEvmMessageState.nonce;
    }

    @Safe
    public static ByteString evmMessageRoot() {
        return MessageBridgeImpl.getMessageBridge().n3ToEvmMessageState.root;
    }

    @Safe
    public static int n3MessageNonce() {
        return MessageBridgeImpl.getMessageBridge().evmToN3MessageState.nonce;
    }

    @Safe
    public static ByteString n3MessageRoot() {
        return MessageBridgeImpl.getMessageBridge().evmToN3MessageState.root;
    }

    // endregion
    // endregion
    // endregion

}
