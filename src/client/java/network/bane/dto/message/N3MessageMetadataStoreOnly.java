package network.bane.dto.message;

import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import network.bane.dto.message.interfaces.IMetadataSerializer;

import java.io.IOException;
import java.math.BigInteger;

import static io.neow3j.types.ContractParameter.byteArray;
import static network.bane.dto.message.N3Message.MESSAGE_TYPE_STORE_ONLY;

public class N3MessageMetadataStoreOnly extends N3MessageMetadata {
    public N3MessageMetadataStoreOnly(BigInteger timestamp, Hash160 sender) {
        super(MESSAGE_TYPE_STORE_ONLY, timestamp, sender);
    }

    @Override
    public byte[] serialize(IMetadataSerializer metadataSerializer) throws IOException {
        return metadataSerializer.serializeMetadataStoreOnly(this);
    }

    @Override
    public ContractParameter serializeToContractParameter(IMetadataSerializer metadataSerializer) throws IOException {
        return byteArray(serialize(metadataSerializer));
    }
}
