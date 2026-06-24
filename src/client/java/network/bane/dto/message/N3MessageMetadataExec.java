package network.bane.dto.message;

import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import network.bane.dto.message.interfaces.IMetadataSerializer;

import java.io.IOException;
import java.math.BigInteger;

import static io.neow3j.types.ContractParameter.byteArray;
import static network.bane.dto.message.N3Message.MESSAGE_TYPE_EXECUTABLE;

public class N3MessageMetadataExec extends N3MessageMetadata {
    public boolean storeResult;

    public N3MessageMetadataExec(BigInteger timestamp, Hash160 sender, boolean storeResult) {
        super(MESSAGE_TYPE_EXECUTABLE, timestamp, sender);
        this.storeResult = storeResult;
    }

    @Override
    public byte[] serialize(IMetadataSerializer metadataSerializer) throws IOException {
        return metadataSerializer.serializeMetadataExecutable(this);
    }

    @Override
    public ContractParameter serializeToContractParameter(IMetadataSerializer metadataSerializer) throws IOException {
        return byteArray(serialize(metadataSerializer));
    }
}
