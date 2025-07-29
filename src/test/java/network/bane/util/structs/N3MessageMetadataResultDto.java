package network.bane.util.structs;

import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import network.bane.util.MessageBridge;

import java.io.IOException;
import java.math.BigInteger;

import static io.neow3j.types.ContractParameter.byteArray;
import static network.bane.util.structs.N3MessageDto.MESSAGE_TYPE_RESULT;

public class N3MessageMetadataResultDto extends N3MessageMetadataDto {
    public BigInteger relatedMessageNonce;

    public N3MessageMetadataResultDto(BigInteger timestamp, Hash160 sender, BigInteger relatedMessageNonce) {
        super(MESSAGE_TYPE_RESULT, timestamp, sender);
        this.relatedMessageNonce = relatedMessageNonce;
    }

    @Override
    public byte[] serialize(MessageBridge messageBridge) throws IOException {
        return messageBridge.serializeMetadataResult(this);
    }

    @Override
    public ContractParameter serializeToContractParameter(MessageBridge messageBridge) throws IOException {
        return byteArray(serialize(messageBridge));
    }
}
