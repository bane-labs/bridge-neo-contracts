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
import io.neow3j.devpack.contracts.GasToken;
import io.neow3j.devpack.contracts.StdLib;
import network.bane.lib.BridgeLib;
import network.bane.lib.MessageBridgeLib;
import network.bane.structs.State;
import network.bane.structs.message.ExecutableState;
import network.bane.structs.message.MessageBridge;
import network.bane.structs.message.N3Message;
import network.bane.structs.message.N3MessageEnvelope;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Runtime.getExecutingScriptHash;
import static network.bane.lib.MessageBridgeLib.MESSAGE_TYPE_EXECUTABLE;
import static network.bane.message.MessageBridgeContractHelper.managementContract;
import static network.bane.message.StorageConstants.KEY_MESSAGE_BRIDGE;
import static network.bane.message.StorageConstants.KEY_UNCLAIMED_FEES;
import static network.bane.message.StorageConstants.PREFIX_MSG_EXECUTABLE_STATE;
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
    private static final int DEFAULT_EXECUTION_WINDOW_MILLIS = 60 * 60 * 24 * 7 * 2 * 1000; // 2 weeks

    static void setDefaultMessageBridge(Hash160 executionManager) {
        // Todo: Consider checking that the execution manager is a contract.
        ByteString zeroHash = Hash256.zero().toByteString();
        MessageBridge messageBridge = new MessageBridge(new State(0, zeroHash), new State(0, zeroHash),
                new MessageBridge.MessageBridgeConfig(DEFAULT_SENDING_FEE, DEFAULT_MAX_BYTES_FOR_SENDING,
                        DEFAULT_MAX_NR_MESSAGES_FOR_STORING, executionManager, DEFAULT_EXECUTION_WINDOW_MILLIS)
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

    static void setExecutionWindowMillis(int newExecutionWindowMillis) {
        MessageBridge messageBridge = getMessageBridge();
        if (newExecutionWindowMillis <= 0) abort("Execution window must be positive");
        messageBridge.config.executionWindowMilliseconds = newExecutionWindowMillis;
        storeMessageBridge(messageBridge);
    }

    // endregion
    // region storing

    static ByteString concatenateOperation(N3MessageEnvelope n3MessageEnvelope) {
        return MessageBridgeLib.concatMessageBridgeOpData(n3MessageEnvelope.nonce,
                n3MessageEnvelope.message.metadataBytes, n3MessageEnvelope.message.rawMessage);
    }

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

        int time = Runtime.getTime();
        int expirationTime = time + messageBridge.config.executionWindowMilliseconds;
        storeMessagesToContractStorage(messages, expirationTime);
    }

    private static void storeMessagesToContractStorage(List<N3MessageEnvelope> messages, int expirationTime) {
        StorageMap messageMap = new StorageMap(MessageBridgeContract.ctx, PREFIX_MSG_MESSAGES);
        StorageMap msgExecStateMap = new StorageMap(MessageBridgeContract.ctx, PREFIX_MSG_EXECUTABLE_STATE);
        int nrMessages = messages.size();
        ByteString execState = new StdLib().serialize(new ExecutableState(false, expirationTime));

        for (int i = 0; i < nrMessages; i++) {
            N3MessageEnvelope n3Message = messages.get(i);
            // Store the message in storage.
            messageMap.put(n3Message.nonce, new StdLib().serialize(n3Message.message));
            int type = ((N3Message.N3Metadata) new StdLib().deserialize(n3Message.message.metadataBytes)).type;
            if (type == MESSAGE_TYPE_EXECUTABLE) {
                // Set the execution state for executable messages.
                msgExecStateMap.put(n3Message.nonce, execState);
            }
            // Fire event including the nonce and the message's metadata.
            MessageBridgeContract.onStore.fire(n3Message.nonce, n3Message.message.metadataBytes);
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

    static N3Message.N3Metadata getMetadata(int nonce) {
        return getMetadata(getMessage(nonce));
    }

    static N3Message.N3Metadata getMetadata(N3Message message) {
        return (N3Message.N3Metadata) new StdLib().deserialize(message.metadataBytes);
    }

    static ExecutableState getExecutableState(int nonce) throws Exception {
        ByteString state = new StorageMap(MessageBridgeContract.ctx, PREFIX_MSG_EXECUTABLE_STATE).get(nonce);
        if (state == null) {
            throw new Exception("Executable state not found");
        }
        return (ExecutableState) new StdLib().deserialize(state);
    }

    static void executeMessage(int nonce) {
        // Validate that the message is executable.
        N3Message message = getMessage(nonce);
        N3Message.N3Metadata abstractMetadata = getMetadata(message);
        assert abstractMetadata.type == 0 : "Message metadata type is not executable";
        if (abstractMetadata.type != MESSAGE_TYPE_EXECUTABLE) {
            abort("Message is not executable");
        }
        N3Message.N3MetadataExecutable metadata = (N3Message.N3MetadataExecutable) abstractMetadata;

        // Validate that the message has not been executed yet and has not expired.
        checkExecutableStateAndMarkExecuted(nonce);

        MessageBridgeContract.onExecution.fire(nonce, metadata);
        Object result = new ExecutionManager(getMessageBridge().config.executionManager).executeMessage(nonce,
                message.rawMessage);
        MessageBridgeContract.onExecutionResult.fire(nonce, result);

        if (metadata.storeResult) {
            storeResult(nonce, result);
        }
    }

    private static void checkExecutableStateAndMarkExecuted(int nonce) {
        StorageMap executableStateMap = new StorageMap(MessageBridgeContract.ctx, PREFIX_MSG_EXECUTABLE_STATE);
        ByteString state = executableStateMap.get(nonce);
        if (state == null) {
            abort("Execution state not found");
        }
        ExecutableState executableState = (ExecutableState) new StdLib().deserialize(state);
        if (executableState.executed) {
            abort("Message has already been executed");
        }
        if (Runtime.getTime() > executableState.expirationTime) {
            abort("Message execution window expired");
        }
        executableState.executed = true;
        executableStateMap.put(nonce, new StdLib().serialize(executableState));
    }

    private static void storeResult(int nonce, Object result) {
        ByteString serializedResult = new StdLib().serialize(result);
        new StorageMap(MessageBridgeContract.ctx, PREFIX_MSG_RESULT).put(nonce, serializedResult);
    }

    public static int sendMessage(ByteString rawMsg, Hash160 feeSponsor, int maxFee) {
        MessageBridgeImpl.payMessageSendingFee(feeSponsor, maxFee);

        int timestamp = Runtime.getTime();
        Hash160 callingScriptHash = Runtime.getCallingScriptHash();

        N3Message.N3MetadataStoreOnly metadata = new N3Message.N3MetadataStoreOnly(timestamp, callingScriptHash);
        return serializeAndUpdateEvmMessageState(metadata, rawMsg);
    }

    public static int sendExecutableMessage(ByteString rawMsg, boolean storeResult, Hash160 feeSponsor, int maxFee) {
        MessageBridgeImpl.payMessageSendingFee(feeSponsor, maxFee);

        int timestamp = Runtime.getTime();
        Hash160 callingScriptHash = Runtime.getCallingScriptHash();
        N3Message.N3MetadataExecutable metadata = new N3Message.N3MetadataExecutable(timestamp, callingScriptHash,
                storeResult);
        return serializeAndUpdateEvmMessageState(metadata, rawMsg);
    }

    public static int sendResultMessage(int relatedMessageNonce, Hash160 feeSponsor, int maxFee) {
        ByteString result = getResult(relatedMessageNonce);
        if (result == null) {
            abort("Result not found");
        }

        MessageBridgeImpl.payMessageSendingFee(feeSponsor, maxFee);

        int timestamp = Runtime.getTime();
        Hash160 callingScriptHash = Runtime.getCallingScriptHash();

        N3Message.N3MetadataResult metadata = new N3Message.N3MetadataResult(timestamp, callingScriptHash,
                relatedMessageNonce);
        return serializeAndUpdateEvmMessageState(metadata, result);
    }

    private static int serializeAndUpdateEvmMessageState(Object metadata, ByteString rawMessage) {
        MessageBridge messageBridge = getMessageBridge();
        ByteString serializedMetadata = new StdLib().serialize(metadata);
        N3Message message = new N3Message(serializedMetadata, rawMessage);
        return updateEvmMessageState(messageBridge, message);
    }

    static ByteString getResult(int relatedMessageNonce) {
        return new StorageMap(MessageBridgeContract.ctx, PREFIX_MSG_RESULT).get(relatedMessageNonce);
    }

    private static void payMessageSendingFee(Hash160 feeSponsor, int maxFee) {
        MessageBridge messageBridge = getMessageBridge();
        // If the deposit fee is higher than the specified max fee, abort.
        int sendingFee = messageBridge.config.sendingFee;
        if (sendingFee > maxFee) abort("Max fee exceeded");

        // Fee payment
        MessageBridgeImpl.payFee(feeSponsor, sendingFee);
    }

    static void payFee(Hash160 feeSponsor, int fee) {
        MessageBridgeImpl.addToUnclaimedFees(fee);
        if (!Hash160.isValid(feeSponsor) || feeSponsor.isZero()) abort("Invalid 'feeSponsor'");
        if (getExecutingScriptHash().equals(feeSponsor)) abort("Prohibited 'feeSponsor'");

        // Pay the fee and transfer the token
        if (!new GasToken().transfer(feeSponsor, getExecutingScriptHash(), fee, null)) {
            abort("Fee transfer failed");
        }
    }

    static void addToUnclaimedFees(int amount) {
        int currentlyUnclaimedFees = getUnclaimedFees();
        MessageBridgeContract.baseMap.put(KEY_UNCLAIMED_FEES, currentlyUnclaimedFees + amount);
    }

    private static int updateEvmMessageState(MessageBridge messageBridge, N3Message message) {
        messageBridge.n3ToEvmMessageState.nonce++;
        N3MessageEnvelope msgEnvelope = new N3MessageEnvelope(messageBridge.n3ToEvmMessageState.nonce, message);
        ByteString messageHash = MessageBridgeLib.hashMessageBridgeOp(MessageBridgeContract.cryptoLib, msgEnvelope);
        ByteString newRoot = BridgeLib.computeNewRoot(MessageBridgeContract.cryptoLib,
                messageBridge.n3ToEvmMessageState.root, messageHash);
        messageBridge.n3ToEvmMessageState.root = newRoot;
        assert messageBridge.n3ToEvmMessageState.root == newRoot : "Root not set correctly";
        storeMessageBridge(messageBridge);
        MessageBridgeContract.onMessageSend.fire(msgEnvelope.nonce, msgEnvelope.message.rawMessage,
                msgEnvelope.message.metadataBytes, messageHash, newRoot);
        return msgEnvelope.nonce;
    }

    public static int getUnclaimedFees() {
        return MessageBridgeContract.baseMap.getInt(KEY_UNCLAIMED_FEES);
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
