package network.bane.lib;

public class Storage {

    // map prefixes
    public static final byte PREFIX_BASE = 0x0a;
    public static final byte PREFIX_GAS_CLAIMABLES = 0x0b;

    public static final int KEY_BRIDGE_MANAGEMENT = 0x01;

    public static final int KEY_GAS_DEPOSIT_FEE = 0x02;
    public static final int KEY_GAS_DEPOSIT_MIN_AMOUNT = 0x03;
    public static final int KEY_GAS_DEPOSIT_MAX_AMOUNT = 0x04;

    public static final int KEY_LOCKED = 0x05;

    public static final int KEY_GAS_DEPOSIT_ROOT = 0x10;
    public static final int KEY_GAS_DEPOSIT_NONCE = 0x11;

    public static final int KEY_GAS_WITHDRAWAL_ROOT = 0x20;
    public static final int KEY_GAS_WITHDRAWAL_NONCE = 0x21;
}
