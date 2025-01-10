package network.bane.util.structs;

import java.math.BigInteger;

public class NativeBridge {
    public boolean paused;
    public BigInteger totalDeposited;
    public State depositState;
    public State withdrawalState;
    public NativeConfig config;

    public NativeBridge(boolean paused, BigInteger totalDeposited, State depositState, State withdrawalState,
            NativeConfig config) {
        this.paused = paused;
        this.totalDeposited = totalDeposited;
        this.depositState = depositState;
        this.withdrawalState = withdrawalState;
        this.config = config;
    }

    public boolean equals(NativeBridge other) {
        if (other == null) {
            return false;
        }
        if (this == other) {
            return true;
        }
        return this.paused == other.paused &&
                this.totalDeposited.equals(other.totalDeposited) &&
                this.depositState.equals(other.depositState) &&
                this.withdrawalState.equals(other.withdrawalState) &&
                this.config.equals(other.config);
    }

    public static class NativeConfig {
        public BigInteger fee;
        public BigInteger minAmount;
        public BigInteger maxAmount;
        public Integer maxWithdrawals;
        public BigInteger maxTotalDeposit;

        public NativeConfig(BigInteger fee, BigInteger minAmount, BigInteger maxAmount, int maxWithdrawals,
                BigInteger maxTotalDeposit) {
            this.fee = fee;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.maxWithdrawals = maxWithdrawals;
            this.maxTotalDeposit = maxTotalDeposit;
        }

        public boolean equals(NativeConfig other) {
            if (other == null) {
                return false;
            }
            if (this == other) {
                return true;
            }
            return this.fee.equals(other.fee) &&
                    this.minAmount.equals(other.minAmount) &&
                    this.maxAmount.equals(other.maxAmount) &&
                    this.maxWithdrawals.equals(other.maxWithdrawals) &&
                    this.maxTotalDeposit.equals(other.maxTotalDeposit);
        }
    }
}
