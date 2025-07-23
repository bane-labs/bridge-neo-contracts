package network.bane.structs.message;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.annotations.Struct;

@Struct
public class N3Message {
    /**
     * The actual executable message bytes.
     */
    public ByteString messageBytes;
    /**
     * Type of metadata associated with the message. This indicates how the metadata bytes should be interpreted.
     */
    public Integer messageType;
    /**
     * Metadata for the message, which can be of different types. It includes context about how the message bytes
     * should be processed.
     */
    public ByteString metadataBytes;

    public N3Message(ByteString messageBytes, int messageType, ByteString metadataBytes) {
        this.messageBytes = messageBytes;
        this.messageType = messageType;
        this.metadataBytes = metadataBytes;
    }

    public static boolean isValid(N3Message execN3Message) {
        // Todo: Implement validation logic for N3Message.
        return true;
    }

    @Struct
    public static class N3MetadataExecutable extends N3Metadata {
        public boolean storeResult;
        public N3MetadataExecutable(int timestamp, Hash160 sender, boolean storeResult) {
            super(timestamp, sender);
            this.storeResult = storeResult;
        }
    }

    @Struct
    public static class N3MetadataStoreOnly extends N3Metadata {
        public N3MetadataStoreOnly(int timestamp, Hash160 sender) {
            super(timestamp, sender);
        }
    }

    @Struct
    public static class N3MetadataResult extends N3Metadata {
        public int initialMessageNonce;
        public N3MetadataResult(int timestamp, Hash160 sender, int initialMessageNonce) {
            super(timestamp, sender);
            this.initialMessageNonce = initialMessageNonce;
        }
    }

    @Struct
    private static class N3Metadata {
        /**
         * Timestamp of the message indicating when it was sent.
         */
        public int timestamp;
        /**
         * The sender of the message, represented as a Hash160 address.
         */
        public Hash160 sender;

        public N3Metadata(int timestamp, Hash160 sender) {
            this.timestamp = timestamp;
            this.sender = sender;
        }

        public static boolean isValid(N3Metadata metadata) {
            return metadata.timestamp > 0 &&
                    metadata.sender != null && Hash160.isValid(metadata.sender) && !metadata.sender.isZero();
        }
    }

}
