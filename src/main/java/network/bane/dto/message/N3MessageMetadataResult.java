package network.bane.dto.message;

import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import network.bane.dto.message.interfaces.IMetadataSerializer;

import java.io.IOException;
import java.math.BigInteger;

import static io.neow3j.types.ContractParameter.byteArray;
import static network.bane.dto.message.N3Message.MESSAGE_TYPE_RESULT;

public class N3MessageMetadataResult extends N3MessageMetadata {
    public BigInteger relatedMessageNonce;

    public N3MessageMetadataResult(BigInteger timestamp, Hash160 sender, BigInteger relatedMessageNonce) {
        super(MESSAGE_TYPE_RESULT, timestamp, sender);
        this.relatedMessageNonce = relatedMessageNonce;
    }

    @Override
    public byte[] serialize(IMetadataSerializer metadataSerializer) throws IOException {
        return metadataSerializer.serializeMetadataResult(this);
    }

    @Override
    public ContractParameter serializeToContractParameter(IMetadataSerializer metadataSerializer) throws IOException {
        return byteArray(serialize(metadataSerializer));
    }
}
