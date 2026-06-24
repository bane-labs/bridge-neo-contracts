package network.bane.dto.message.interfaces;

import io.neow3j.protocol.core.stackitem.StackItem;

import java.io.IOException;

public interface INeoDeserializer {
    StackItem deserialize(byte[] bytes) throws IOException;
}
