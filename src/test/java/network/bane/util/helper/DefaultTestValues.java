package network.bane.util.helper;

import io.neow3j.types.Hash160;

import java.math.BigInteger;

public class DefaultTestValues {
    public static final Hash160 MANAGEMENT_CONTRACT_HASH = new Hash160("0x437348b2507d3215c5a7ef5cf01baf616b030ff3");

    public static final BigInteger DEFAULT_GAS_DEPOSIT_FEE = new BigInteger("10000000");
    public static final BigInteger DEFAULT_MIN_GAS_DEPOSIT = new BigInteger("100000000");
    public static final BigInteger DEFAULT_MAX_GAS_DEPOSIT = new BigInteger("1000000000000");
    public static final BigInteger DEFAULT_MAX_WITHDRAWALS = new BigInteger("100");

}
