package network.bane.lib;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.List;
import io.neow3j.devpack.contracts.CryptoLib;
import io.neow3j.devpack.contracts.FungibleToken;
import network.bane.structs.Withdrawal;

import static io.neow3j.devpack.Helper.concat;

public class BridgeLib {

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

    /**
     * Calculates the scaling factor for the decimals of a token on the native chain to the decimals on the linked chain.
     * <p>
     * The side with fewer decimals is used as the base for the hash computation.
     *
     * @param tokenForNativeBridge the token hash of the token that is used for the native bridge.
     * @param decimalsOnLinkedChain the number of decimals of the native token on the linked chain.
     * @return the decimal scaling factor.
     */
    public static int calculateDecimalScalingFactor(Hash160 tokenForNativeBridge, int decimalsOnLinkedChain) {
        int decimalsHere = new FungibleToken(tokenForNativeBridge).decimals();
        if (decimalsHere > decimalsOnLinkedChain) {
            return decimalsHere - decimalsOnLinkedChain;
        } else {
            return 0;
        }
    }

}
