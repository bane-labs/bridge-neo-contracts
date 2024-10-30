package network.bane.bridge;

import static io.neow3j.devpack.Helper.abort;

public class BridgeImpl {

    static int getUnclaimedRewards() {
        return BridgeContract.baseMap.getInt(StorageConstants.KEY_UNCLAIMED_REWARDS);
    }

    static void addToUnclaimedRewards(int amount) {
        int currentRewards = getUnclaimedRewards();
        BridgeContract.baseMap.put(StorageConstants.KEY_UNCLAIMED_REWARDS, currentRewards + amount);
    }

    public static int getNeoHoldingGasRewards() {
        return BridgeContract.baseMap.getInt(StorageConstants.KEY_NEO_HOLDING_GAS_REWARDS);
    }

    static void addNeoHoldingGasRewards(int amount) {
        int currentRewards = getNeoHoldingGasRewards();
        BridgeContract.baseMap.put(StorageConstants.KEY_NEO_HOLDING_GAS_REWARDS, currentRewards + amount);
    }

    private static boolean entered() {
        return BridgeContract.baseMap.getBoolean(StorageConstants.KEY_ENTERED);
    }

    static void enteringNonReentrant() {
        if (entered()) abort("Reentrancy detected.");
        BridgeContract.baseMap.put(StorageConstants.KEY_ENTERED, true);
    }

    static void exitingNonReentrant() {
        BridgeContract.baseMap.put(StorageConstants.KEY_ENTERED, false);
    }
}
