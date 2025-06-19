package network.bane.bridge;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.ECPoint;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Hash256;
import io.neow3j.devpack.List;
import io.neow3j.devpack.Map;
import io.neow3j.devpack.Runtime;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.contracts.StdLib;
import network.bane.lib.BridgeLib;
import network.bane.lib.MessageBridgeLib;
import network.bane.structs.message.ExecutableMessage;
import network.bane.structs.message.Message;
import network.bane.structs.message.MessageBridge;
import network.bane.structs.State;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Runtime.getCallingScriptHash;
import static network.bane.bridge.BridgeContract.linkedChainId;
import static network.bane.bridge.BridgeHelper.managementContract;
import static network.bane.bridge.BridgeHelper.messageExecutorContract;
import static network.bane.bridge.StorageConstants.KEY_MESSAGE_BRIDGE;
import static network.bane.bridge.StorageConstants.KEY_MESSAGE_EXECUTOR;
import static network.bane.bridge.StorageConstants.PREFIX_EXECUTABLE_MESSAGES;
import static network.bane.bridge.StorageConstants.PREFIX_MESSAGES_EXECUTED;

public class MessageBridgeImpl {

    // region pausing

    static void onlyWhenMessageBridgePaused() {
        abort("Not implemented yet");
    }

    static void onlyWhenMessageBridgeNotPaused() {
        abort("Not implemented yet");
    }

    static void pauseMessageBridge() {
        abort("Not implemented yet");
    }

    static void unpauseMessageBridge() {
        abort("Not implemented yet");
    }

    // endregion
    // region message bridge

    static void setMessageBridge(Hash160 executorContract, int fee) {
        if (!new ContractManagement().isContract(executorContract)) abort("Executor contract not found");

        BridgeContract.baseMap.put(KEY_MESSAGE_EXECUTOR, executorContract);
        ByteString zeroHash = Hash256.zero().toByteString();
        State state = new State(0, zeroHash);
        State state2 = state;
        BridgeContract.baseMap.put(KEY_MESSAGE_BRIDGE, new StdLib().serialize(new MessageBridge(true, state, state2,
                new MessageBridge.MessageConfig(fee))));
    }

    private static MessageBridge getMessageBridge() {
        return (MessageBridge) new StdLib().deserialize(BridgeContract.baseMap.get(KEY_MESSAGE_BRIDGE));
    }

    // endregion
    // region message N3 to EVM

    static int sendMessage(ByteString evmMessageBytes, Hash160 feePayer, int maxFee) {
        Hash160 callingScriptHash = getCallingScriptHash();
        int time = Runtime.getTime();
        Message.Metadata metadata = new Message.Metadata(time, callingScriptHash);

        MessageBridge messageBridge = getMessageBridge();
        if (messageBridge.config.messageFee > maxFee) abort("Max fee exceeded");

        // Fee payment
        BridgeImpl.payFee(feePayer, messageBridge.config.messageFee, null);

        // Update the evm message send state and return the nonce
        return updateN3ToEvmMessageState(messageBridge, metadata, evmMessageBytes);
    }

    static int updateN3ToEvmMessageState(MessageBridge messageBridge, Message.Metadata metadata,
            ByteString messageBytes) {
        messageBridge.n3ToEvmMessageState.nonce++;
        Message message = new Message(messageBridge.n3ToEvmMessageState.nonce, metadata, messageBytes);
        ByteString messageHash = MessageBridgeLib.hashMessageBridgeOp(BridgeContract.cryptoLib, message);
        ByteString newRoot = BridgeLib.computeNewRoot(BridgeContract.cryptoLib, messageBridge.n3ToEvmMessageState.root,
                messageHash);
        messageBridge.n3ToEvmMessageState.root = newRoot;

        assert messageBridge.n3ToEvmMessageState.root == newRoot : "Root not set correctly";
        assert messageBridge.n3ToEvmMessageState.nonce == message.nonce : "Nonce not set correctly";

        BridgeContract.onMessageSend.fire(message, messageBridge.n3ToEvmMessageState.root);
        return message.nonce;
    }

    // endregion
    // region message EVM to N3

    private static boolean subsequentNonces(List<Message> messages, int startNonce) {
        int nrMessages = messages.size();
        for (int i = 1; i <= nrMessages; i++) {
            if (messages.get(i - 1).nonce != startNonce + i) {
                return false;
            }
        }
        return true;
    }

    static void storeMessages(ByteString newRoot, Map<ECPoint, ByteString> signatures, List<Message> messages) {
        int nrMessages = messages.size();
        MessageBridge messageBridge = getMessageBridge();

        if (nrMessages <= 0) abort("At least one message required");
        if (!subsequentNonces(messages, messageBridge.evmToN3MessageState.nonce)) {
            abort("Provided messages not subsequent");
        }
        if (!MessageBridgeLib.computeNewTopRoot(BridgeContract.cryptoLib, messageBridge.evmToN3MessageState.root,
                messages).equals(newRoot)) {
            abort("Invalid root");
        }
        if (!managementContract().verifyValidatorSignatures(linkedChainId(), newRoot, signatures)) {
            abort("Invalid validator signatures");
        }

        // Update the evmToN3 message state
        messageBridge.evmToN3MessageState.nonce = messages.get(nrMessages - 1).nonce;
        messageBridge.evmToN3MessageState.root = newRoot;
        assert messageBridge.evmToN3MessageState.root == newRoot : "Root not set correctly";
        BridgeContract.baseMap.put(KEY_MESSAGE_BRIDGE, new StdLib().serialize(messageBridge));
        BridgeContract.onEvmToN3MessageRootUpdate.fire(messageBridge.evmToN3MessageState.nonce,
                messageBridge.evmToN3MessageState.root);

        // Store the message to storage
        storeMessages(messages);
    }

    private static void storeMessages(List<Message> messages) {
        StorageMap executableMessageMap = new StorageMap(BridgeContract.ctx, PREFIX_EXECUTABLE_MESSAGES);
        StorageMap executedMap = new StorageMap(BridgeContract.ctx, PREFIX_MESSAGES_EXECUTED);
        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);
            executableMessageMap.put(message.nonce, new StdLib().serialize(new ExecutableMessage(message)));
            executedMap.put(message.nonce, false);
            BridgeContract.onMessageStore.fire(message);
        }
    }

    static Object executeMessage(int messageId) {
        StorageMap executableMessagesMap = new StorageMap(BridgeContract.ctx, PREFIX_EXECUTABLE_MESSAGES);
        if (executableMessagesMap.getBoolean(messageId)) abort("Message already executed");
        executableMessagesMap.put(messageId, true);

        BridgeContract.onMessageExecute.fire(messageId);
        Message message = (Message) new StdLib().deserialize(
                new StorageMap(BridgeContract.ctx, PREFIX_EXECUTABLE_MESSAGES).get(messageId));
        Object result = messageExecutorContract().executeMessage(message);
        // Todo: handle result
        // updateResultState(messageId, result);
        // onResultUpdate.fire(messageId, result);
        abort("Not completely implemented yet");
        return result;
    }

    // endregion

}
