package network.bane.bridge;

import static io.neow3j.devpack.Helper.abort;
import static network.bane.bridge.StorageConstants.KEY_ENTERED;

public class BridgeImpl {

    static int getUnclaimedRewards() {
        return BridgeContract.baseMap.getInt(StorageConstants.KEY_UNCLAIMED_REWARDS);
    }

    static void addToUnclaimedRewards(int amount) {
        int currentRewards = BridgeContract.baseMap.getInt(StorageConstants.KEY_UNCLAIMED_REWARDS);
        BridgeContract.baseMap.put(StorageConstants.KEY_UNCLAIMED_REWARDS, currentRewards + amount);
    }

    private static boolean entered() {
        return BridgeContract.baseMap.getBoolean(KEY_ENTERED);
    }

    static void enteringNonReentrant() {
        if (entered()) abort("Reentrancy detected.");
        BridgeContract.baseMap.put(KEY_ENTERED, true);
    }

    static void exiting() {
        BridgeContract.baseMap.put(KEY_ENTERED, false);
    }

}
