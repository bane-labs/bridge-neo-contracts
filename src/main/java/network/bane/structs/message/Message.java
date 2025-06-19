package network.bane.structs.message;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.annotations.Struct;

@Struct
public class Message {
    public Integer nonce;
    public Metadata metadata;
    public ByteString messageBytes;

    public Message(Integer nonce, Metadata metadata, ByteString messageBytes) {
        this.nonce = nonce;
        this.metadata = metadata;
        this.messageBytes = messageBytes;
    }

    public static boolean isValid(Message message) {
        return message.nonce != null &&
                message.metadata != null && message.metadata.isValid() &&
                message.messageBytes != null;
    }

    public static class Metadata {
        public int timestamp;
        public Hash160 sender;

        public Metadata(int timestamp, Hash160 sender) {
            this.timestamp = timestamp;
            this.sender = sender;
        }

        public boolean isValid() {
            return timestamp > 0 && sender != null && Hash160.isValid(sender) && !sender.isZero();
        }
    }

}
