package network.bane.dto.message;

import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import network.bane.dto.message.interfaces.IMetadataSerializer;
import network.bane.dto.message.interfaces.INeoDeserializer;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.utils.Numeric.cleanHexPrefix;
import static io.neow3j.utils.Numeric.toHexString;

public class N3Message {
    public static final int MESSAGE_TYPE_EXECUTABLE = 0;
    public static final int MESSAGE_TYPE_STORE_ONLY = 1;
    public static final int MESSAGE_TYPE_RESULT = 2;

    public N3MessageMetadata metadata;
    public String messageBytes;

    public N3Message(N3MessageMetadata metadata, String messageBytes) {
        this.metadata = metadata;
        this.messageBytes = cleanHexPrefix(messageBytes);
    }

    public N3Message(N3MessageMetadata metadata, byte[] messageBytes) {
        this.metadata = metadata;
        this.messageBytes = cleanHexPrefix(toHexString(messageBytes));
    }

    public static N3Message fromStackItem(INeoDeserializer neoDeserializer, StackItem item) throws IOException {
        List<StackItem> itemList = item.getList();
        StackItem metadataItem = neoDeserializer.deserialize(itemList.get(0).getByteArray());
        N3MessageMetadata metadata = getMetadataFromStackItem(metadataItem);
        byte[] msgBytes = itemList.get(1).getByteArray();
        return new N3Message(metadata, msgBytes);
    }

    private static N3MessageMetadata getMetadataFromStackItem(StackItem stackItem) {
        List<StackItem> items = stackItem.getList();
        int type = items.get(0).getInteger().intValue();
        BigInteger timestamp = items.get(1).getInteger();
        Hash160 sender = Hash160.fromAddress(items.get(2).getAddress());
        if (type == N3Message.MESSAGE_TYPE_STORE_ONLY) {
            return new N3MessageMetadataStoreOnly(timestamp, sender);
        }
        if (type == N3Message.MESSAGE_TYPE_EXECUTABLE) {
            boolean storeResult = items.get(3).getBoolean();
            return new N3MessageMetadataExec(timestamp, sender, storeResult);
        }
        if (type == N3Message.MESSAGE_TYPE_RESULT) {
            BigInteger initialMsgNonce = items.get(3).getInteger();
            return new N3MessageMetadataResult(timestamp, sender, initialMsgNonce);
        }
        throw new IllegalStateException("Unexpected metadata format");
    }

    public ContractParameter toContractParameter(IMetadataSerializer messageBridge) throws IOException {
        return array(
                metadata.serializeToContractParameter(messageBridge),
                byteArray(messageBytes)
        );
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof N3Message)) {
            return false;
        }
        N3Message that = (N3Message) other;
        return metadata.equals(that.metadata) &&
                messageBytes.equals(that.messageBytes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metadata, messageBytes);
    }

}
