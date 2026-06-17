package network.bane.support.hash;

import io.neow3j.crypto.Hash;
import io.neow3j.utils.BigIntegers;

import java.math.BigInteger;

import static io.neow3j.utils.ArrayUtils.concatenate;
import static io.neow3j.utils.Numeric.cleanHexPrefix;
import static io.neow3j.utils.Numeric.hexStringToByteArray;
import static io.neow3j.utils.Numeric.toHexString;
import static io.neow3j.utils.Numeric.toHexStringNoPrefix;
import static java.lang.String.format;

public class HashChainHelper {

    public static byte[] concatLeftRight(String leftHex, String rightHex) {
        return concatenate(hexStringToByteArray(leftHex), hexStringToByteArray(rightHex));
    }

    public static String keccak256Hex(byte[] input) {
        return toHexString(Hash.keccak256(input));
    }

    public static String keccak256HexNoPrefix(byte[] input) {
        return cleanHexPrefix(keccak256Hex(input));
    }

    public static String concatAndKeccak256(String leftHex, String rightHex) {
        return keccak256Hex(concatLeftRight(leftHex, rightHex));
    }

    public static String prependIntToStringLittleEndian(BigInteger intValue, String stringValue) {
        byte[] chainIdLittleEndian = BigIntegers.toLittleEndianByteArray(intValue);
        return toHexStringNoPrefix(concatenate(chainIdLittleEndian, hexStringToByteArray(stringValue)));
    }

    private static byte[] padToBytes(byte[] data, int padToSize) {
        int dataSize = data.length;
        int toPad = padToSize - dataSize;
        assert toPad >= 0 : "Data is too long.";
        byte[] padding = new byte[toPad];
        return concatenate(data, padding);
    }

    // big-endian modification of io.neow3j.utils.BigIntegers.toLittleEndianByteArrayZeroPadded()
    public static byte[] toBigEndianByteArrayZeroPadded(BigInteger value, int length) {
        // BigInteger.toByteArray() returns the two's complement of the number in big-endian order.
        byte[] bytes = value.toByteArray();
        if (bytes.length > length) {
            throw new IllegalArgumentException(
                    format("given integer needs more space (%s bytes) than the given " + "minimum length (%s bytes).",
                            bytes.length, length));
        }
        if (bytes.length < length) {
            byte[] temp = new byte[length];
            System.arraycopy(bytes, 0, temp, length - bytes.length, bytes.length);
            return temp;
        }
        return bytes;
    }

}
