package network.bane.util.structs;

import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;

import java.math.BigInteger;
import java.util.Objects;

import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.utils.Numeric.cleanHexPrefix;

public class N3MessageDto {
    public N3MessageMetadataDto metadata;
    public String messageCodeHex;

    public N3MessageDto(N3MessageMetadataDto metadata, String messageCodeHex) {
        this.metadata = metadata;
        this.messageCodeHex = cleanHexPrefix(messageCodeHex);
    }

    public ContractParameter toContractParameter() {
        return array(
                metadata.toContractParameter(),
                byteArray(messageCodeHex)
        );
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof N3MessageDto)) {
            return false;
        }
        N3MessageDto that = (N3MessageDto) other;
        return metadata.equals(that.metadata) &&
                messageCodeHex.equals(that.messageCodeHex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metadata, messageCodeHex);
    }

    public static class N3MessageMetadataDto {
        public BigInteger timestamp;
        public Hash160 sender;

        public N3MessageMetadataDto(BigInteger timestamp, Hash160 sender) {
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
            return timestamp.equals(that.timestamp) && sender.equals(that.sender);
        }

        @Override
        public int hashCode() {
            return Objects.hash(timestamp, sender);
        }

        public ContractParameter toContractParameter() {
            return array(integer(timestamp), hash160(sender));
        }

    }

}
