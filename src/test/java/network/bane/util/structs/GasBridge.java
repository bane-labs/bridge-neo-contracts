package network.bane.util.structs;

import java.math.BigInteger;

public class GasBridge {
    boolean paused;
    public State depositState;
    public State withdrawalState;
    public GasConfig config;

    public GasBridge(boolean paused, State depositState, State withdrawalState, GasConfig config) {
        this.paused = paused;
        this.depositState = depositState;
        this.withdrawalState = withdrawalState;
        this.config = config;
    }

    public boolean equals(GasBridge other) {
        if (other == null) {
            return false;
        }
        if (this == other) {
            return true;
        }
        return this.paused == other.paused &&
                this.depositState.equals(other.depositState) &&
                this.withdrawalState.equals(other.withdrawalState) &&
                this.config.equals(other.config);
    }

    public static class GasConfig {
        public BigInteger fee;
        public BigInteger minAmount;
        public BigInteger maxAmount;
        public BigInteger maxWithdrawals;

        public GasConfig(BigInteger fee, BigInteger minAmount, BigInteger maxAmount, BigInteger maxWithdrawals) {
            this.fee = fee;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.maxWithdrawals = maxWithdrawals;
        }

        public boolean equals(GasConfig other) {
            if (other == null) {
                return false;
            }
            if (this == other) {
                return true;
            }
            return this.fee.equals(other.fee) &&
                    this.minAmount.equals(other.minAmount) &&
                    this.maxAmount.equals(other.maxAmount) &&
                    this.maxWithdrawals.equals(other.maxWithdrawals);
        }
    }
}
