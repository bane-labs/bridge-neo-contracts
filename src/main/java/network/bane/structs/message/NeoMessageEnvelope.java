package network.bane.structs.message;

import io.neow3j.devpack.annotations.Struct;

@Struct
public class NeoMessageEnvelope {
    public Integer nonce;
    public NeoMessage message;

    public NeoMessageEnvelope(Integer nonce, NeoMessage message) {
        this.nonce = nonce;
        this.message = message;
    }

    public static boolean isValid(NeoMessageEnvelope message) {
        return message != null &&
                message.nonce != null &&
                message.message != null && NeoMessage.isValid(message.message);
    }

}
