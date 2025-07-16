package network.bane.bridge;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.ECPoint;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Hash256;
import io.neow3j.devpack.List;
import io.neow3j.devpack.Map;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.contracts.StdLib;
import network.bane.lib.MessageBridgeLib;
import network.bane.structs.State;
import network.bane.structs.message.MessageBridge;
import network.bane.structs.message.N3MessageEnvelope;

import static io.neow3j.devpack.Helper.abort;
import static network.bane.bridge.StorageConstants.KEY_MESSAGE_BRIDGE;
import static network.bane.bridge.StorageConstants.PREFIX_MSG_EXECUTED;
import static network.bane.bridge.StorageConstants.PREFIX_MSG_MESSAGES;

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
        if (!new ContractManagement().isContract(executionManager)) abort("Execution manager must be a contract");

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

    static void setMaxBytesForSending(int newMaxBytes) {
        MessageBridge messageBridge = getMessageBridge();
        if (newMaxBytes <= 0) abort("Max bytes for sending must be positive");
        messageBridge.config.maxBytesForSending = newMaxBytes;
        storeMessageBridge(messageBridge);
    }

    static void setMaxNrMessagesForStoring(int newMaxNrMessages) {
        MessageBridge messageBridge = getMessageBridge();
        if (newMaxNrMessages <= 0) abort("Max number of messages for storing must be positive");
        messageBridge.config.maxNrMessagesForStoring = newMaxNrMessages;
        storeMessageBridge(messageBridge);
    }

    static void setExecutionManager(Hash160 newExecutionManager) {
        MessageBridge messageBridge = getMessageBridge();
        if (!new ContractManagement().isContract(newExecutionManager)) abort("Execution manager must be a contract");
        messageBridge.config.executionManager = newExecutionManager;
        storeMessageBridge(messageBridge);
    }

    static void setExecutionWindowSeconds(int newExecutionWindowSeconds) {
        MessageBridge messageBridge = getMessageBridge();
        if (newExecutionWindowSeconds <= 0) abort("Execution window seconds must be positive");
        messageBridge.config.executionWindowSeconds = newExecutionWindowSeconds;
        storeMessageBridge(messageBridge);
    }

    // endregion
    // region storing

    static void storeMessages(ByteString newN3MessageRoot, Map<ECPoint, ByteString> signatures,
            List<N3MessageEnvelope> messages) {
        int nrMessages = messages.size();
        MessageBridge messageBridge = getMessageBridge();

        if (nrMessages <= 0) abort("At least one message required");
        if (!subsequentNonces(messages, messageBridge.evmToN3MessageState.nonce)) {
            abort("Provided messages are not subsequent");
        }
        if (!MessageBridgeLib.computeNewTopRoot(BridgeContract.cryptoLib, messageBridge.evmToN3MessageState.root,
                messages).equals(newN3MessageRoot)) {
            abort("Invalid root");
        }
        if (!BridgeHelper.managementContract().verifyValidatorSignatures(BridgeContract.linkedChainId(),
                newN3MessageRoot, signatures)) {
            abort("Invalid validator signatures");
        }

        // Update evmToN3 messsage state
        messageBridge.evmToN3MessageState.nonce = messages.get(nrMessages - 1).nonce;
        messageBridge.evmToN3MessageState.root = newN3MessageRoot;
        assert messageBridge.evmToN3MessageState.root == newN3MessageRoot : "Root not set correctly";
        storeMessageBridge(messageBridge);
        BridgeContract.onEvmToN3MessageRootUpdate.fire(messageBridge.evmToN3MessageState.nonce,
                messageBridge.evmToN3MessageState.root);

        // Store the messages in the contract storage
        storeMessagesToContractStorage(messages);
    }

    private static void storeMessagesToContractStorage(List<N3MessageEnvelope> messages) {
        StorageMap messageMap = new StorageMap(BridgeContract.ctx, PREFIX_MSG_MESSAGES);
        StorageMap msgExecutedMap = new StorageMap(BridgeContract.ctx, PREFIX_MSG_EXECUTED);
        int nrMessages = messages.size();
        for (int i = 0; i < nrMessages; i++) {
            N3MessageEnvelope n3Message = messages.get(i);
            // Store the message in storage.
            messageMap.put(n3Message.nonce, new StdLib().serialize(n3Message.message));
            // Todo: consider not storing anything in the executedMap and just storing a value (e.g., timestamp of
            //  execution) once executed.
            msgExecutedMap.put(n3Message.nonce, false);
            // Fire event including the nonce and the message's metadata.
            BridgeContract.onMessageStore.fire(n3Message.nonce, n3Message.message.metadata);
        }
    }

    // Makes sure the messages have subsequent nonces.
    private static boolean subsequentNonces(List<N3MessageEnvelope> n3Messages, int startNonce) {
        int nrMessage = n3Messages.size();
        for (int i = 1; i <= nrMessage; i++) {
            if (n3Messages.get(i - 1).nonce != startNonce + i) {
                return false;
            }
        }
        return true;
    }

    static N3MessageEnvelope.N3ExecutableMessage getMessage(int nonce) {
        return (N3MessageEnvelope.N3ExecutableMessage) new StdLib().deserialize(
                new StorageMap(BridgeContract.ctx, PREFIX_MSG_MESSAGES).get(nonce)
        );
    }

    static boolean messageHasBeenExecuted(int nonce) {
        return new StorageMap(BridgeContract.ctx, PREFIX_MSG_EXECUTED).getBoolean(nonce);
    }

    // endregion

}
