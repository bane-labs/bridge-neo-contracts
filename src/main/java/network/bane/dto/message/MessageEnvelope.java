package network.bane.dto.message;

import java.math.BigInteger;

public class MessageEnvelope {
    private BigInteger nonce;
    private N3Message message;

    public MessageEnvelope(BigInteger nonce, N3Message message) {
        this.nonce = nonce;
        this.message = message;
    }

    public BigInteger getNonce() {
        return nonce;
    }

    public N3Message getMessage() {
        return message;
    }
}
