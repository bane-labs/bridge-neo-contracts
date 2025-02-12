package network.bane.util.structs;

import io.neow3j.types.Hash160;

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
        public Hash160 tokenHash;
        public int decimalScalingFactor;

        public NativeConfig(BigInteger fee, BigInteger minAmount, BigInteger maxAmount, int maxWithdrawals,
                BigInteger maxTotalDeposit, Hash160 token, int decimalScalingFactor) {
            this.fee = fee;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.maxWithdrawals = maxWithdrawals;
            this.maxTotalDeposit = maxTotalDeposit;
            this.tokenHash = token;
            this.decimalScalingFactor = decimalScalingFactor;
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
                    this.maxTotalDeposit.equals(other.maxTotalDeposit) &&
                    this.tokenHash.equals(other.tokenHash) &&
                    this.decimalScalingFactor == other.decimalScalingFactor;
        }
    }
}
