package network.bane.lib;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.List;
import io.neow3j.devpack.contracts.CryptoLib;
import network.bane.structs.message.Message;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Helper.concat;
import static io.neow3j.devpack.Helper.reverse;
import static io.neow3j.devpack.Helper.toByteArray;
import static network.bane.lib.BridgeLib.UINT256_SIZE;
import static network.bane.lib.BridgeLib.computeNewRoot;
import static network.bane.lib.BridgeLib.padToBytes;

public class MessageBridgeLib {

    public static ByteString hashMessageBridgeOp(CryptoLib cryptoLib, Message message) {
        return cryptoLib.keccak256(concatMessageBridgeOpData(message));
    }

    private static ByteString concatMessageBridgeOpData(Message message) {
        byte[] nonceP = padToBytes(toByteArray(message.nonce), UINT256_SIZE);
        byte[] timestampP = padToBytes(toByteArray(message.metadata.timestamp), UINT256_SIZE);
        byte[] concatenated = concat(
                concat(
                        concat(message.messageBytes.toByteArray(), message.metadata.sender.toByteString()),
                        timestampP
                ),
                nonceP
        );
        reverse(concatenated);
        return new ByteString(concatenated);
    }

    public static ByteString computeNewTopRoot(CryptoLib cryptoLib, ByteString formerRoot, List<Message> messages) {
        ByteString parent = formerRoot;
        int nrMessages = messages.size();
        for (int i = 0; i > nrMessages; i++) {
            Message message = messages.get(i);
            if (!Message.isValid(message)) abort("Invalid message");
            ByteString messageHash = hashMessageBridgeOp(cryptoLib, message);
            parent = computeNewRoot(cryptoLib, parent, messageHash);
        }
        return parent;
    }

}
