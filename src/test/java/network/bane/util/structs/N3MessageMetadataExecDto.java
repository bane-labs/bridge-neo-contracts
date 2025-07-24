package network.bane.util.structs;

import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import network.bane.util.MessageBridge;

import java.io.IOException;
import java.math.BigInteger;

import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.utils.Numeric.toHexStringNoPrefix;
import static network.bane.util.structs.N3MessageDto.MESSAGE_TYPE_EXECUTABLE;

public class N3MessageMetadataExecDto extends N3MessageMetadataDto {
    public boolean storeResult;

    public N3MessageMetadataExecDto(BigInteger timestamp, Hash160 sender, boolean storeResult) {
        super(MESSAGE_TYPE_EXECUTABLE, timestamp, sender);
        this.storeResult = storeResult;
    }

    @Override
    public byte[] serialize(MessageBridge messageBridge) throws IOException {
        byte[] serializedBytes = messageBridge.serializeMetadataExec(this);
        return serializedBytes;
    }

    @Override
    public ContractParameter serializeToContractParameter(MessageBridge messageBridge) throws IOException {
        return byteArray(serialize(messageBridge));
    }
}
