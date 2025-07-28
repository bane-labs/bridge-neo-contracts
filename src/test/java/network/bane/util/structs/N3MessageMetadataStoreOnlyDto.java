package network.bane.util.structs;

import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import network.bane.util.MessageBridge;

import java.io.IOException;
import java.math.BigInteger;

import static io.neow3j.types.ContractParameter.byteArray;
import static network.bane.util.structs.N3MessageDto.MESSAGE_TYPE_STORE_ONLY;

public class N3MessageMetadataStoreOnlyDto extends N3MessageMetadataDto {
    public N3MessageMetadataStoreOnlyDto(BigInteger timestamp, Hash160 sender) {
        super(MESSAGE_TYPE_STORE_ONLY, timestamp, sender);
    }

    @Override
    public byte[] serialize(MessageBridge messageBridge) throws IOException {
        return messageBridge.serializeMetadataStoreOnly(this);
    }

    @Override
    public ContractParameter serializeToContractParameter(MessageBridge messageBridge) throws IOException {
        return byteArray(serialize(messageBridge));
    }
}
