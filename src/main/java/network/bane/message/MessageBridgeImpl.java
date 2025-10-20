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
import network.bane.structs.message.NeoMessage;
import network.bane.structs.message.NeoMessageEnvelope;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Runtime.getExecutingScriptHash;
import static io.neow3j.devpack.StringLiteralHelper.stringToInt;
import static network.bane.lib.MessageBridgeLib.MESSAGE_TYPE_EXECUTABLE;
import static network.bane.lib.MessageBridgeLib.MESSAGE_TYPE_RESULT;
import static network.bane.message.MessageBridgeContractHelper.managementContract;
import static network.bane.message.StorageConstants.KEY_MESSAGE_BRIDGE;
import static network.bane.message.StorageConstants.KEY_UNCLAIMED_FEES;
import static network.bane.message.StorageConstants.PREFIX_MSG_EXECUTABLE_STATE;
import static network.bane.message.StorageConstants.PREFIX_MSG_MESSAGES;
import static network.bane.message.StorageConstants.PREFIX_MSG_RESULT_EVM_EXEC;
import static network.bane.message.StorageConstants.PREFIX_MSG_RESULT_NEO_EXEC;

class MessageBridgeImpl {

    // These values are absolute upper bounds and serve as protective constraints to avoid setting too large values. If
    // needed, they can be adjusted with a contract update.
    private static final int UPPER_LIMIT_MAX_BYTES_FOR_SENDING = 10240; // 10 KB
    private static final int UPPER_LIMIT_MAX_NR_MESSAGES = 1000;
    // Value is too large for int, using a string instead and convert when needed.
    private static final String UPPER_LIMIT_MAX_EXECUTION_WINDOW_MILLIS = "31536000000"; // 1 year in milliseconds

    // region message bridge

    static MessageBridge getMessageBridge() {
        ByteString serialized = MessageBridgeContract.baseMap.get(KEY_MESSAGE_BRIDGE);
        if (serialized == null) abort("Message bridge not set");
        return (MessageBridge) new StdLib().deserialize(serialized);
    }

    private static final int DEFAULT_SENDING_FEE = 10000000; // 0.1 GAS
    // This value has been chosen to fit with the capped max size of notifications which is currently 1024 bytes.
    private static final int DEFAULT_MAX_BYTES_FOR_SENDING = 800; // 800 bytes
    private static final int DEFAULT_MAX_NR_MESSAGES_FOR_STORING = 10;
    private static final int DEFAULT_EXECUTION_WINDOW_MILLIS = 60 * 60 * 24 * 7 * 2 * 1000; // 2 weeks

    static void setDefaultMessageBridge(Hash160 executionManager) {
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
        if (newFee < 0) abort("Sending fee must be nonnegative");
        MessageBridge messageBridge = getMessageBridge();
        messageBridge.config.sendingFee = newFee;
        storeMessageBridge(messageBridge);
    }

    static void setMaxMessageSize(int newMaxBytes) {
        if (newMaxBytes <= 0) abort("Max message size must be positive");
        if (newMaxBytes > UPPER_LIMIT_MAX_BYTES_FOR_SENDING) abort("Max message size too large");
        MessageBridge messageBridge = getMessageBridge();
        messageBridge.config.maxMessageSize = newMaxBytes;
        storeMessageBridge(messageBridge);
    }

    static void setMaxNrMessages(int newMaxNrMessages) {
        if (newMaxNrMessages <= 0) abort("Max number of messages must be positive");
        if (newMaxNrMessages > UPPER_LIMIT_MAX_NR_MESSAGES)
            abort("Max number of messages too large");
        MessageBridge messageBridge = getMessageBridge();
        messageBridge.config.maxNrMessages = newMaxNrMessages;
        storeMessageBridge(messageBridge);
    }

    static void setExecutionManager(Hash160 newExecutionManager) {
        if (newExecutionManager == null || !Hash160.isValid(newExecutionManager) || newExecutionManager.isZero())
            abort("Invalid execution manager");
        if (!new ContractManagement().isContract(newExecutionManager)) abort("Execution manager must be a contract");
        MessageBridge messageBridge = getMessageBridge();
        messageBridge.config.executionManager = newExecutionManager;
        storeMessageBridge(messageBridge);
    }

    static void setExecutionWindowMillis(int newExecutionWindowMillis) {
        if (newExecutionWindowMillis <= 0) abort("Execution window must be positive");
        if (newExecutionWindowMillis > stringToInt(UPPER_LIMIT_MAX_EXECUTION_WINDOW_MILLIS))
            abort("Execution window too large");
        MessageBridge messageBridge = getMessageBridge();
        messageBridge.config.executionWindowMilliseconds = newExecutionWindowMillis;
        storeMessageBridge(messageBridge);
    }

    // endregion
    // region storing

    static ByteString concatenateOperation(NeoMessageEnvelope neoMessageEnvelope) {
        return MessageBridgeLib.concatMessageBridgeOpData(neoMessageEnvelope.nonce,
                neoMessageEnvelope.message.metadataBytes, neoMessageEnvelope.message.rawMessage);
    }

    static void storeMessages(ByteString newEvmToNeoRoot, Map<ECPoint, ByteString> signatures,
            List<NeoMessageEnvelope> messages) {
        int nrMessages = messages.size();
        MessageBridge messageBridge = getMessageBridge();

        if (nrMessages <= 0) abort("At least one message required");
        if (!subsequentNonces(messages, messageBridge.evmToNeoState.nonce)) {
            abort("Provided messages are not subsequent");
        }
        if (!MessageBridgeLib.computeNewTopRoot(MessageBridgeContract.cryptoLib, messageBridge.evmToNeoState.root,
                messages).equals(newEvmToNeoRoot)) {
            abort("Invalid root");
        }
        if (!managementContract().verifyValidatorSignatures(MessageBridgeContract.linkedChainId(),
                newEvmToNeoRoot, signatures)) {
            abort("Invalid validator signatures");
        }

        // Update evmToNeo messsage state
        messageBridge.evmToNeoState.nonce = messages.get(nrMessages - 1).nonce;
        messageBridge.evmToNeoState.root = newEvmToNeoRoot;
        assert messageBridge.evmToNeoState.root == newEvmToNeoRoot : "Root not set correctly";
        storeMessageBridge(messageBridge);
        MessageBridgeContract.onEvmToNeoRootUpdate.fire(messageBridge.evmToNeoState.nonce,
                messageBridge.evmToNeoState.root);

        // Store the messages in the contract storage

        int time = Runtime.getTime();
        int expirationTime = time + messageBridge.config.executionWindowMilliseconds;
        storeMessagesToContractStorage(messages, expirationTime);
    }

    private static void storeMessagesToContractStorage(List<NeoMessageEnvelope> messages, int expirationTime) {
        StorageMap messageMap = new StorageMap(MessageBridgeContract.ctx, PREFIX_MSG_MESSAGES);
        StorageMap msgExecStateMap = new StorageMap(MessageBridgeContract.ctx, PREFIX_MSG_EXECUTABLE_STATE);
        StorageMap resultEvmExecMap = new StorageMap(MessageBridgeContract.ctx, PREFIX_MSG_RESULT_EVM_EXEC);
        int nrMessages = messages.size();
        StdLib stdLib = new StdLib();
        ByteString execState = stdLib.serialize(new ExecutableState(false, expirationTime));

        for (int i = 0; i < nrMessages; i++) {
            NeoMessageEnvelope neoMessage = messages.get(i);
            // Store the message in storage.
            messageMap.put(neoMessage.nonce, stdLib.serialize(neoMessage.message));
            NeoMessage.NeoMetadata metadata =
                    (NeoMessage.NeoMetadata) stdLib.deserialize(neoMessage.message.metadataBytes);
            if (metadata.type == MESSAGE_TYPE_EXECUTABLE) {
                // Set the execution state for executable messages.
                msgExecStateMap.put(neoMessage.nonce, execState);
            }
            if (metadata.type == MESSAGE_TYPE_RESULT) {
                // Link the result to the related executable message for direct lookup.
                resultEvmExecMap.put(((NeoMessage.NeoMetadataResult) metadata).initialMessageNonce, neoMessage.nonce);
            }
            // Fire event including the nonce and the message's metadata.
            MessageBridgeContract.onStore.fire(neoMessage.nonce, neoMessage.message.metadataBytes);
        }
    }

    // Makes sure the messages have subsequent nonces.
    private static boolean subsequentNonces(List<NeoMessageEnvelope> neoMessages, int startNonce) {
        int nrMessage = neoMessages.size();
        for (int i = 1; i <= nrMessage; i++) {
            if (neoMessages.get(i - 1).nonce != startNonce + i) {
                return false;
            }
        }
        return true;
    }

    static NeoMessage getMessage(int nonce) {
        return (NeoMessage) new StdLib().deserialize(
                new StorageMap(MessageBridgeContract.ctx.asReadOnly(), PREFIX_MSG_MESSAGES).get(nonce)
        );
    }

    static NeoMessage.NeoMetadata getMetadata(int nonce) {
        return getMetadata(getMessage(nonce));
    }

    static NeoMessage.NeoMetadata getMetadata(NeoMessage message) {
        return (NeoMessage.NeoMetadata) new StdLib().deserialize(message.metadataBytes);
    }

    static ExecutableState getExecutableState(int nonce) throws Exception {
        ByteString state = new StorageMap(MessageBridgeContract.ctx.asReadOnly(), PREFIX_MSG_EXECUTABLE_STATE)
                .get(nonce);
        if (state == null) {
            throw new Exception("Executable state not found");
        }
        return (ExecutableState) new StdLib().deserialize(state);
    }

    static void executeMessage(int nonce) {
        // Validate that the message is executable.
        NeoMessage message = getMessage(nonce);
        NeoMessage.NeoMetadata abstractMetadata = getMetadata(message);
        assert abstractMetadata.type == 0 : "Message metadata type is not executable";
        if (abstractMetadata.type != MESSAGE_TYPE_EXECUTABLE) {
            abort("Message is not executable");
        }
        NeoMessage.NeoMetadataExecutable metadata = (NeoMessage.NeoMetadataExecutable) abstractMetadata;

        // Validate that the message has not been executed yet and has not expired.
        checkExecutableStateAndMarkExecuted(nonce);

        MessageBridge.MessageBridgeConfig config = getMessageBridge().config;
        Object result = new ExecutionManager(config.executionManager).executeMessage(nonce, message.rawMessage);

        ByteString serializedResult = new StdLib().serialize(result);

        if (metadata.storeResult) {
            new StorageMap(MessageBridgeContract.ctx, PREFIX_MSG_RESULT_NEO_EXEC).put(nonce, serializedResult);
        }

        // The max message size is used as the max byte size for results in a single event.
        // While this configuration value is used in this different context, it allows to easily identify which
        // results are fit to be sent back to EVM given the current configuration and support.
        int maxResultBytesPerEvent = config.maxMessageSize;

        // If the size of the serialized result is in the allowed range to send it back to EVM, it is emitted in a
        // single event. Otherwise, it is split and emitted in chunks, i.e., in multiple events.
        int resultBytesLen = serializedResult.length();
        if (resultBytesLen <= maxResultBytesPerEvent) {
            MessageBridgeContract.onExecution.fire(nonce, 1, 0, result);
        } else {
            fireChunkedExecutionEvents(nonce, serializedResult, maxResultBytesPerEvent);
        }
    }

    private static void fireChunkedExecutionEvents(int nonce, ByteString serializedResult, int maxBytesPerEvent) {
        int totalSize = serializedResult.length();
        // Potential decimal places are cut off in the division. If there is a remainder it is handled directly after
        // the for loop. nrFullChunks denotes how many chunks there are that are fully filled with
        // maxResultBytesPerEvent bytes.
        int nrFullChunks = totalSize / maxBytesPerEvent;
        int remainder = totalSize % maxBytesPerEvent;
        boolean hasRemainder = remainder > 0;
        // If there is a remainder, we need one additional chunk to hold those remaining bytes.
        int nrTotalChunks = nrFullChunks;
        if (hasRemainder) {
            nrTotalChunks++;
        }
        for (int i = 0; i < nrFullChunks; i++) {
            // Take the i-th chunk of the result. Each chunk should have maxResultBytesPerEvent bytes, except
            // possibly the last one.
            int startIndex = i * maxBytesPerEvent;
            ByteString serializedResultChunk = serializedResult.range(startIndex, maxBytesPerEvent);
            MessageBridgeContract.onExecution.fire(nonce, nrTotalChunks, i, serializedResultChunk);
        }
        if (hasRemainder) {
            // Take the last chunk of the result.
            MessageBridgeContract.onExecution.fire(nonce, nrTotalChunks, nrFullChunks,
                    serializedResult.last(remainder));
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

    public static int sendMessage(ByteString rawMsg, Hash160 feeSponsor, int maxFee) {
        MessageBridgeImpl.checkMsgSize(rawMsg.length());
        MessageBridgeImpl.payMessageSendingFee(feeSponsor, maxFee);

        int timestamp = Runtime.getTime();
        Hash160 callingScriptHash = Runtime.getCallingScriptHash();

        NeoMessage.NeoMetadataStoreOnly metadata = new NeoMessage.NeoMetadataStoreOnly(timestamp, callingScriptHash);
        return serializeAndUpdateNeoToEvmMessageState(metadata, rawMsg);
    }

    public static int sendExecutableMessage(ByteString rawMsg, boolean storeResult, Hash160 feeSponsor, int maxFee) {
        MessageBridgeImpl.checkMsgSize(rawMsg.length());
        MessageBridgeImpl.payMessageSendingFee(feeSponsor, maxFee);

        int timestamp = Runtime.getTime();
        Hash160 callingScriptHash = Runtime.getCallingScriptHash();
        NeoMessage.NeoMetadataExecutable metadata = new NeoMessage.NeoMetadataExecutable(timestamp, callingScriptHash,
                storeResult);
        return serializeAndUpdateNeoToEvmMessageState(metadata, rawMsg);
    }

    public static int sendResultMessage(int relatedMessageNonce, Hash160 feeSponsor, int maxFee) {
        ByteString result = getNeoExecutionResult(relatedMessageNonce);
        if (result == null) {
            abort("Result not found");
        }

        MessageBridgeImpl.checkMsgSize(result.length());
        MessageBridgeImpl.payMessageSendingFee(feeSponsor, maxFee);

        int timestamp = Runtime.getTime();
        Hash160 callingScriptHash = Runtime.getCallingScriptHash();

        NeoMessage.NeoMetadataResult metadata = new NeoMessage.NeoMetadataResult(timestamp, callingScriptHash,
                relatedMessageNonce);
        return serializeAndUpdateNeoToEvmMessageState(metadata, result);
    }

    private static void checkMsgSize(int msgSize) {
        if (msgSize > getMessageBridge().config.maxMessageSize) {
            abort("Message too large");
        }
    }

    private static int serializeAndUpdateNeoToEvmMessageState(Object metadata, ByteString rawMessage) {
        MessageBridge messageBridge = getMessageBridge();
        ByteString serializedMetadata = new StdLib().serialize(metadata);
        NeoMessage message = new NeoMessage(serializedMetadata, rawMessage);
        return updateNeoToEvmMessageState(messageBridge, message);
    }

    static ByteString getNeoExecutionResult(int relatedMessageNonce) {
        return new StorageMap(MessageBridgeContract.ctx.asReadOnly(), PREFIX_MSG_RESULT_NEO_EXEC)
                .get(relatedMessageNonce);
    }

    static int getEvmExecutionResultNonce(int relatedMessageNonce) {
        return new StorageMap(MessageBridgeContract.ctx.asReadOnly(), PREFIX_MSG_RESULT_EVM_EXEC)
                .getIntOrZero(relatedMessageNonce);
    }

    static ByteString getEvmExecutionResult(int relatedMessageNonce) {
        int resultMessageNonce = getEvmExecutionResultNonce(relatedMessageNonce);
        if (resultMessageNonce == 0) {
            return null;
        }
        return getMessage(resultMessageNonce).rawMessage;
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

    private static int updateNeoToEvmMessageState(MessageBridge messageBridge, NeoMessage message) {
        messageBridge.neoToEvmState.nonce++;
        NeoMessageEnvelope msgEnvelope = new NeoMessageEnvelope(messageBridge.neoToEvmState.nonce, message);
        ByteString messageHash = MessageBridgeLib.hashMessageBridgeOp(MessageBridgeContract.cryptoLib, msgEnvelope);
        ByteString newRoot = BridgeLib.computeNewRoot(MessageBridgeContract.cryptoLib,
                messageBridge.neoToEvmState.root, messageHash);
        messageBridge.neoToEvmState.root = newRoot;
        assert messageBridge.neoToEvmState.root == newRoot : "Root not set correctly";
        storeMessageBridge(messageBridge);
        MessageBridgeContract.onMessageSend.fire(msgEnvelope.nonce, msgEnvelope.message.metadataBytes,
                msgEnvelope.message.rawMessage, messageHash, newRoot);
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

        @CallFlags(io.neow3j.devpack.constants.CallFlags.ReadOnly)
        public native ByteString serializeCall(Hash160 target, String method, byte callFlags, Object[] args);

        @CallFlags(io.neow3j.devpack.constants.CallFlags.ReadOnly)
        public native boolean isValidCall(ByteString serializedCall);

        @CallFlags(io.neow3j.devpack.constants.CallFlags.ReadOnly)
        public native boolean isAllowedCall(ByteString serializedCall);
    }

    // endregion

}
