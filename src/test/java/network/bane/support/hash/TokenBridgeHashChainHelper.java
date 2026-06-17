package network.bane.support.hash;

import io.neow3j.types.Hash160;

import java.math.BigInteger;

import static io.neow3j.utils.ArrayUtils.concatenate;
import static io.neow3j.utils.ArrayUtils.reverseArray;
import static io.neow3j.utils.BigIntegers.toLittleEndianByteArrayZeroPadded;
import static io.neow3j.utils.Numeric.cleanHexPrefix;
import static network.bane.support.hash.HashChainHelper.concatAndKeccak256;
import static network.bane.support.hash.HashChainHelper.keccak256Hex;
import static network.bane.support.hash.HashChainHelper.prependIntToStringLittleEndian;
import static network.bane.support.TestConstants.UINT256_SIZE;

public class TokenBridgeHashChainHelper {

    public static String createDepositHash(BigInteger nonce, Hash160 to, BigInteger amount) {
        return keccak256Hex(concatDepositData(nonce, to, amount));
    }

    public static String createWithdrawalMessageToSign(BigInteger network, BigInteger linkedChainId, String root) {
        return prependIntToStringLittleEndian(network, prependIntToStringLittleEndian(linkedChainId, root));
    }

    public static String createDepositHashNoPrefix(BigInteger nonce, Hash160 to, BigInteger amount) {
        return cleanHexPrefix(createDepositHash(nonce, to, amount));
    }

    public static byte[] concatTokenOpData(Hash160 neoN3Token, Hash160 neoXToken, BigInteger nonce, Hash160 recipient,
            BigInteger value) {
        byte[] neoN3TokenArray = reverseArray(neoN3Token.toArray());
        byte[] neoXTokenArray = reverseArray(neoXToken.toArray());
        byte[] noncePadded = toLittleEndianByteArrayZeroPadded(nonce, UINT256_SIZE);
        byte[] recipientArray = reverseArray(recipient.toArray());
        byte[] valuePadded = toLittleEndianByteArrayZeroPadded(value, UINT256_SIZE);
        byte[] concatenated = concatenate(
                concatenate(concatenate(concatenate(valuePadded, recipientArray), noncePadded), neoXTokenArray),
                neoN3TokenArray);
        return reverseArray(concatenated);
    }

    public static String createTokenOpHash(Hash160 neoN3Token, Hash160 neoXToken, BigInteger nonce, Hash160 recipient,
            BigInteger value) {
        return keccak256Hex(concatTokenOpData(neoN3Token, neoXToken, nonce, recipient, value));
    }

    public static String createTokenOpHashNoPrefix(Hash160 neoN3Token, Hash160 neoXToken, BigInteger nonce,
            Hash160 recipient, BigInteger value) {
        return cleanHexPrefix(createTokenOpHash(neoN3Token, neoXToken, nonce, recipient, value));
    }


    public static byte[] concatDepositData(BigInteger nonce, Hash160 recipient, BigInteger amount) {
        byte[] noncePadded = toLittleEndianByteArrayZeroPadded(nonce, UINT256_SIZE);
        byte[] recipientArray = reverseArray(recipient.toArray());
        byte[] amountPadded = toLittleEndianByteArrayZeroPadded(amount, UINT256_SIZE);
        byte[] concatenated = concatenate(concatenate(amountPadded, recipientArray), noncePadded);
        return reverseArray(concatenated);
    }

    public static String computeNewTokenRootNoPrefix(String previousRoot, Hash160 neoN3Token, Hash160 neoXToken,
            BigInteger nonce, Hash160 recipient, BigInteger value) {
        return cleanHexPrefix(computeNewTokenRoot(previousRoot, neoN3Token, neoXToken, nonce, recipient, value));
    }

    public static String computeNewTokenRoot(String previousRoot, Hash160 neoN3Token, Hash160 neoXToken,
            BigInteger nonce, Hash160 recipient, BigInteger value) {
        return concatAndKeccak256(previousRoot,
                createTokenOpHashNoPrefix(neoN3Token, neoXToken, nonce, recipient, value));
    }

}
