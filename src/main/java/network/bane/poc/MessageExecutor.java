package network.bane.poc;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Contract;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Storage;
import io.neow3j.devpack.StorageContext;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.annotations.DisplayName;
import io.neow3j.devpack.annotations.EventParameterNames;
import io.neow3j.devpack.annotations.ManifestExtra;
import io.neow3j.devpack.annotations.ManifestExtra.ManifestExtras;
import io.neow3j.devpack.annotations.OnDeployment;
import io.neow3j.devpack.annotations.Permission;
import io.neow3j.devpack.annotations.Safe;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.contracts.StdLib;
import io.neow3j.devpack.events.Event1Arg;
import io.neow3j.devpack.events.Event2Args;
import network.bane.structs.message.Invocation;
import network.bane.structs.message.Message;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Storage.getStorageContext;

@Permission(contract = "*")
@ManifestExtras({@ManifestExtra(key = "Author", value = "BaneLabs"),
        @ManifestExtra(key = "Description", value = "Message Executor"),
        @ManifestExtra(key = "Source", value = "https://github.com/bane-labs/bridge-neo-contracts")})
public class MessageExecutor {

    // region constants

    private static final StdLib stdLib = new StdLib();

    private static final int messagesPrefix = 0x10;
    private static final StorageContext context = getStorageContext();
    private static final StorageMap messagesMap = new StorageMap(context, messagesPrefix);

    // Base Storage Keys
    private static final int MESSAGES_CURRENT_ID_KEY = 0x01;

    // endregion
    // region events

    @DisplayName("MessageStore")
    @EventParameterNames({"Id", "Message"})
    public static Event2Args<Integer, Message> onMessageStore;

    @DisplayName("MessageExecute")
    @EventParameterNames({"Id"})
    public static Event1Arg<Integer> onMessageExecution;

    @DisplayName("MessageExecuteReturn")
    @EventParameterNames({"Id", "ReturnValue"})
    public static Event2Args<Integer, Object> onMessageExecutionResult;

    // endregion events
    // region methods

    @Safe
    public static int getCurrentMessageId() {
        return Storage.getInt(context, MESSAGES_CURRENT_ID_KEY);
    }

    private static int incrementAndGetMessageId() {
        int newMessageId = getCurrentMessageId() + 1;
        Storage.put(context, MESSAGES_CURRENT_ID_KEY, newMessageId);
        return newMessageId;
    }

    @Safe
    public static ByteString getMessage(int id) {
        return messagesMap.get(id);
    }

    @Safe
    public static Message getMessageDeserialized(int id) {
        return (Message) stdLib.deserialize(messagesMap.get(id));
    }

    public static void storeMessage(int timestamp, Hash160 sender, ByteString serializedInvocation) {
        if (serializedInvocation == null || serializedInvocation.length() == 0) {
            abort("Serialized invocation cannot be null or empty.");
        }
        int messageId = incrementAndGetMessageId();
        Message message = new Message(messageId, new Message.Metadata(timestamp, sender), serializedInvocation);
        messagesMap.put(messageId, stdLib.serialize(message));

        // todo: compute message hash and update hash chain with hash(messageId, message).
        //  onMessageStoreRootUpdate.fire(messageId, message.hash, n3MessageStoreRoot

        onMessageStore.fire(messageId, message);
    }

    @Safe
    public static ByteString serializeMessage(int messageId, int timestamp, Hash160 msgSender, Hash160 contract,
            String method, byte callFlags, Object[] args) {
        return stdLib.serialize(new Message(messageId, new Message.Metadata(timestamp, msgSender),
                stdLib.serialize(new Invocation(contract, method, callFlags, args))));
    }

    @Safe
    public static ByteString serializeMessage(int messageId, int timestamp, Hash160 msgSender, ByteString invocation) {
        return stdLib.serialize(new Message(messageId, new Message.Metadata(timestamp, msgSender), invocation));
    }

    @Safe
    public static Message deserializeMessage(ByteString message) {
        return (Message) stdLib.deserialize(message);
    }

    @Safe
    public static Invocation deserializeInvocation(ByteString invocation) {
        return (Invocation) stdLib.deserialize(invocation);
    }

    @Safe
    public static ByteString serializeInvocation(Hash160 contract, String method, byte callFlags, Object[] args) {
        return stdLib.serialize(new Invocation(contract, method, callFlags, args));
    }

    @Safe
    public static Invocation getInvocation(ByteString serialized) {
        return (Invocation) stdLib.deserialize(serialized);
    }

    public static Object executeMessage(int id) {
        Message message = getMessageDeserialized(id);
        Invocation invocation = (Invocation) stdLib.deserialize(message.messageBytes);
        if (invocation.args == null) {
            invocation.args = new Object[0];
        }
        onMessageExecution.fire(id);
        // Todo: Reject disallowed calls, e.g., any call to the native ContractManagement contract.
        Object returnValue = Contract.call(invocation.contract, invocation.method, invocation.callFlags,
                invocation.args);
        onMessageExecutionResult.fire(id, returnValue);
        return returnValue;
    }

    // endregion methods
    // region deploy/update

    @OnDeployment
    public static void deploy(Object data, boolean update) {
        if (!update) {
            Storage.put(context, MESSAGES_CURRENT_ID_KEY, -1);
        }
    }

    public static void update(ByteString nefFile, String manifest, Object data) {
        new ContractManagement().update(nefFile, manifest, data);
    }

    // endregion

}
