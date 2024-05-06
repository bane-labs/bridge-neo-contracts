package network.bane.lib;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.contracts.CryptoLib;

import static io.neow3j.devpack.Helper.concat;

public class BridgeLib {

    public static final int UINT256_SIZE = 32;
    public static final byte HASH160_SIZE = 20;

    public static ByteString computeNewRoot(CryptoLib cryptoLib, ByteString left, ByteString right) {
        ByteString leftRight = new ByteString(concat(left.toByteArray(), right));
        return cryptoLib.sha256(leftRight);
    }

    private static byte[] padToBytes(byte[] data, int padToSize) {
        int dataSize = data.length;
        int toPad = padToSize - dataSize;
        assert toPad >= 0 : "Data is too long.";
        byte[] padding = new byte[toPad];
        return concat(data, padding);
    }

}
