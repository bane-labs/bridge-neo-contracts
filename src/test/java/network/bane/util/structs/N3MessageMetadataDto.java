package network.bane.util.structs;

import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import network.bane.util.MessageBridge;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Objects;

public abstract class N3MessageMetadataDto {
    public Integer type; // 0: EXECUTABLE, 1: STORE_ONLY, 2: RESULT
    public BigInteger timestamp;
    public Hash160 sender;

    public N3MessageMetadataDto(int type, BigInteger timestamp, Hash160 sender) {
        this.type = type;
        this.timestamp = timestamp;
        this.sender = sender;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof N3MessageMetadataDto)) {
            return false;
        }
        N3MessageMetadataDto that = (N3MessageMetadataDto) other;
        return Objects.equals(type, that.type) &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(sender, that.sender);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, timestamp, sender);
    }

    public byte[] serialize(MessageBridge messageBridge) throws IOException {
        throw new UnsupportedOperationException("This method should be overridden in subclasses");
    }

    public ContractParameter serializeToContractParameter(MessageBridge messageBridge) throws IOException {
        throw new UnsupportedOperationException("This method should be overridden in subclasses");
    }

}
