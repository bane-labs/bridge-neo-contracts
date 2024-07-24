package network.bane.util.helper;

import io.neow3j.types.Hash160;

import java.math.BigInteger;

public class DefaultTestValues {
    public static final Hash160 MANAGEMENT_CONTRACT_HASH = new Hash160("0xc0c708adc241f96252ec2b8366926ea660f15ba8");

    public static final BigInteger DEFAULT_GAS_DEPOSIT_FEE = new BigInteger("10000000");
    public static final BigInteger DEFAULT_MIN_GAS_DEPOSIT = new BigInteger("100000000");
    public static final BigInteger DEFAULT_MAX_GAS_DEPOSIT = new BigInteger("1000000000000");
    public static final BigInteger DEFAULT_MAX_WITHDRAWALS = new BigInteger("100");
    public static final BigInteger DEFAULT_TOTAL_MAX_DEPOSITED_GAS = new BigInteger("10000000000000");

}
