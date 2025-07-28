package network.bane.message;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.ECPoint;
import io.neow3j.devpack.Hash160;
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
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.contracts.CryptoLib;
import io.neow3j.devpack.contracts.StdLib;
import io.neow3j.devpack.events.Event;
import io.neow3j.devpack.events.Event1Arg;
import io.neow3j.devpack.events.Event2Args;
import network.bane.structs.message.MessageBridge;
import network.bane.structs.message.N3Message;
import network.bane.structs.message.N3MessageEnvelope;
import network.bane.structs.message.N3MethodCall;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Runtime.checkWitness;
import static network.bane.message.MessageBridgeContractHelper.managementContract;
import static network.bane.message.MessageBridgeContractHelper.onlyGovernor;
import static network.bane.message.MessageBridgeContractHelper.onlyGovernorOrSecurityGuard;
import static network.bane.message.MessageBridgeContractHelper.onlyRelayer;
import static network.bane.message.MessageBridgeContractHelper.onlyWhenNotPaused;
import static network.bane.message.MessageBridgeContractHelper.onlyWhenPaused;
import static network.bane.message.MessageBridgeContractHelper.onlyWhenSendAndExecuteNotPaused;
import static network.bane.message.MessageBridgeContractHelper.onlyWhenSendAndExecutePaused;
import static network.bane.message.StorageConstants.KEY_BRIDGE_MANAGEMENT;
import static network.bane.message.StorageConstants.KEY_BRIDGE_PAUSE;
import static network.bane.message.StorageConstants.KEY_ENTERED;
import static network.bane.message.StorageConstants.KEY_SEND_AND_EXECUTE_PAUSE;
import static network.bane.message.StorageConstants.KEY_LINKED_CHAIN_ID;
import static network.bane.message.StorageConstants.KEY_UNCLAIMED_REWARDS;
import static network.bane.message.StorageConstants.KEY_VERSION;
import static network.bane.message.StorageConstants.PREFIX_BASE;

@DisplayName("MessageBridge")
@Permission(contract = "*")
@ManifestExtra.ManifestExtras({@ManifestExtra(key = "Author", value = "BaneLabs"),
        @ManifestExtra(key = "Description",
                       value = "Contract for bridging arbitrary messages between Neo N3 and a linked chain."),
        @ManifestExtra(key = "Source", value = "https://github.com/bane-labs/bridge-neo-contracts")})
public class MessageBridgeContract {

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

    @DisplayName("SendAndExecutePause")
    static Event onSendAndExecutePause;

    @DisplayName("SendAndExecuteUnpause")
    static Event onSendAndExecuteUnpause;

    @DisplayName("N3RootUpdate")
    @EventParameterNames({"Nonce", "N3MessageRoot"})
    static Event2Args<Integer, ByteString> onN3RootUpdate;

    @DisplayName("Store")
    @EventParameterNames({"Nonce", "MetadataBytes"})
    static Event2Args<Integer, ByteString> onStore;

    @DisplayName("Execute")
    @EventParameterNames({"Nonce", "Metadata"})
    static Event2Args<Integer, N3Message.N3MetadataExecutable> onExecution;

    @DisplayName("ExecutionResult")
    @EventParameterNames({"Nonce", "Result"})
    static Event2Args<Integer, Object> onExecutionResult;

    @DisplayName("SendingFeeChange")
    @EventParameterNames({"NewFee"})
    static Event1Arg<Integer> onSendingFeeChange;

    @DisplayName("MaxBytesForSendingChange")
    @EventParameterNames({"NewMaxBytes"})
    static Event1Arg<Integer> onMaxBytesForSendingChange;

    @DisplayName("MaxNrMessagesForStoringChange")
    @EventParameterNames({"NewMaxNrMessages"})
    static Event1Arg<Integer> onMaxNrMessagesForStoringChange;

    @DisplayName("ExecutionManagerChange")
    @EventParameterNames({"NewExecutionManager"})
    static Event1Arg<Hash160> onExecutionManagerChange;

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
            DeploymentData deploymentData = (DeploymentData) data;
            if (deploymentData.managementContract == null ||
                    !Hash160.isValid(deploymentData.managementContract))
                abort("Invalid bridge management");
            if (deploymentData.linkedChainId == null || deploymentData.linkedChainId <= 0)
                abort("Invalid linked chain id");

            baseMap.put(KEY_LINKED_CHAIN_ID, deploymentData.linkedChainId);
            baseMap.put(KEY_BRIDGE_MANAGEMENT, deploymentData.managementContract);
            // Todo: Add execution manager in baseMap instead of bridge configuration.
            // Pause initially
            baseMap.put(KEY_BRIDGE_PAUSE, true);

            baseMap.put(KEY_UNCLAIMED_REWARDS, 0);
            baseMap.put(KEY_SEND_AND_EXECUTE_PAUSE, false);

            baseMap.put(KEY_ENTERED, false);
            baseMap.put(KEY_VERSION, 4);

            // Set message bridge configuration upon deployment
            MessageBridgeImpl.setDefaultMessageBridge(deploymentData.executionManager);
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
     * Pausing the bridge will reject any messages and executions.
     * <p>
     * This feature is useful to halt any interaction with the contract besides governor or owner actions, such as
     * updating parameters, or contract updates.
     */
    public static void pause() {
        onlyWhenNotPaused();
        onlyGovernorOrSecurityGuard();
        baseMap.put(KEY_BRIDGE_PAUSE, true);
        onBridgePause.fire();
    }

    public static void unpause() {
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
     * Pausing send and execute will reject any incoming messages to be sent as well as stored messages to be executed.
     */
    public static void pauseSendAndExecute() {
        onlyWhenSendAndExecuteNotPaused();
        onlyGovernor();
        baseMap.put(KEY_SEND_AND_EXECUTE_PAUSE, true);
        onSendAndExecutePause.fire();
    }

    public static void unpauseSendAndExecute() {
        onlyWhenSendAndExecutePaused();
        onlyGovernor();
        baseMap.put(KEY_SEND_AND_EXECUTE_PAUSE, false);
        onSendAndExecuteUnpause.fire();
    }

    @Safe
    public static boolean sendAndExecuteIsPaused() {
        return baseMap.getBoolean(KEY_SEND_AND_EXECUTE_PAUSE);
    }

    // endregion
    // region OnNEP17Payment

    @OnNEP17Payment
    public static void onNep17Payment(Hash160 from, int amount, Object data) {
        // Todo: handle NEP17 payment
        abort("Not implemented yet");
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
        return MessageBridgeImpl.getUnclaimedRewards();
    }

    // endregion
    // region message bridge functionality
    // region message sending (N3 to EVM)

    public static void sendMessage() {
        // Todo: send message
        abort("Not implemented yet");
    }

    // region storing and executing messages (EVM to N3)

    @Safe
    public static ByteString getSerializedN3MethodCall(Hash160 target, String method, byte callFlags, Object[] args) {
        return new StdLib().serialize(new N3MethodCall(target, method, callFlags, args));
    }

    @Safe
    public static ByteString concatenateOperation(N3MessageEnvelope message) {
        return MessageBridgeImpl.concatenateOperation(message);
    }

    public static void storeMessages(ByteString n3MessageRoot, Map<ECPoint, ByteString> signatures,
            List<N3MessageEnvelope> messages) {
        onlyRelayer();
        onlyWhenNotPaused();

        MessageBridgeImpl.storeMessages(n3MessageRoot, signatures, messages);
    }

    @Safe
    public static N3Message getMessage(int nonce) {
        return MessageBridgeImpl.getMessage(nonce);
    }

    @Safe
    public static Object deserialize(ByteString serializedMessage) {
        return new StdLib().deserialize(serializedMessage);
    }

    @Safe
    public static Object getMetadata(int nonce) {
        return MessageBridgeImpl.getMetadata(nonce);
    }

    @Safe
    public static Object serializeMetadataExecutable(int timestamp, Hash160 sender, boolean storeResult) {
        return new StdLib().serialize(new N3Message.N3MetadataExecutable(timestamp, sender, storeResult));
    }

    @Safe
    public static Object serializeMetadataStoreOnly(int timestamp, Hash160 sender) {
        return new StdLib().serialize(new N3Message.N3MetadataStoreOnly(timestamp, sender));
    }

    @Safe
    public static Object serializeMetadataResult(int timestamp, Hash160 sender, int initialMessageNonce) {
        return new StdLib().serialize(new N3Message.N3MetadataResult(timestamp, sender, initialMessageNonce));
    }

    @Safe
    public static boolean isPending(int nonce) {
        return MessageBridgeImpl.isPending(nonce);
    }

    public static void executeMessage(int nonce) {
        onlyWhenNotPaused();

        MessageBridgeImpl.executeMessage(nonce);
    }

    // endregion
    // region message execution results from EVM

    // todo: handle message execution results from EVM.

    // endregion
    // region message bridge configuration/state
    // region message bridge configuration

    @Safe
    public static MessageBridge getMessageBridge() {
        return MessageBridgeImpl.getMessageBridge();
    }

    @Safe
    public static int sendingFee() {
        return MessageBridgeImpl.getMessageBridge().config.sendingFee;
    }

    public static void setSendingFee(int newFee) {
        onlyGovernor();
        MessageBridgeImpl.setSendingFee(newFee);
        onSendingFeeChange.fire(newFee);
    }

    @Safe
    public static int maxBytesForSending() {
        return MessageBridgeImpl.getMessageBridge().config.maxBytesForSending;
    }

    public static void setMaxBytesForSending(int newMaxBytes) {
        onlyGovernor();
        MessageBridgeImpl.setMaxBytesForSending(newMaxBytes);
        onMaxBytesForSendingChange.fire(newMaxBytes);
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
    public static Hash160 executionManager() {
        return MessageBridgeImpl.getMessageBridge().config.executionManager;
    }

    public static void setExecutionManager(Hash160 newExecutionManager) {
        onlyGovernor();
        MessageBridgeImpl.setExecutionManager(newExecutionManager);
        onExecutionManagerChange.fire(newExecutionManager);
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
