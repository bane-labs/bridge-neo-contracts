package network.bane.dto.message;

import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import network.bane.dto.message.interfaces.IMetadataSerializer;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Objects;

public abstract class N3MessageMetadata {
    public Integer type; // 0: EXECUTABLE, 1: STORE_ONLY, 2: RESULT
    public BigInteger timestamp;
    public Hash160 sender;

    public N3MessageMetadata(int type, BigInteger timestamp, Hash160 sender) {
        this.type = type;
        this.timestamp = timestamp;
        this.sender = sender;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof N3MessageMetadata)) {
            return false;
        }
        N3MessageMetadata that = (N3MessageMetadata) other;
        return Objects.equals(type, that.type) &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(sender, that.sender);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, timestamp, sender);
    }

    public byte[] serialize(IMetadataSerializer metadataSerializer) throws IOException {
        throw new UnsupportedOperationException("This method should be overridden in subclasses");
    }

    public ContractParameter serializeToContractParameter(IMetadataSerializer metadataSerializer) throws IOException {
        throw new UnsupportedOperationException("This method should be overridden in subclasses");
    }

}
