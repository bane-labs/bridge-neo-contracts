package network.bane.structs;

import io.neow3j.devpack.annotations.Struct;

@Struct
public class GasConfig {
    public int depositFee;
    public int minAmount;
    public int maxAmount;
    public int maxWithdrawals;

    public GasConfig(int depositFee, int minAmount, int maxAmount, int maxWithdrawals) {
        this.depositFee = depositFee;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.maxWithdrawals = maxWithdrawals;
    }

    public static boolean isValid(GasConfig config) {
        return config != null &&
                config.depositFee >= 0 &&
                config.minAmount >= 0 &&
                config.maxAmount > config.minAmount &&
                config.maxWithdrawals > 0;
    }
}
