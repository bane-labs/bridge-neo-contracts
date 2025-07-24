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
import static network.bane.lib.BridgeLib.computeNewRoot;
import static network.bane.lib.BridgeLib.padToBytes;

public class MessageBridgeLib {

    public static final int MESSAGE_TYPE_EXECUTABLE = 0;
    public static final int MESSAGE_TYPE_STORE_ONLY = 1;
    public static final int MESSAGE_TYPE_RESULT = 2;

    private static ByteString concatMessageBridgeOpData(int nonce, ByteString msgBytes, ByteString metadataBytes) {
        N3Message.N3Metadata metadata = (N3Message.N3Metadata) new StdLib().deserialize(metadataBytes);
        if (metadata.type == MESSAGE_TYPE_EXECUTABLE) {
            return concatenateExecutable(nonce, msgBytes, (N3Message.N3MetadataExecutable) metadata);
        }
        if (metadata.type == MESSAGE_TYPE_STORE_ONLY) {
            return concatenateStoreOnly(nonce, msgBytes, (N3Message.N3MetadataStoreOnly) metadata);
        }
        if (metadata.type != MESSAGE_TYPE_RESULT) {
            abort("Unsupported message type");
        }
        return concatenateResult(nonce, msgBytes, (N3Message.N3MetadataResult) metadata);
    }

    // nonce-uint256, messagebytes-arbitrarysize, type-uint256, timestamp-uint256, sender-uint160, storeResult-bool
    private static ByteString concatenateExecutable(int nonce, ByteString msgBytes,
            N3Message.N3MetadataExecutable metadata) {
        byte[] baseConcat = concatenate(nonce, msgBytes, metadata.type, metadata.timestamp, metadata.sender);
        int storeResultByte = 0;
        if (metadata.storeResult) {
            storeResultByte = 1;
        }
        byte[] c = concat(padToBytes(toByteArray(storeResultByte), BOOL_SIZE), baseConcat);
        reverse(c);
        return new ByteString(c);
    }

    // nonce-uint256, messagebytes-arbitrarysize, type-uint256, timestamp-uint256, sender-uint160,
    // initialMsgNonce-uint256
    private static ByteString concatenateResult(int nonce, ByteString messageBytes,
            N3Message.N3MetadataResult metadata) {
        byte[] baseConcat = concatenate(nonce, messageBytes, metadata.type, metadata.timestamp, metadata.sender);
        byte[] initialMsgNonceP = padToBytes(toByteArray(metadata.initialMessageNonce), UINT256_SIZE);
        byte[] concatenated = concat(initialMsgNonceP, baseConcat);
        reverse(concatenated);
        return new ByteString(concatenated);
    }

    // nonce-uint256, messagebytes-arbitrarysize, type-uint256, timestamp-uint256, sender-uint160
    private static ByteString concatenateStoreOnly(int nonce, ByteString messageBytes,
            N3Message.N3MetadataStoreOnly metadata) {
        byte[] c = concatenate(nonce, messageBytes, metadata.type, metadata.timestamp, metadata.sender);
        reverse(c);
        return new ByteString(c);
    }

    private static byte[] concatenate(int nonce, ByteString messageBytes, int messageType, int timestamp,
            Hash160 sender) {
        byte[] nonceP = padToBytes(toByteArray(nonce), UINT256_SIZE);
        byte[] messageTypeP = padToBytes(toByteArray(messageType), UINT256_SIZE);
        byte[] timestampP = padToBytes(toByteArray(timestamp), UINT256_SIZE);
        return concat(concat(concat(concat(sender.toByteArray(), timestampP), messageTypeP), messageBytes), nonceP);
    }

    private static ByteString hashMessageBridgeOp(CryptoLib cryptoLib, N3MessageEnvelope n3MessageEnvelope) {
        return cryptoLib.keccak256(
                concatMessageBridgeOpData(n3MessageEnvelope.nonce, n3MessageEnvelope.message.messageBytes,
                        n3MessageEnvelope.message.metadataBytes
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
