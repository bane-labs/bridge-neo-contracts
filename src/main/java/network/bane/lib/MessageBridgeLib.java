package network.bane.lib;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.List;
import io.neow3j.devpack.contracts.CryptoLib;
import io.neow3j.devpack.contracts.StdLib;
import network.bane.structs.message.N3Message;
import network.bane.structs.message.N3MessageEnvelope;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Helper.concat;
import static io.neow3j.devpack.Helper.reverse;
import static io.neow3j.devpack.Helper.toByteArray;
import static network.bane.lib.BridgeLib.BOOL_SIZE;
import static network.bane.lib.BridgeLib.UINT256_SIZE;
import static network.bane.lib.BridgeLib.UINT8_SIZE;
import static network.bane.lib.BridgeLib.computeNewRoot;
import static network.bane.lib.BridgeLib.padToBytes;

public class MessageBridgeLib {

    public static final int MESSAGE_TYPE_EXECUTABLE = 0;
    public static final int MESSAGE_TYPE_STORE_ONLY = 1;
    public static final int MESSAGE_TYPE_RESULT = 2;

    public static ByteString concatMessageBridgeOpData(int nonce, ByteString metadataBytes, ByteString msgBytes) {
        N3Message.N3Metadata metadata = (N3Message.N3Metadata) new StdLib().deserialize(metadataBytes);
        if (metadata.type == MESSAGE_TYPE_EXECUTABLE) {
            byte[] metadataConcatBytes = concatMetadata((N3Message.N3MetadataExecutable) metadata);
            return new ByteString(concatMessage(nonce, metadataConcatBytes, msgBytes));
        }
        if (metadata.type == MESSAGE_TYPE_STORE_ONLY) {
            byte[] metadataConcatBytes = concatMetadata((N3Message.N3MetadataStoreOnly) metadata);
            return new ByteString(concatMessage(nonce, metadataConcatBytes, msgBytes));
        }
        if (metadata.type == MESSAGE_TYPE_RESULT) {
            byte[] metadataConcatBytes = concatMetadata((N3Message.N3MetadataResult) metadata);
            return new ByteString(concatMessage(nonce, metadataConcatBytes, msgBytes));
        }
        abort("Unsupported message type");
        return null; // This line will never be reached, but is needed to satisfy the compiler.
    }

    private static byte[] concatMessage(int nonce, byte[] metadataConcatBytes, ByteString msgBytes) {
        byte[] noncePadded = padToBytes(toByteArray(nonce), UINT256_SIZE);
        byte[] msgBytesByteArray = msgBytes.toByteArray();
        reverse(msgBytesByteArray);
        byte[] completeConcat = concat(concat(msgBytesByteArray, metadataConcatBytes), noncePadded);
        reverse(completeConcat);
        return completeConcat;
    }

    private static byte[] concatMetadata(N3Message.N3MetadataExecutable metadata) {
        byte[] metadataBase = concateBaseMetadata(metadata.type, metadata.timestamp, metadata.sender);
        int storeResultByte = 0;
        if (metadata.storeResult) {
            storeResultByte = 1;
        }
        return concat(padToBytes(toByteArray(storeResultByte), BOOL_SIZE), metadataBase);
    }

    private static byte[] concatMetadata(N3Message.N3MetadataStoreOnly metadata) {
        return concateBaseMetadata(metadata.type, metadata.timestamp, metadata.sender);
    }

    private static byte[] concatMetadata(N3Message.N3MetadataResult metadata) {
        byte[] metadataBase = concateBaseMetadata(metadata.type, metadata.timestamp, metadata.sender);
        return concat(padToBytes(toByteArray(metadata.initialMessageNonce), UINT256_SIZE), metadataBase);
    }

    private static byte[] concateBaseMetadata(int type, int timestamp, Hash160 sender) {
        byte[] msgType = padToBytes(toByteArray(type), UINT8_SIZE);
        byte[] timestampP = padToBytes(toByteArray(timestamp), UINT256_SIZE);
        return concat(concat(sender.toByteArray(), timestampP), msgType);
    }

    private static ByteString hashMessageBridgeOp(CryptoLib cryptoLib, N3MessageEnvelope n3MessageEnvelope) {
        return cryptoLib.keccak256(
                concatMessageBridgeOpData(n3MessageEnvelope.nonce, n3MessageEnvelope.message.metadataBytes,
                        n3MessageEnvelope.message.messageBytes
                )
        );
    }

    public static ByteString computeNewTopRoot(CryptoLib cryptoLib, ByteString formerRoot,
            List<N3MessageEnvelope> n3Messages) {
        ByteString parent = formerRoot;
        int nrMessages = n3Messages.size();
        for (int i = 0; i < nrMessages; i++) {
            N3MessageEnvelope n3MessageEnvelope = n3Messages.get(i);
            if (!N3MessageEnvelope.isValid(n3MessageEnvelope)) abort("Invalid N3 message");
            ByteString n3MessageHash = hashMessageBridgeOp(cryptoLib, n3MessageEnvelope);
            parent = computeNewRoot(cryptoLib, parent, n3MessageHash);
        }
        return parent;
    }

}
