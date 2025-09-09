package network.bane.structs.message;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.annotations.Struct;

import static network.bane.lib.MessageBridgeLib.MESSAGE_TYPE_EXECUTABLE;
import static network.bane.lib.MessageBridgeLib.MESSAGE_TYPE_RESULT;
import static network.bane.lib.MessageBridgeLib.MESSAGE_TYPE_STORE_ONLY;

@Struct
public class N3Message {
    /**
     * Metadata for the message, which can be of different types. It includes context about how the message bytes
     * should be processed.
     */
    public ByteString metadataBytes;
    /**
     * The actual raw message bytes.
     */
    public ByteString rawMessage;

    public N3Message(ByteString metadataBytes, ByteString rawMessage) {
        this.metadataBytes = metadataBytes;
        this.rawMessage = rawMessage;
    }

    public static boolean isValid(N3Message message) {
        return message != null &&
                message.metadataBytes != null &&
                message.rawMessage != null && message.rawMessage.length() > 0;
    }

    @Struct
    public static class N3MetadataExecutable extends N3Metadata {
        public boolean storeResult;

        public N3MetadataExecutable(int timestamp, Hash160 sender, boolean storeResult) {
            super(MESSAGE_TYPE_EXECUTABLE, timestamp, sender);
            this.storeResult = storeResult;
        }
    }

    @Struct
    public static class N3MetadataStoreOnly extends N3Metadata {
        public N3MetadataStoreOnly(int timestamp, Hash160 sender) {
            super(MESSAGE_TYPE_STORE_ONLY, timestamp, sender);
        }
    }

    @Struct
    public static class N3MetadataResult extends N3Metadata {
        public int initialMessageNonce;

        public N3MetadataResult(int timestamp, Hash160 sender, int initialMessageNonce) {
            super(MESSAGE_TYPE_RESULT, timestamp, sender);
            this.initialMessageNonce = initialMessageNonce;
        }
    }

    @Struct
    public abstract static class N3Metadata {
        /**
         * Type of the metadata, indicating how the message should be processed.
         */
        public int type; // 0: EXECUTABLE, 1: STORE_ONLY, 2: RESULT
        /**
         * Timestamp of the message indicating when it was sent.
         */
        public int timestamp;
        /**
         * The sender of the message, represented as a Hash160 address.
         */
        public Hash160 sender;

        public N3Metadata(int type, int timestamp, Hash160 sender) {
            this.type = type;
            this.timestamp = timestamp;
            this.sender = sender;
        }

        public static boolean isValid(N3Metadata metadata) {
            return metadata != null &&
                    metadata.timestamp > 0 &&
                    metadata.sender != null && Hash160.isValid(metadata.sender) && !metadata.sender.isZero();
        }
    }

}
