package network.bane.util.structs;

import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.types.ContractParameter;
import network.bane.util.MessageBridge;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.utils.Numeric.cleanHexPrefix;
import static io.neow3j.utils.Numeric.toHexString;
import static java.util.Arrays.asList;
import static network.bane.util.MessageTestStorer.getMetadataFromStackItem;
import static network.bane.util.helper.TestHelper.messageBridge;

public class N3MessageDto {
    public static final int MESSAGE_TYPE_EXECUTABLE = 0;
    public static final int MESSAGE_TYPE_STORE_ONLY = 1;
    public static final int MESSAGE_TYPE_RESULT = 2;

    public N3MessageMetadataDto metadata;
    public String messageBytes;

    public N3MessageDto(N3MessageMetadataDto metadata, String messageBytes) {
        this.metadata = metadata;
        this.messageBytes = cleanHexPrefix(messageBytes);
    }

    public N3MessageDto(N3MessageMetadataDto metadata, byte[] messageBytes) {
        this.metadata = metadata;
        this.messageBytes = cleanHexPrefix(toHexString(messageBytes));
    }

    public static N3MessageDto fromStackItem(StackItem item) throws IOException {
        List<StackItem> itemList = item.getList();
        StackItem metadataItem = messageBridge.callInvokeFunction("deserialize",
                asList(byteArray(itemList.get(0).getByteArray()))).getInvocationResult().getFirstStackItem();
        N3MessageMetadataDto metadata = getMetadataFromStackItem(metadataItem);
        byte[] msgBytes = itemList.get(1).getByteArray();
        return new N3MessageDto(metadata, msgBytes);
    }

    public ContractParameter toContractParameter(MessageBridge messageBridge) throws IOException {
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
        if (!(other instanceof N3MessageDto)) {
            return false;
        }
        N3MessageDto that = (N3MessageDto) other;
        return metadata.equals(that.metadata) &&
                messageBytes.equals(that.messageBytes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metadata, messageBytes);
    }

}
