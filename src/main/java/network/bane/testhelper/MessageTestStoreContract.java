package network.bane.testhelper;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Runtime;
import io.neow3j.devpack.Storage;
import io.neow3j.devpack.StorageContext;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.annotations.CallFlags;
import io.neow3j.devpack.annotations.DisplayName;
import io.neow3j.devpack.annotations.ManifestExtra;
import io.neow3j.devpack.annotations.Safe;
import io.neow3j.devpack.contracts.ContractInterface;
import io.neow3j.devpack.contracts.StdLib;
import network.bane.structs.message.N3MessageEnvelope;

import static io.neow3j.devpack.Helper.abort;

@DisplayName("MessageTestContract")
@ManifestExtra(key = "author", value = "BaneLabs")
@ManifestExtra(key = "description", value = "Contract for testing purposes.")
public class MessageTestStoreContract {

    private static final byte PREFIX_BASE = 0x0a;
    private static final byte PREFIX_STORING = 0x0b;
    private static final byte PREFIX_METADATA = 0x0c;

    private static final int KEY_MESSAGE_BRIDGE = 0x02;

    private static final StorageContext ctx = Storage.getStorageContext();
    private static final StorageMap baseMap = new StorageMap(ctx, PREFIX_BASE);
    private static final StorageMap storeMap = new StorageMap(ctx, PREFIX_STORING);
    private static final StorageMap metadataMap = new StorageMap(ctx, PREFIX_METADATA);

    public static void setMessageBridge(Hash160 messageBridgeHash) {
        baseMap.put(KEY_MESSAGE_BRIDGE, messageBridgeHash);
    }

    private static MessageBridge messageBridge() {
        return new MessageBridge(baseMap.getHash160(KEY_MESSAGE_BRIDGE));
    }

    public static void storeValue(String key, Object value) {
        ByteString serialize = new StdLib().serialize(value);
        storeMap.put(key, serialize);
    }

    @Safe
    public static Object getStoredValue(String key) {
        ByteString storedByteString = storeMap.get(key);
        if (storedByteString == null) {
            return null;
        }
        return new StdLib().deserialize(storedByteString);
    }

    public static void storeMetadataOfExecutingMessage() {
        int currentlyExecutedNonce = new ExecutionManager(Runtime.getCallingScriptHash()).getExecutingNonce();
        if (currentlyExecutedNonce == 0) {
            abort("No message is currently being executed");
        }
        N3MessageEnvelope.N3ExecutableMessage.N3Metadata metadata = messageBridge().getMessage(
                currentlyExecutedNonce).metadata;
        metadataMap.put(currentlyExecutedNonce, new StdLib().serialize(metadata));
    }

    @Safe
    public static N3MessageEnvelope.N3ExecutableMessage.N3Metadata getStoredMetadata(int nonce) {
        ByteString byteString = metadataMap.get(nonce);
        if (byteString == null) {
            return null;
        }
        return (N3MessageEnvelope.N3ExecutableMessage.N3Metadata) new StdLib().deserialize(byteString);
    }

    static class ExecutionManager extends ContractInterface {
        public ExecutionManager(Hash160 contractHash) {
            super(contractHash);
        }

        @CallFlags(io.neow3j.devpack.constants.CallFlags.ReadStates)
        public native int getExecutingNonce();
    }

    static class MessageBridge extends ContractInterface {
        public MessageBridge(Hash160 contractHash) {
            super(contractHash);
        }

        @CallFlags(io.neow3j.devpack.constants.CallFlags.ReadOnly)
        public native N3MessageEnvelope.N3ExecutableMessage getMessage(int nonce);
    }

}
