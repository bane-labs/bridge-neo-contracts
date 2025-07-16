package network.bane.lib;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.List;
import io.neow3j.devpack.contracts.CryptoLib;
import network.bane.structs.message.N3MessageEnvelope;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Helper.concat;
import static io.neow3j.devpack.Helper.reverse;
import static io.neow3j.devpack.Helper.toByteArray;
import static network.bane.lib.BridgeLib.UINT256_SIZE;
import static network.bane.lib.BridgeLib.computeNewRoot;
import static network.bane.lib.BridgeLib.padToBytes;

public class MessageBridgeLib {

    private static ByteString hashMessageBridgeOp(CryptoLib cryptoLib, N3MessageEnvelope n3MessageEnvelope) {
        return cryptoLib.keccak256(
                concatMessageBridgeOpData(n3MessageEnvelope.nonce, n3MessageEnvelope.message.metadata.timestamp,
                        n3MessageEnvelope.message.metadata.sender, n3MessageEnvelope.message.executableCode
                )
        );
    }

    private static ByteString concatMessageBridgeOpData(int nonce, int timestamp, Hash160 sender,
            ByteString executableCode) {
        byte[] nonceP = padToBytes(toByteArray(nonce), UINT256_SIZE);
        byte[] timestampP = padToBytes(toByteArray(timestamp), UINT256_SIZE);
        // nonce-uint256, timestamp-uint256, sender-uint160, executableCode-arbitrarysize
        byte[] concatenated = concat(
                concat(concat(executableCode.toByteArray(), sender.toByteString()), timestampP), nonceP);
        reverse(concatenated);
        return new ByteString(concatenated);
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
