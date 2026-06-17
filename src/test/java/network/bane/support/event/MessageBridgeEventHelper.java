package network.bane.support.event;

import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.Notification;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class MessageBridgeEventHelper {

    // region message events

    public static List<N3MessageStoreEvent> getMessageStorEvents(Hash256 txHash, Neow3j neow3j, Hash160 contract)
            throws IOException {
        return neow3j.getApplicationLog(txHash).send().getApplicationLog().getFirstExecution().getNotifications()
                .stream()
                .filter(n -> n.getContract().equals(contract) && n.getEventName().equals("Store"))
                .map(n -> getN3MessageStoreEventFromNotification(n, contract)).collect(toList());
    }

    private static N3MessageStoreEvent getN3MessageStoreEventFromNotification(Notification n3MessageStoreEvent,
            Hash160 contract) {
        return N3MessageStoreEvent.fromNotification(n3MessageStoreEvent, contract);
    }

    // endregion
    // region message event DTOs

    public static class N3MessageStoreEvent {
        public BigInteger nonce;
        public String metadataSerializedHex;

        public N3MessageStoreEvent(BigInteger nonce, String metadataSerializedHex) {
            this.nonce = nonce;
            this.metadataSerializedHex = metadataSerializedHex;
        }

        public static N3MessageStoreEvent fromNotification(Notification n3MessageStoreEvent, Hash160 contract) {
            if (!n3MessageStoreEvent.getContract().equals(contract) ||
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
