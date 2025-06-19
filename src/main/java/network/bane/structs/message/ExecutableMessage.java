package network.bane.structs.message;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.annotations.Struct;

@Struct
public class ExecutableMessage {
    public Message.Metadata metadata;
    public ByteString messageBytes;

    public ExecutableMessage(Message message) {
        this.metadata = message.metadata;
        this.messageBytes = message.messageBytes;
    }
}
