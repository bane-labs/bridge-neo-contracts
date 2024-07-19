package network.bane.bridge;

public class StorageConstants {

    // map prefixes
    public static final byte PREFIX_BASE = 0x0a;
    public static final byte PREFIX_GAS_CLAIMABLES = 0x0b;
    public static final byte PREFIX_TOKEN_BRIDGES = 0x1a;
    public static final byte PREFIX_TOKEN_CLAIMABLES = 0x1b;

    public static final int KEY_BRIDGE_MANAGEMENT = 0x01;
    public static final int KEY_BRIDGE_PAUSE = 0x02;
    public static final int KEY_GAS_BRIDGE = 0x03;
    public static final int KEY_UNCLAIMED_REWARDS = 0x04;

    // key used to restrict migrate from being called multiple times.
    public static final int KEY_MIGRATED = 0xff;
}
