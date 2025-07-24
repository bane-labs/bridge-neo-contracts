package network.bane.lib;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.List;
import io.neow3j.devpack.contracts.CryptoLib;
import network.bane.structs.Withdrawal;

import static io.neow3j.devpack.Helper.concat;

public class BridgeLib {

    static final int BOOL_SIZE = 1;
    static final int UINT256_SIZE = 32;

    public static ByteString computeNewRoot(CryptoLib cryptoLib, ByteString left, ByteString right) {
        ByteString leftRight = new ByteString(concat(left.toByteArray(), right));
        return cryptoLib.keccak256(leftRight);
    }

    static byte[] padToBytes(byte[] data, int padToSize) {
        int dataSize = data.length;
        int toPad = padToSize - dataSize;
        assert toPad >= 0 : "Data too long";
        byte[] padding = new byte[toPad];
        return concat(data, padding);
    }

    // Makes sure the withdrawals have subsequent nonces.
    public static boolean subsequentNonces(List<Withdrawal> withdrawals, int startNonce) {
        for (int i = 1; i <= withdrawals.size(); i++) {
            if (withdrawals.get(i - 1).nonce != startNonce + i) {
                return false;
            }
        }
        return true;
    }

}
