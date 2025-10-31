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
import io.neow3j.devpack.contracts.GasToken;
import io.neow3j.devpack.contracts.StdLib;
import io.neow3j.devpack.events.Event;
import io.neow3j.devpack.events.Event1Arg;
import io.neow3j.devpack.events.Event2Args;
import io.neow3j.devpack.events.Event4Args;
import io.neow3j.devpack.events.Event5Args;
import network.bane.structs.message.ExecutableState;
import network.bane.structs.message.MessageBridge;
import network.bane.structs.message.NeoMessage;
import network.bane.structs.message.NeoMessageEnvelope;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Runtime.checkWitness;
import static io.neow3j.devpack.Runtime.getCallingScriptHash;
import static network.bane.message.MessageBridgeContractHelper.managementContract;
import static network.bane.message.MessageBridgeContractHelper.onlyGovernor;
import static network.bane.message.MessageBridgeContractHelper.onlyGovernorOrSecurityGuard;
import static network.bane.message.MessageBridgeContractHelper.onlyRelayer;
import static network.bane.message.MessageBridgeContractHelper.onlyWhenExecutingNotPaused;
import static network.bane.message.MessageBridgeContractHelper.onlyWhenExecutingPaused;
import static network.bane.message.MessageBridgeContractHelper.onlyWhenNotPaused;
import static network.bane.message.MessageBridgeContractHelper.onlyWhenPaused;
import static network.bane.message.MessageBridgeContractHelper.onlyWhenSendingNotPaused;
import static network.bane.message.MessageBridgeContractHelper.onlyWhenSendingPaused;
import static network.bane.message.StorageConstants.KEY_BRIDGE_MANAGEMENT;
import static network.bane.message.StorageConstants.KEY_PAUSE;
import static network.bane.message.StorageConstants.KEY_ENTERED;
import static network.bane.message.StorageConstants.KEY_EXECUTING_PAUSE;
import static network.bane.message.StorageConstants.KEY_SENDING_PAUSE;
import static network.bane.message.StorageConstants.KEY_LINKED_CHAIN_ID;
import static network.bane.message.StorageConstants.KEY_UNCLAIMED_FEES;
import static network.bane.message.StorageConstants.KEY_VERSION;
import static network.bane.message.StorageConstants.PREFIX_BASE;

@DisplayName("MessageBridge")
@Permission(contract = "*")
@ManifestExtra.ManifestExtras({@ManifestExtra(key = "Author", value = "BaneLabs"),
        @ManifestExtra(key = "Description",
                       value = "Contract for bridging arbitrary messages between Neo N3 and a linked EVM chain."),
        @ManifestExtra(key = "Source", value = "https://github.com/bane-labs/bridge-neo-contracts")}
)
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

    @DisplayName("Pause")
    static Event onPause;

    @DisplayName("Unpause")
    static Event onUnpause;

    @DisplayName("SendingPause")
    static Event onSendingPause;

    @DisplayName("SendingUnpause")
    static Event onSendingUnpause;

    @DisplayName("ExecutingPause")
    static Event onExecutingPause;

    @DisplayName("ExecutingUnpause")
    static Event onExecutingUnpause;

    @DisplayName("MessageSend")
    @EventParameterNames({"Nonce", "Metadata", "Message", "MessageHash", "NeoToEvmRoot"})
    static Event5Args<Integer, ByteString, ByteString, ByteString, ByteString> onMessageSend;

    @DisplayName("EvmToNeoRootUpdate")
    @EventParameterNames({"Nonce", "EvmToNeoRoot"})
    static Event2Args<Integer, ByteString> onEvmToNeoRootUpdate;

    @DisplayName("Store")
    @EventParameterNames({"Nonce", "MetadataBytes"})
    static Event2Args<Integer, ByteString> onStore;

    @DisplayName("Execution")
    @EventParameterNames({"Nonce", "NumberOfChunks", "Index", "Result"})
    static Event4Args<Integer, Integer, Integer, Object> onExecution;

    @DisplayName("SendingFeeChange")
    @EventParameterNames({"NewFee"})
    static Event1Arg<Integer> onSendingFeeChange;

    @DisplayName("MaxMessageSizeChange")
    @EventParameterNames({"NewMaxBytes"})
    static Event1Arg<Integer> onMaxMessageSizeChange;

    @DisplayName("MaxNrMessagesChange")
    @EventParameterNames({"NewMaxNrMessages"})
    static Event1Arg<Integer> onMaxNrMessagesChange;

    @DisplayName("ExecutionManagerChange")
    @EventParameterNames({"NewExecutionManager"})
    static Event1Arg<Hash160> onExecutionManagerChange;

    @DisplayName("ExecutionWindowChange")
    @EventParameterNames({"NewExecutionWindowMilliseconds"})
    static Event1Arg<Integer> onExecutionWindowChange;

    // endregion
    // endregion
    // region deployment/update

    @OnDeployment
    public static void deploy(Object data, boolean isUpdate) {
        if (isUpdate) {
            // Make sure that this version of the contract is only used to update a deployed contract in version 1.
            // if (baseMap.getInt(KEY_VERSION) != 1) abort("Invalid version");
            // Update internal versioning.
            // baseMap.put(KEY_VERSION, 2);
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
            // Pause initially
            baseMap.put(KEY_PAUSE, true);

            baseMap.put(KEY_UNCLAIMED_FEES, 0);
            baseMap.put(KEY_SENDING_PAUSE, false);
            baseMap.put(KEY_EXECUTING_PAUSE, false);

            baseMap.put(KEY_ENTERED, false);
            baseMap.put(KEY_VERSION, 1);

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
        baseMap.put(KEY_PAUSE, true);
        onPause.fire();
    }

    public static void unpause() {
        onlyWhenPaused();
        onlyGovernor();
        baseMap.put(KEY_PAUSE, false);
        onUnpause.fire();
    }

    /**
     * @return true if the bridge is paused and no deposits nor withdrawals are accepted. False otherwise.
     */
    @Safe
    public static boolean isPaused() {
        return baseMap.getBoolean(KEY_PAUSE);
    }

    /**
     * Pauses sending messages. While sending is paused, no messages can be sent.
     */
    public static void pauseSending() {
        onlyWhenSendingNotPaused();
        onlyGovernorOrSecurityGuard();
        baseMap.put(KEY_SENDING_PAUSE, true);
        onSendingPause.fire();
    }

    /**
     * Unpauses sending messages. While sending is unpaused, messages can be sent.
     */
    public static void unpauseSending() {
        onlyWhenSendingPaused();
        onlyGovernor();
        baseMap.put(KEY_SENDING_PAUSE, false);
        onSendingUnpause.fire();
    }

    /**
     * Pauses executing messages. While executing is paused, no messages can be executed.
     */
    public static void pauseExecuting() {
        onlyWhenExecutingNotPaused();
        onlyGovernorOrSecurityGuard();
        baseMap.put(KEY_EXECUTING_PAUSE, true);
        onExecutingPause.fire();
    }

    /**
     * Unpauses executing messages. While executing is unpaused, messages can be executed.
     */
    public static void unpauseExecuting() {
        onlyWhenExecutingPaused();
        onlyGovernor();
        baseMap.put(KEY_EXECUTING_PAUSE, false);
        onExecutingUnpause.fire();
    }

    @Safe
    public static boolean sendingIsPaused() {
        return baseMap.getBoolean(KEY_SENDING_PAUSE);
    }

    @Safe
    public static boolean executingIsPaused() {
        return baseMap.getBoolean(KEY_EXECUTING_PAUSE);
    }

    // endregion
    // region OnNEP17Payment

    @OnNEP17Payment
    public static void onNep17Payment(Hash160 from, int amount, Object data) {
        // This contract accepts only GasToken payments. No data is allowed.
        Hash160 callingScriptHash = getCallingScriptHash();
        if (!callingScriptHash.equals(new GasToken().getHash())) {
            abort("Only GasToken payments are accepted");
        }
        if (data != null) {
            abort("No data accepted");
        }
        if (amount <= 0) {
            abort("Invalid amount");
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
     * @return the amount of unclaimed fees for the bridge operators. This amount is increased by the sending fees.
     */
    @Safe
    public static int unclaimedFees() {
        return MessageBridgeImpl.getUnclaimedFees();
    }

    // endregion
    // region message bridge functionality
    // region message sending (N3 to EVM)

    public static int sendStoreOnlyMessage(ByteString rawMessage, Hash160 feeSponsor, int maxFee) {
        onlyWhenNotPaused();
        onlyWhenSendingNotPaused();
        return MessageBridgeImpl.sendStoreOnlyMessage(rawMessage, feeSponsor, maxFee);
    }

    public static int sendExecutableMessage(ByteString rawMessage, boolean storeResult, Hash160 feeSponsor,
            int maxFee) {
        onlyWhenNotPaused();
        onlyWhenSendingNotPaused();
        return MessageBridgeImpl.sendExecutableMessage(rawMessage, storeResult, feeSponsor, maxFee);
    }

    public static int sendResultMessage(int relatedMessageNonce, Hash160 feeSponsor, int maxFee) {
        onlyWhenNotPaused();
        onlyWhenSendingNotPaused();

        return MessageBridgeImpl.sendResultMessage(relatedMessageNonce, feeSponsor, maxFee);
    }

    // endregion
    // region storing and executing messages (EVM to N3)

    @Safe
    public static ByteString serializeCall(Hash160 target, String method, byte callFlags, Object[] args) {
        return new MessageBridgeImpl.ExecutionManager(executionManager())
                .serializeCall(target, method, callFlags, args);
    }

    @Safe
    public static boolean isValidCall(ByteString serializedCall) {
        return new MessageBridgeImpl.ExecutionManager(executionManager()).isValidCall(serializedCall);
    }

    @Safe
    public static boolean isAllowedCall(ByteString serializedCall) {
        return new MessageBridgeImpl.ExecutionManager(executionManager()).isAllowedCall(serializedCall);
    }

    @Safe
    public static ByteString concatenateOperation(NeoMessageEnvelope message) {
        return MessageBridgeImpl.concatenateOperation(message);
    }

    public static void storeMessages(ByteString evmToNeoRoot, Map<ECPoint, ByteString> signatures,
            List<NeoMessageEnvelope> messages) {
        onlyRelayer();
        onlyWhenNotPaused();

        MessageBridgeImpl.storeMessages(evmToNeoRoot, signatures, messages);
    }

    @Safe
    public static NeoMessage getMessage(int nonce) {
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
        return new StdLib().serialize(new NeoMessage.NeoMetadataExecutable(timestamp, sender, storeResult));
    }

    @Safe
    public static Object serializeMetadataStoreOnly(int timestamp, Hash160 sender) {
        return new StdLib().serialize(new NeoMessage.NeoMetadataStoreOnly(timestamp, sender));
    }

    @Safe
    public static Object serializeMetadataResult(int timestamp, Hash160 sender, int initialMessageNonce) {
        return new StdLib().serialize(new NeoMessage.NeoMetadataResult(timestamp, sender, initialMessageNonce));
    }

    /**
     * Gets the executable state of a message by its nonce.
     *
     * @param nonce the nonce of the message.
     * @return the state of the executable message.
     * @throws Exception if the message does not exist or is not of type EXECUTABLE.
     */
    @Safe
    public static ExecutableState getExecutableState(int nonce) throws Exception {
        return MessageBridgeImpl.getExecutableState(nonce);
    }

    public static void executeMessage(int nonce) {
        onlyWhenNotPaused();
        onlyWhenExecutingNotPaused();

        MessageBridgeImpl.executeMessage(nonce);
    }

    /**
     * Gets the result of the execution of an executable message that was sent and executed on N3. If the result was
     * not returned AND stored as a result message to EVM, the result is null.
     *
     * @param relatedEvmToNeoMessageNonce the nonce of the N3 executable message.
     * @return the result of the execution of the N3 executable message.
     */
    @Safe
    public static Object getNeoExecutionResult(int relatedEvmToNeoMessageNonce) {
        return MessageBridgeImpl.getNeoExecutionResult(relatedEvmToNeoMessageNonce);
    }

    /**
     * Gets the serialized result of the execution of an executable message that was sent and executed on N3. If the
     * result was not returned AND stored as a result message to EVM, the result is null.
     *
     * @param relatedEvmToNeoMessageNonce the nonce of the N3 executable message.
     * @return the result of the execution of the N3 executable message in serialized form.
     */
    @Safe
    public static ByteString getSerializedNeoExecutionResult(int relatedEvmToNeoMessageNonce) {
        return MessageBridgeImpl.getSerializedNeoExecutionResult(relatedEvmToNeoMessageNonce);
    }

    /**
     * Gets the nonce of the EVM result message that corresponds to the execution of an executable message that was
     * sent and executed on EVM. If the result of this execution was sent back to N3 as a result message, the nonce
     * of this result message is returned.
     * <p>
     * If the result is non-existent, it hasn't been sent back, or it has been sent back but not stored, then 0 is
     * returned.
     *
     * @param relatedNeoToEvmMessageNonce the nonce of the executable message that was sent to EVM for execution.
     * @return the nonce of the result message that corresponds to the execution of the executable EVM message.
     */
    @Safe
    public static int getEvmExecutionResultNonce(int relatedNeoToEvmMessageNonce) {
        return MessageBridgeImpl.getEvmExecutionResultNonce(relatedNeoToEvmMessageNonce);
    }

    /**
     * Gets the result of the execution of an executable message that was sent and executed on EVM. If the result was
     * not returned AND stored as a result message to N3, the result is null.
     *
     * @param relatedNeoToEvmMessageNonce the nonce of the executable message that was sent to EVM for execution.
     * @return the result of the execution of the executable EVM message.
     */
    @Safe
    public static ByteString getEvmExecutionResult(int relatedNeoToEvmMessageNonce) {
        return MessageBridgeImpl.getEvmExecutionResult(relatedNeoToEvmMessageNonce);
    }

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
    public static int maxMessageSize() {
        return MessageBridgeImpl.getMessageBridge().config.maxMessageSize;
    }

    public static void setMaxMessageSize(int newMaxBytes) {
        onlyGovernor();
        MessageBridgeImpl.setMaxMessageSize(newMaxBytes);
        onMaxMessageSizeChange.fire(newMaxBytes);
    }

    @Safe
    public static int maxNrMessages() {
        return MessageBridgeImpl.getMessageBridge().config.maxNrMessages;
    }

    public static void setMaxNrMessages(int newMaxNrMessages) {
        onlyGovernor();
        MessageBridgeImpl.setMaxNrMessages(newMaxNrMessages);
        onMaxNrMessagesChange.fire(newMaxNrMessages);
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
    public static int executionWindowMilliseconds() {
        return MessageBridgeImpl.getMessageBridge().config.executionWindowMilliseconds;
    }

    public static void setExecutionWindowMilliseconds(int newExecutionWindowMilliseconds) {
        onlyGovernor();
        MessageBridgeImpl.setExecutionWindowMillis(newExecutionWindowMilliseconds);
        onExecutionWindowChange.fire(newExecutionWindowMilliseconds);
    }

    // endregion
    // region message bridge state

    @Safe
    public static int neoToEvmNonce() {
        return MessageBridgeImpl.getMessageBridge().neoToEvmState.nonce;
    }

    @Safe
    public static ByteString neoToEvmRoot() {
        return MessageBridgeImpl.getMessageBridge().neoToEvmState.root;
    }

    @Safe
    public static int evmToNeoNonce() {
        return MessageBridgeImpl.getMessageBridge().evmToNeoState.nonce;
    }

    @Safe
    public static ByteString evmToNeoRoot() {
        return MessageBridgeImpl.getMessageBridge().evmToNeoState.root;
    }

    // endregion
    // endregion
    // endregion

}
