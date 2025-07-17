package network.bane.structs.message;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.annotations.Struct;

@Struct
public class N3MessageEnvelope {
    public Integer nonce;
    public N3ExecutableMessage message;

    public N3MessageEnvelope(Integer nonce, N3ExecutableMessage message) {
        this.nonce = nonce;
        this.message = message;
    }

    public static boolean isValid(N3MessageEnvelope message) {
        return message.nonce != null &&
                message.message != null && N3ExecutableMessage.isValid(message.message);
    }

    @Struct
    public static class N3ExecutableMessage {
        /**
         * Metadata for the N3 message.
         */
        public N3Metadata metadata;
        /**
         * The actual executable message bytes.
         */
        public ByteString executableCode;

        public N3ExecutableMessage(N3Metadata metadata, ByteString executableCode) {
            this.metadata = metadata;
            this.executableCode = executableCode;
        }

        public static boolean isValid(N3ExecutableMessage execN3Message) {
            return execN3Message.metadata != null && N3Metadata.isValid(execN3Message.metadata) &&
                    execN3Message.executableCode != null;
        }

        @Struct
        public static class N3Metadata {
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

}
