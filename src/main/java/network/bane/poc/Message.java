package network.bane.poc;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.annotations.Struct;

@Struct
public class Message {
//    public int nonce; // Todo:
    public Metadata metadata;
    public ByteString invocation;

    public Message(Metadata metadata, ByteString invocation) {
        this.metadata = metadata;
        this.invocation = invocation;
    }

    public boolean isValid() {
        return metadata != null && invocation != null && metadata.isValid();
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
