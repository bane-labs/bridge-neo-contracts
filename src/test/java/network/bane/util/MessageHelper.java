package network.bane.util;

import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.Notification;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import network.bane.util.structs.N3MessageMetadataDto;
import network.bane.util.structs.N3MessageMetadataExecDto;
import network.bane.util.structs.N3MessageMetadataResultDto;
import network.bane.util.structs.N3MessageMetadataStoreOnlyDto;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

import static io.neow3j.utils.ArrayUtils.concatenate;
import static io.neow3j.utils.ArrayUtils.reverseArray;
import static io.neow3j.utils.BigIntegers.toLittleEndianByteArrayZeroPadded;
import static io.neow3j.utils.Numeric.hexStringToByteArray;
import static network.bane.util.TestHelper.BOOL_SIZE;
import static network.bane.util.TestHelper.UINT256_SIZE;
import static network.bane.util.TestHelper.UINT8_SIZE;
import static network.bane.util.TestHelper.keccak256Hex;
import static network.bane.util.helper.TestHelper.messageBridge;

public class MessageHelper {

    // region message hash chain

    public static String createN3MessageHash(BigInteger nonce, N3MessageMetadataExecDto metadata, String rawMsgHex) {
        return createN3MessageHash(nonce, metadata, hexStringToByteArray(rawMsgHex));
    }

    public static String createN3MessageHash(BigInteger nonce, N3MessageMetadataExecDto metadata, byte[] rawMsgBytes) {
        return keccak256Hex(concatenateOp(nonce, metadata, rawMsgBytes));
    }

    public static String createN3MessageHash(BigInteger nonce, N3MessageMetadataStoreOnlyDto metadata,
            String rawMsgHex) {
        return createN3MessageHash(nonce, metadata, hexStringToByteArray(rawMsgHex));
    }

    public static String createN3MessageHash(BigInteger nonce, N3MessageMetadataStoreOnlyDto metadata, byte[] rawMsgBytes) {
        return keccak256Hex(concatenateOp(nonce, metadata, rawMsgBytes));
    }

    public static String createN3MessageHash(BigInteger nonce, N3MessageMetadataResultDto metadata, String rawMsgHex) {
        return createN3MessageHash(nonce, metadata, hexStringToByteArray(rawMsgHex));
    }

    public static String createN3MessageHash(BigInteger nonce, N3MessageMetadataResultDto metadata, byte[] rawMsgBytes) {
        return keccak256Hex(concatenateOp(nonce, metadata, rawMsgBytes));
    }

    public static byte[] concatenateOp(BigInteger nonce, N3MessageMetadataExecDto metadata, byte[] rawMsgBytes) {
        return concatenateOp(nonce, concatenateMetadata(metadata), rawMsgBytes);
    }

    public static byte[] concatenateOp(BigInteger nonce, N3MessageMetadataStoreOnlyDto metadata, byte[] rawMsgBytes) {
        return concatenateOp(nonce, concatenateMetadata(metadata), rawMsgBytes);
    }

    public static byte[] concatenateOp(BigInteger nonce, N3MessageMetadataResultDto metadata, byte[] rawMsgBytes) {
        return concatenateOp(nonce, concatenateMetadata(metadata), rawMsgBytes);
    }

    private static byte[] concatenateOp(BigInteger nonce, byte[] metadataConcat, byte[] rawMsgBytes) {
        byte[] noncePadded = toLittleEndianByteArrayZeroPadded(nonce, UINT256_SIZE);
        return reverseArray(concatenate(concatenate(reverseArray(rawMsgBytes), metadataConcat), noncePadded));
    }

    private static byte[] concatenateMetadata(N3MessageMetadataExecDto metadata) {
        byte[] metadataBase = concatenateMetadataBase(metadata);
        byte storeResultByte = 0;
        if (metadata.storeResult) {
            storeResultByte = 1;
        }
        byte[] storeResultPadded = toLittleEndianByteArrayZeroPadded(storeResultByte, BOOL_SIZE);
        return concatenate(storeResultPadded, metadataBase);
    }

    private static byte[] concatenateMetadata(N3MessageMetadataStoreOnlyDto metadata) {
        return concatenateMetadataBase(metadata);
    }

    private static byte[] concatenateMetadata(N3MessageMetadataResultDto metadata) {
        byte[] metadataBase = concatenateMetadataBase(metadata);
        byte[] relatedMsgNonce = toLittleEndianByteArrayZeroPadded(metadata.relatedMessageNonce, UINT256_SIZE);
        return concatenate(relatedMsgNonce, metadataBase);
    }

    private static byte[] concatenateMetadataBase(N3MessageMetadataDto metadata) {
        byte[] msgTypePadded = toLittleEndianByteArrayZeroPadded(metadata.type, UINT8_SIZE);
        byte[] timestampPadded = toLittleEndianByteArrayZeroPadded(metadata.timestamp, UINT256_SIZE);
        byte[] senderArray = reverseArray(metadata.sender.toArray());
        return concatenate(concatenate(senderArray, timestampPadded), msgTypePadded);
    }

    // endregion
    // region message events

    public static List<N3MessageStoreEvent> getMessageStorEvents(
            Hash256 txHash, Neow3j neow3j, Hash160 bridge)
            throws IOException {
        return neow3j.getApplicationLog(txHash).send().getApplicationLog().getFirstExecution().getNotifications()
                .stream()
                .filter(n -> n.getContract().equals(messageBridge.getScriptHash()) && n.getEventName().equals("Store"))
                .map(MessageHelper::getN3MessageStoreEventFromNotification).collect(Collectors.toList());
    }

    private static N3MessageStoreEvent getN3MessageStoreEventFromNotification(
            Notification n3MessageStoreEvent) {
        return N3MessageStoreEvent.fromNotification(n3MessageStoreEvent);
    }

    // endregion
    // region message event dtos

    public static class N3MessageStoreEvent {
        public BigInteger nonce;
        public String metadataSerializedHex;

        public N3MessageStoreEvent(BigInteger nonce, String metadataSerializedHex) {
            this.nonce = nonce;
            this.metadataSerializedHex = metadataSerializedHex;
        }

        public static N3MessageStoreEvent fromNotification(Notification n3MessageStoreEvent) {
            if (!n3MessageStoreEvent.getContract().equals(messageBridge.getScriptHash()) ||
                    !n3MessageStoreEvent.getEventName().equals("Store")) {
                throw new IllegalArgumentException("Notification is not a Store event.");
            }
            List<StackItem> items = n3MessageStoreEvent.getState().getList();
            BigInteger nonce = items.get(0).getInteger();
            String metadataSerializedHex = items.get(1).getHexString();
            return new N3MessageStoreEvent(nonce, metadataSerializedHex);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof N3MessageStoreEvent)) return false;
            N3MessageStoreEvent that = (N3MessageStoreEvent) o;
            return nonce.equals(that.nonce) &&
                    metadataSerializedHex.equals(that.metadataSerializedHex);
        }

        @Override
        public String toString() {
            return "StoreEvent{" +
                    "nonce=" + nonce +
                    ", metadataHex=" + metadataSerializedHex +
                    "}";
        }
    }

    // endregion

}
