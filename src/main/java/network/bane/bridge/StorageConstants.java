package network.bane.bridge;

public class StorageConstants {

    // map prefixes
    static final byte PREFIX_BASE = 0x0a;
    static final byte PREFIX_NATIVE_CLAIMABLES = 0x0b;
    static final byte PREFIX_TOKEN_BRIDGES = 0x1a;
    static final byte PREFIX_TOKEN_CLAIMABLES = 0x1b;
    static final byte PREFIX_MSG_MESSAGES = 0x2a;
    static final byte PREFIX_MSG_EXECUTED = 0x2b;

    static final int KEY_BRIDGE_MANAGEMENT = 0x01;
    static final int KEY_BRIDGE_PAUSE = 0x02;
    static final int KEY_NATIVE_BRIDGE = 0x03;
    static final int KEY_UNCLAIMED_REWARDS = 0x04;
    static final int KEY_DEPOSIT_PAUSE = 0x05;
    static final int KEY_NEO_HOLDING_GAS_REWARDS = 0x06;

    static final int KEY_LINKED_CHAIN_ID = 0x10;

    static final int KEY_MESSAGE_BRIDGE = 0x20;

    static final int KEY_ENTERED = 0x70;

    // key used to restrict migrate from being called multiple times.
    static final int KEY_VERSION = 0x7f;
}
