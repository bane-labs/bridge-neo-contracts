package network.bane.structs.message;

import io.neow3j.devpack.annotations.Struct;

@Struct
public class N3MessageEnvelope {
    public Integer nonce;
    public N3Message message;

    public N3MessageEnvelope(Integer nonce, N3Message message) {
        this.nonce = nonce;
        this.message = message;
    }

    public static boolean isValid(N3MessageEnvelope message) {
        return message != null &&
                message.nonce != null &&
                message.message != null && N3Message.isValid(message.message);
    }

}
