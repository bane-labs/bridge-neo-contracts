package network.bane.util.helper;

import io.neow3j.types.Hash160;

import java.math.BigInteger;

public class DefaultTestValues {
    public static final BigInteger DEFAULT_LINKED_CHAIN_ID = BigInteger.valueOf(12345);

    public static final Hash160 MANAGEMENT_CONTRACT_HASH = new Hash160("0x74eace325d73d7fa70c6b8772fabaac20a632706");
    public static final Hash160 MESSAGE_BRIDGE_CONTRACT_HASH =
            new Hash160("0xaa69a3d968239e8081e015680cdcbf7ef24326b5");
    public static final Hash160 EXECUTION_MANAGER_CONTRACT_HASH =
            new Hash160("0xd9af41a90950d2399dc2543b81368642ddd63761");
    public static final Hash160 DUMMY_EXEC_MANAGER = new Hash160("0x5cd87a79046523454325a77827006ebfae27e05f");

    public static final BigInteger DEFAULT_DEPOSIT_FEE = new BigInteger("10000000");
    public static final BigInteger DEFAULT_MIN_DEPOSIT_GAS = new BigInteger("100000000");
    public static final BigInteger DEFAULT_MAX_DEPOSIT_GAS = new BigInteger("1000000000000");
    public static final int DEFAULT_MAX_WITHDRAWALS = 100;
    public static final BigInteger DEFAULT_TOTAL_MAX_DEPOSITED_GAS = new BigInteger("10000000000000");
    public static final int DEFAULT_DECIMAL_SCALING_FACTOR_GAS = 0;

    public static final Hash160 DUMMY_TARGET_CONTRACT_HASH = new Hash160("0x605edab7b33d187e9a22f33cc0722f9c3ccab3b3");
}
