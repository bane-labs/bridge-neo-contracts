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
import io.neow3j.devpack.annotations.Struct;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.contracts.StdLib;
import io.neow3j.devpack.events.Event2Args;

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

    @DisplayName("MessageExecution")
    @EventParameterNames({"Id", "ReturnValue"})
    public static Event2Args<Integer, Object> onMessageExecution;

    // endregion events
    // region structs

    @Struct
    public static class MessageContainer {
        public Message message;
        public int state;
    }

    public static class MessageExecutionState {
        public static final int PENDING = 1;
        public static final int EXECUTED = 1 << 1; // 2
    }

    @Struct
    public static class Message {
        public Invocation invocation;

        public Message(Invocation invocation) {
            this.invocation = invocation;
        }

        public static class Invocation {
            public Hash160 contract;
            public String method;
            public byte callFlags;
            public Object[] args;

            public Invocation(Hash160 contract, String method, byte callFlags, Object[] args) {
                this.contract = contract;
                this.method = method;
                this.callFlags = callFlags;
                this.args = args;
            }
        }
    }

    // endregion structs
    // region methods

    @Safe
    public ByteString getMessage(int id) {
        return messagesMap.get(id);
    }

    @Safe
    public static Message getMessageDeserialized(int id) {
        return (Message) stdLib.deserialize(messagesMap.get(id));
    }

    public static void storeMessage(ByteString serializedMessage) {
        if (serializedMessage == null || serializedMessage.length() == 0) {
            abort("Serialized message cannot be null or empty.");
        }
    }

    public static Object executeMessage(int id) {
        Message message = getMessageDeserialized(id);
        Message.Invocation invocation = message.invocation;
        if (invocation.args == null) {
            invocation.args = new Object[0];
        }
        Object returnValue = Contract.call(invocation.contract, invocation.method, invocation.callFlags,
                invocation.args);
        onMessageExecution.fire(id, returnValue);
        return returnValue;
    }

    // endregion methods
    // region deploy/update

    @OnDeployment
    public static void deploy(Object data, boolean update) {
        if (!update) {
            Storage.put(context, MESSAGES_CURRENT_ID_KEY, 0);
        }
    }

    public static void update(ByteString nefFile, String manifest, Object data) {
        new ContractManagement().update(nefFile, manifest, data);
    }

    // endregion

}
