package network.bane.bridge;

public class BridgeImpl {

    static int getUnclaimedRewards() {
        return BridgeContract.baseMap.getInt(StorageConstants.KEY_UNCLAIMED_REWARDS);
    }

    static void addToUnclaimedRewards(int amount) {
        int currentRewards = BridgeContract.baseMap.getInt(StorageConstants.KEY_UNCLAIMED_REWARDS);
        BridgeContract.baseMap.put(StorageConstants.KEY_UNCLAIMED_REWARDS, currentRewards + amount);
    }

}
