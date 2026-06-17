package network.bane.support.hash;

import network.bane.dto.message.N3MessageMetadata;
import network.bane.dto.message.N3MessageMetadataExec;
import network.bane.dto.message.N3MessageMetadataResult;
import network.bane.dto.message.N3MessageMetadataStoreOnly;

import java.math.BigInteger;

import static io.neow3j.utils.ArrayUtils.concatenate;
import static io.neow3j.utils.ArrayUtils.reverseArray;
import static io.neow3j.utils.BigIntegers.toLittleEndianByteArrayZeroPadded;
import static io.neow3j.utils.Numeric.hexStringToByteArray;
import static network.bane.support.hash.HashChainHelper.keccak256Hex;
import static network.bane.support.TestConstants.BOOL_SIZE;
import static network.bane.support.TestConstants.UINT256_SIZE;
import static network.bane.support.TestConstants.UINT8_SIZE;

public class MessageBridgeHashChainHelper {

    // region message hash chain

    public static String createN3MessageHash(BigInteger nonce, N3MessageMetadataExec metadata, String rawMsgHex) {
        return createN3MessageHash(nonce, metadata, hexStringToByteArray(rawMsgHex));
    }

    public static String createN3MessageHash(BigInteger nonce, N3MessageMetadataExec metadata, byte[] rawMsgBytes) {
        return keccak256Hex(concatenateOp(nonce, metadata, rawMsgBytes));
    }

    public static String createN3MessageHash(BigInteger nonce, N3MessageMetadataStoreOnly metadata,
            String rawMsgHex) {
        return createN3MessageHash(nonce, metadata, hexStringToByteArray(rawMsgHex));
    }

    public static String createN3MessageHash(BigInteger nonce, N3MessageMetadataStoreOnly metadata, byte[] rawMsgBytes) {
        return keccak256Hex(concatenateOp(nonce, metadata, rawMsgBytes));
    }

    public static String createN3MessageHash(BigInteger nonce, N3MessageMetadataResult metadata, String rawMsgHex) {
        return createN3MessageHash(nonce, metadata, hexStringToByteArray(rawMsgHex));
    }

    public static String createN3MessageHash(BigInteger nonce, N3MessageMetadataResult metadata, byte[] rawMsgBytes) {
        return keccak256Hex(concatenateOp(nonce, metadata, rawMsgBytes));
    }

    public static byte[] concatenateOp(BigInteger nonce, N3MessageMetadataExec metadata, byte[] rawMsgBytes) {
        return concatenateOp(nonce, concatenateMetadata(metadata), rawMsgBytes);
    }

    public static byte[] concatenateOp(BigInteger nonce, N3MessageMetadataStoreOnly metadata, byte[] rawMsgBytes) {
        return concatenateOp(nonce, concatenateMetadata(metadata), rawMsgBytes);
    }

    public static byte[] concatenateOp(BigInteger nonce, N3MessageMetadataResult metadata, byte[] rawMsgBytes) {
        return concatenateOp(nonce, concatenateMetadata(metadata), rawMsgBytes);
    }

    private static byte[] concatenateOp(BigInteger nonce, byte[] metadataConcat, byte[] rawMsgBytes) {
        byte[] noncePadded = toLittleEndianByteArrayZeroPadded(nonce, UINT256_SIZE);
        return reverseArray(concatenate(concatenate(reverseArray(rawMsgBytes), metadataConcat), noncePadded));
    }

    private static byte[] concatenateMetadata(N3MessageMetadataExec metadata) {
        byte[] metadataBase = concatenateMetadataBase(metadata);
        byte storeResultByte = 0;
        if (metadata.storeResult) {
            storeResultByte = 1;
        }
        byte[] storeResultPadded = toLittleEndianByteArrayZeroPadded(storeResultByte, BOOL_SIZE);
        return concatenate(storeResultPadded, metadataBase);
    }

    private static byte[] concatenateMetadata(N3MessageMetadataStoreOnly metadata) {
        return concatenateMetadataBase(metadata);
    }

    private static byte[] concatenateMetadata(N3MessageMetadataResult metadata) {
        byte[] metadataBase = concatenateMetadataBase(metadata);
        byte[] relatedMsgNonce = toLittleEndianByteArrayZeroPadded(metadata.relatedMessageNonce, UINT256_SIZE);
        return concatenate(relatedMsgNonce, metadataBase);
    }

    private static byte[] concatenateMetadataBase(N3MessageMetadata metadata) {
        byte[] msgTypePadded = toLittleEndianByteArrayZeroPadded(metadata.type, UINT8_SIZE);
        byte[] timestampPadded = toLittleEndianByteArrayZeroPadded(metadata.timestamp, UINT256_SIZE);
        byte[] senderArray = reverseArray(metadata.sender.toArray());
        return concatenate(concatenate(senderArray, timestampPadded), msgTypePadded);
    }

    // endregion

}
