package network.bane.bridge;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Hash256;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.contracts.StdLib;
import network.bane.structs.State;
import network.bane.structs.message.MessageBridge;

import static io.neow3j.devpack.Helper.abort;
import static network.bane.bridge.StorageConstants.KEY_MESSAGE_BRIDGE;

public class MessageBridgeImpl {

    // region message bridge

    static boolean messageBridgeIsSet() {
        return BridgeContract.baseMap.get(KEY_MESSAGE_BRIDGE) != null;
    }

    static MessageBridge getMessageBridge() {
        ByteString serialized = BridgeContract.baseMap.get(KEY_MESSAGE_BRIDGE);
        if (serialized == null) abort("Message bridge not set");
        return (MessageBridge) new StdLib().deserialize(serialized);
    }

    static void setMessageBridge(int sendingFee, int maxBytesForSending, int maxNrMsgsForStoring,
            Hash160 executionManager, int executionWindowSeconds) {

        if (messageBridgeIsSet()) abort("Message bridge already set");
        if (!new ContractManagement().isContract(executionManager)) abort("ExecutionManager must be a contract");

        ByteString zeroHash = Hash256.zero().toByteString();
        MessageBridge messageBridge = new MessageBridge(true, new State(0, zeroHash), new State(0, zeroHash),
                new MessageBridge.MessageConfig(sendingFee, maxBytesForSending, maxNrMsgsForStoring,
                        executionManager, executionWindowSeconds)
        );
        if (!MessageBridge.isValid(messageBridge)) abort("Invalid message bridge configuration");
        storeMessageBridge(messageBridge);
    }

    private static void storeMessageBridge(MessageBridge messageBridge) {
        BridgeContract.baseMap.put(KEY_MESSAGE_BRIDGE, new StdLib().serialize(messageBridge));
    }

    // endregion
    // region pause

    static void onlyWhenMessageBridgePaused() {
        if (!getMessageBridge().paused) abort("Message bridge not paused");
    }

    static void onlyWhenMessageBridgeNotPaused() {
        if (getMessageBridge().paused) abort("Message bridge paused");
    }

    static void pauseMessageBridge() {
        MessageBridge messageBridge = getMessageBridge();
        messageBridge.paused = true;
        storeMessageBridge(messageBridge);
    }

    static void unpauseMessageBridge() {
        MessageBridge messageBridge = getMessageBridge();
        messageBridge.paused = false;
        storeMessageBridge(messageBridge);
    }

    // endregion
    // region config

    static void setMessageSendingFee(int newFee) {
        MessageBridge messageBridge = getMessageBridge();
        if (newFee < 0) abort("Sending fee must be nonnegative");
        messageBridge.config.sendingFee = newFee;
        storeMessageBridge(messageBridge);
    }

    static void setMessageExecutionManager(Hash160 newExecutionManager) {
        MessageBridge messageBridge = getMessageBridge();
        if (!new ContractManagement().isContract(newExecutionManager)) abort("ExecutionManager must be a contract");
        messageBridge.config.executionManager = newExecutionManager;
        storeMessageBridge(messageBridge);
    }

    public static void setMaxBytesForSending(int newMaxBytes) {
        MessageBridge messageBridge = getMessageBridge();
        if (newMaxBytes <= 0) abort("Max bytes for sending must be positive");
        messageBridge.config.maxBytesForSending = newMaxBytes;
        storeMessageBridge(messageBridge);
    }

    public static void setMaxNrMessagesForStoring(int newMaxNrMessages) {
        MessageBridge messageBridge = getMessageBridge();
        if (newMaxNrMessages <= 0) abort("Max number of messages for storing must be positive");
        messageBridge.config.maxNrMessagesForStoring = newMaxNrMessages;
        storeMessageBridge(messageBridge);
    }

    // endregion

}
