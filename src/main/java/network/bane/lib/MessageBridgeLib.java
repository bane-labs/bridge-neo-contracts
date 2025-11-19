package network.bane.lib;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.List;
import io.neow3j.devpack.contracts.CryptoLib;
import io.neow3j.devpack.contracts.StdLib;
import network.bane.structs.message.NeoMessage;
import network.bane.structs.message.NeoMessageEnvelope;

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
        NeoMessage.NeoMetadata metadata = (NeoMessage.NeoMetadata) new StdLib().deserialize(metadataBytes);
        if (metadata.type == MESSAGE_TYPE_EXECUTABLE) {
            byte[] metadataConcatBytes = concatMetadata((NeoMessage.NeoMetadataExecutable) metadata);
            return new ByteString(concatMessage(nonce, metadataConcatBytes, msgBytes));
        }
        if (metadata.type == MESSAGE_TYPE_STORE_ONLY) {
            byte[] metadataConcatBytes = concatMetadata((NeoMessage.NeoMetadataStoreOnly) metadata);
            return new ByteString(concatMessage(nonce, metadataConcatBytes, msgBytes));
        }
        if (metadata.type == MESSAGE_TYPE_RESULT) {
            byte[] metadataConcatBytes = concatMetadata((NeoMessage.NeoMetadataResult) metadata);
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

    private static byte[] concatMetadata(NeoMessage.NeoMetadataExecutable metadata) {
        byte[] metadataBase = concatBaseMetadata(metadata.type, metadata.timestamp, metadata.sender);
        int storeResultByte = 0;
        if (metadata.storeResult) {
            storeResultByte = 1;
        }
        return concat(padToBytes(toByteArray(storeResultByte), BOOL_SIZE), metadataBase);
    }

    private static byte[] concatMetadata(NeoMessage.NeoMetadataStoreOnly metadata) {
        return concatBaseMetadata(metadata.type, metadata.timestamp, metadata.sender);
    }

    private static byte[] concatMetadata(NeoMessage.NeoMetadataResult metadata) {
        byte[] metadataBase = concatBaseMetadata(metadata.type, metadata.timestamp, metadata.sender);
        return concat(padToBytes(toByteArray(metadata.initialMessageNonce), UINT256_SIZE), metadataBase);
    }

    private static byte[] concatBaseMetadata(int type, int timestamp, Hash160 sender) {
        byte[] msgType = padToBytes(toByteArray(type), UINT8_SIZE);
        byte[] timestampP = padToBytes(toByteArray(timestamp), UINT256_SIZE);
        return concat(concat(sender.toByteArray(), timestampP), msgType);
    }

    public static ByteString hashMessageBridgeOp(CryptoLib cryptoLib, NeoMessageEnvelope neoMessageEnvelope) {
        return cryptoLib.keccak256(
                concatMessageBridgeOpData(neoMessageEnvelope.nonce, neoMessageEnvelope.message.metadataBytes,
                        neoMessageEnvelope.message.rawMessage
                )
        );
    }

    public static ByteString computeNewTopRoot(CryptoLib cryptoLib, ByteString formerRoot,
            List<NeoMessageEnvelope> neoMessages) {
        ByteString parent = formerRoot;
        int nrMessages = neoMessages.size();
        for (int i = 0; i < nrMessages; i++) {
            NeoMessageEnvelope neoMessageEnvelope = neoMessages.get(i);
            if (!NeoMessageEnvelope.isValid(neoMessageEnvelope)) abort("Invalid Neo message envelope");
            ByteString neoMessageHash = hashMessageBridgeOp(cryptoLib, neoMessageEnvelope);
            parent = computeNewRoot(cryptoLib, parent, neoMessageHash);
        }
        return parent;
    }

}
