package network.bane.util.helper;

import io.neow3j.types.Hash160;

import java.math.BigInteger;

public class DefaultTestValues {
    public static final Hash160 MANAGEMENT_CONTRACT_HASH = new Hash160("0x7196956a6b09bc3938d395ce1b62f3c8c65d9189");

    public static final BigInteger DEFAULT_GAS_DEPOSIT_FEE = new BigInteger("10000000");
    public static final BigInteger DEFAULT_MIN_GAS_DEPOSIT = new BigInteger("100000000");
    public static final BigInteger DEFAULT_MAX_GAS_DEPOSIT = new BigInteger("1000000000000");
    public static final BigInteger DEFAULT_MAX_WITHDRAWALS = new BigInteger("100");
    public static final BigInteger DEFAULT_TOTAL_MAX_DEPOSITED_GAS = new BigInteger("10000000000000");

}
