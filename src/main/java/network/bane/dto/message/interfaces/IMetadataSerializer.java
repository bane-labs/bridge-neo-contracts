package network.bane.dto.message.interfaces;

import network.bane.dto.message.N3MessageMetadataExec;
import network.bane.dto.message.N3MessageMetadataResult;
import network.bane.dto.message.N3MessageMetadataStoreOnly;

import java.io.IOException;

public interface IMetadataSerializer {
    byte[] serializeMetadataExecutable(N3MessageMetadataExec metadata) throws IOException;
    byte[] serializeMetadataStoreOnly(N3MessageMetadataStoreOnly metadata) throws IOException;
    byte[] serializeMetadataResult(N3MessageMetadataResult metadata) throws IOException;
}
