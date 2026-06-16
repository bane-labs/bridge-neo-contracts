package network.bane.dto.message.interfaces;

import network.bane.dto.message.N3MessageMetadataExec;
import network.bane.dto.message.N3MessageMetadataResult;
import network.bane.dto.message.N3MessageMetadataStoreOnly;

import java.io.IOException;

public interface IMetadataSerializer {
    byte[] serializeMetadataExec(N3MessageMetadataExec dto) throws IOException;
    byte[] serializeMetadataStoreOnly(N3MessageMetadataStoreOnly dto) throws IOException;
    byte[] serializeMetadataResult(N3MessageMetadataResult dto) throws IOException;
}
