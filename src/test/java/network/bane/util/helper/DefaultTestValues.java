package network.bane.util.helper;

import io.neow3j.contract.GasToken;
import io.neow3j.types.Hash160;

import java.math.BigInteger;

public class DefaultTestValues {
    public static final BigInteger DEFAULT_LINKED_CHAIN_ID = BigInteger.valueOf(12345);

    public static final Hash160 MANAGEMENT_CONTRACT_HASH = new Hash160("0xfbfbc3b7fcab6eeb6d1f180ac624fc413f1cbcbe");

    public static final BigInteger DEFAULT_DEPOSIT_FEE = new BigInteger("10000000");
    public static final BigInteger DEFAULT_MIN_DEPOSIT = new BigInteger("100000000");
    public static final BigInteger DEFAULT_MAX_DEPOSIT = new BigInteger("1000000000000");
    public static final int DEFAULT_MAX_WITHDRAWALS = 100;
    public static final BigInteger DEFAULT_TOTAL_MAX_DEPOSITED_NATIVE = new BigInteger("10000000000000");
    public static final Hash160 DEFAULT_NATIVE_TOKEN_HASH = GasToken.SCRIPT_HASH;
    public static final int DEFAULT_NATIVE_DECIMAL_SCALING_FACTOR = 0;

    public static final Hash160 DUMMY_TARGET_CONTRACT_HASH = new Hash160("0x605edab7b33d187e9a22f33cc0722f9c3ccab3b3");
}
