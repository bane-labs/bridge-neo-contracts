package network.bane.message;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.ECPoint;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Hash256;
import io.neow3j.devpack.List;
import io.neow3j.devpack.Map;
import io.neow3j.devpack.Runtime;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.annotations.CallFlags;
import io.neow3j.devpack.contracts.ContractInterface;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.contracts.StdLib;
import network.bane.lib.MessageBridgeLib;
import network.bane.structs.State;
import network.bane.structs.message.MessageBridge;
import network.bane.structs.message.N3Message;
import network.bane.structs.message.N3MessageEnvelope;

import static io.neow3j.devpack.Helper.abort;
import static network.bane.lib.MessageBridgeLib.MESSAGE_TYPE_EXECUTABLE;
import static network.bane.message.MessageBridgeContractHelper.managementContract;
import static network.bane.message.StorageConstants.KEY_MESSAGE_BRIDGE;
import static network.bane.message.StorageConstants.KEY_UNCLAIMED_REWARDS;
import static network.bane.message.StorageConstants.PREFIX_MSG_EXECUTION_PENDING;
import static network.bane.message.StorageConstants.PREFIX_MSG_MESSAGES;
import static network.bane.message.StorageConstants.PREFIX_MSG_RESULT;

class MessageBridgeImpl {

    // region message bridge

    static MessageBridge getMessageBridge() {
        ByteString serialized = MessageBridgeContract.baseMap.get(KEY_MESSAGE_BRIDGE);
        if (serialized == null) abort("Message bridge not set");
        return (MessageBridge) new StdLib().deserialize(serialized);
    }

    private static final int DEFAULT_SENDING_FEE = 10000000; // 0.1 GAS
    private static final int DEFAULT_MAX_BYTES_FOR_SENDING = 10000;
    private static final int DEFAULT_MAX_NR_MESSAGES_FOR_STORING = 10;
    private static final int DEFAULT_EXECUTION_WINDOW_SECONDS = 60 * 60 * 24 * 7 * 2; // 2 weeks

    static void setDefaultMessageBridge(Hash160 executionManager) {
        // Todo: Consider checking that the execution manager is a contract.
        ByteString zeroHash = Hash256.zero().toByteString();
        MessageBridge messageBridge = new MessageBridge(new State(0, zeroHash), new State(0, zeroHash),
                new MessageBridge.MessageBridgeConfig(DEFAULT_SENDING_FEE, DEFAULT_MAX_BYTES_FOR_SENDING,
                        DEFAULT_MAX_NR_MESSAGES_FOR_STORING, executionManager, DEFAULT_EXECUTION_WINDOW_SECONDS)
        );
        if (!MessageBridge.isValid(messageBridge)) abort("Invalid message bridge configuration");
        storeMessageBridge(messageBridge);
    }

    private static void storeMessageBridge(MessageBridge messageBridge) {
        MessageBridgeContract.baseMap.put(KEY_MESSAGE_BRIDGE, new StdLib().serialize(messageBridge));
    }

    // endregion
    // region config

    static void setSendingFee(int newFee) {
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
        if (!MessageBridgeLib.computeNewTopRoot(MessageBridgeContract.cryptoLib, messageBridge.evmToN3MessageState.root,
                messages).equals(newN3MessageRoot)) {
            abort("Invalid root");
        }
        if (!managementContract().verifyValidatorSignatures(MessageBridgeContract.linkedChainId(),
                newN3MessageRoot, signatures)) {
            abort("Invalid validator signatures");
        }

        // Update evmToN3 messsage state
        messageBridge.evmToN3MessageState.nonce = messages.get(nrMessages - 1).nonce;
        messageBridge.evmToN3MessageState.root = newN3MessageRoot;
        assert messageBridge.evmToN3MessageState.root == newN3MessageRoot : "Root not set correctly";
        storeMessageBridge(messageBridge);
        MessageBridgeContract.onN3RootUpdate.fire(messageBridge.evmToN3MessageState.nonce,
                messageBridge.evmToN3MessageState.root);

        // Store the messages in the contract storage
        storeMessagesToContractStorage(messages);
    }

    private static void storeMessagesToContractStorage(List<N3MessageEnvelope> messages) {
        StorageMap messageMap = new StorageMap(MessageBridgeContract.ctx, PREFIX_MSG_MESSAGES);
        int nrMessages = messages.size();
        for (int i = 0; i < nrMessages; i++) {
            N3MessageEnvelope n3Message = messages.get(i);
            // Store the message in storage.
            messageMap.put(n3Message.nonce, new StdLib().serialize(n3Message.message));
            if (n3Message.message.messageType == MESSAGE_TYPE_EXECUTABLE) {
                // Mark the message as pending for execution.
                new StorageMap(MessageBridgeContract.ctx,PREFIX_MSG_EXECUTION_PENDING).put(n3Message.nonce, true);
            }
            // Fire event including the nonce and the message's metadata.
            MessageBridgeContract.onStore.fire(n3Message.nonce, n3Message.message.messageType,
                    n3Message.message.metadataBytes);
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

    static N3Message getMessage(int nonce) {
        return (N3Message) new StdLib().deserialize(
                new StorageMap(MessageBridgeContract.ctx, PREFIX_MSG_MESSAGES).get(nonce)
        );
    }

    static boolean isPending(int nonce) {
        return new StorageMap(MessageBridgeContract.ctx, PREFIX_MSG_EXECUTION_PENDING).get(nonce) != null;
    }

    static void executeMessage(int nonce) {
        // Validate that the message is executable.
        N3Message message = getMessage(nonce);
        if (message.messageType != MESSAGE_TYPE_EXECUTABLE) {
            abort("Message is not executable");
        }

        // Validate that the message is pending and has not been executed already.
        if (!isPending(nonce)) {
            abort("Message is not pending");
        }

        // Check that the message's execution window has not expired yet.
        // Todo: Consider overwriting the message metadata's timestamp with the expiration time or setting the
        //  expiration time when initially storing it.
        int currentTime = Runtime.getTime();
        N3Message.N3MetadataExecutable metadata =
                (N3Message.N3MetadataExecutable) new StdLib().deserialize(message.metadataBytes);

        int maxTimeForExecution = metadata.timestamp +
                (getMessageBridge().config.executionWindowSeconds * 1000); // Convert to milliseconds
        if (currentTime > maxTimeForExecution) {
            abort("Message execution window expired");
        }

        // Mark the message as executed.
        new StorageMap(MessageBridgeContract.ctx, PREFIX_MSG_EXECUTION_PENDING).delete(nonce);

        MessageBridgeContract.onExecution.fire(nonce, metadata);
        Object result = new ExecutionManager(getMessageBridge().config.executionManager).executeMessage(nonce,
                message.messageBytes);
        MessageBridgeContract.onExecutionResult.fire(nonce, result);

        if (metadata.storeResult) {
            storeResult(nonce, result);
        }
    }

    private static void storeResult(int nonce, Object result) {
        ByteString serializedResult = new StdLib().serialize(result);
        new StorageMap(MessageBridgeContract.ctx, PREFIX_MSG_RESULT).put(nonce, serializedResult);
    }

    public static int getUnclaimedRewards() {
        return MessageBridgeContract.baseMap.getInt(KEY_UNCLAIMED_REWARDS);
    }

    static class ExecutionManager extends ContractInterface {
        public ExecutionManager(Hash160 contractHash) {
            super(contractHash);
        }

        @CallFlags(io.neow3j.devpack.constants.CallFlags.All)
        public native Object executeMessage(int nonce, ByteString executableCode);
    }

    // endregion

}
