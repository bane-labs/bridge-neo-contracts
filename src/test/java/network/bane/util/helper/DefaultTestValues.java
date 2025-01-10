package network.bane.util.helper;

import io.neow3j.types.Hash160;

import java.math.BigInteger;

public class DefaultTestValues {
    public static final BigInteger DEFAULT_LINKED_CHAIN_ID = BigInteger.valueOf(12345);

    public static final Hash160 MANAGEMENT_CONTRACT_HASH = new Hash160("0x6945b982c603ca8e640b0e13275e0a5edefed90b");

    public static final BigInteger DEFAULT_DEPOSIT_FEE = new BigInteger("10000000");
    public static final BigInteger DEFAULT_MIN_DEPOSIT = new BigInteger("100000000");
    public static final BigInteger DEFAULT_MAX_DEPOSIT = new BigInteger("1000000000000");
    public static final int DEFAULT_MAX_WITHDRAWALS = 100;
    public static final BigInteger DEFAULT_TOTAL_MAX_DEPOSITED_GAS = new BigInteger("10000000000000");

    public static final Hash160 DUMMY_TARGET_CONTRACT_HASH = new Hash160("0x605edab7b33d187e9a22f33cc0722f9c3ccab3b3");
}
