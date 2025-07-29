package network.bane.message;

class StorageConstants {

    // map prefixes
    static final byte PREFIX_BASE = 0x0a;
    static final byte PREFIX_MSG_MESSAGES = 0x2a;
    static final byte PREFIX_MSG_EXECUTABLE_STATE = 0x2b;
    static final byte PREFIX_MSG_RESULT = 0x2c;

    static final int KEY_BRIDGE_MANAGEMENT = 0x01;
    static final int KEY_PAUSE = 0x02;

    static final int KEY_UNCLAIMED_FEES = 0x04;
    static final int KEY_SENDING_PAUSE = 0x05;
    static final int KEY_EXECUTING_PAUSE = 0x06;

    static final int KEY_LINKED_CHAIN_ID = 0x10;

    static final int KEY_MESSAGE_BRIDGE = 0x20;

    static final int KEY_ENTERED = 0x70;

    // key used to restrict migrate from being called multiple times.
    static final int KEY_VERSION = 0x7f;

}
