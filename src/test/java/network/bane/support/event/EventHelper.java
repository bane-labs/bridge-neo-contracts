package network.bane.support.event;

import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.Notification;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;

import java.io.IOException;
import java.util.List;

public class EventHelper {

    private static List<Notification> getEvents(Hash256 txHash, Neow3j neow3j) throws IOException {
        return neow3j.getApplicationLog(txHash).send().getApplicationLog().getFirstExecution().getNotifications();
    }

    public static boolean hasFiredEvent(Neow3j neow3j, Hash256 txHash, Hash160 contract, String eventName,
            StackItem state) throws IOException {
        return getEvents(txHash, neow3j).stream().filter(e -> e.getEventName().equals(eventName))
                .filter(e -> e.getContract().equals(contract)).anyMatch(e -> e.getState().equals(state));
    }

}
