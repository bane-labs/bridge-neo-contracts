package network.bane.utils.structs;

import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.types.Hash160;

import java.math.BigInteger;
import java.util.List;

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

    public static NativeBridge fromStackItem(StackItem item) {
        List<StackItem> list = item.getList();
        boolean paused = list.get(0).getBoolean();
        BigInteger totalDeposited = list.get(1).getInteger();
        State depositState = State.fromStackItem(list.get(2));
        State withdrawalState = State.fromStackItem(list.get(3));
        NativeConfig config = NativeConfig.fromStackItem(list.get(4));
        return new NativeBridge(paused, totalDeposited, depositState, withdrawalState, config);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NativeBridge that = (NativeBridge) o;
        return paused == that.paused &&
                totalDeposited.equals(that.totalDeposited) &&
                depositState.equals(that.depositState) &&
                withdrawalState.equals(that.withdrawalState) &&
                config.equals(that.config);
    }

    public static class NativeConfig {
        public BigInteger fee;
        public BigInteger minAmount;
        public BigInteger maxAmount;
        public int maxWithdrawals;
        public BigInteger maxTotalDeposited;
        public Hash160 nativeToken;
        public int decimalScalingFactor;

        public NativeConfig(BigInteger fee, BigInteger minAmount, BigInteger maxAmount, int maxWithdrawals,
                BigInteger maxTotalDeposited, Hash160 nativeToken, int decimalScalingFactor) {
            this.fee = fee;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.maxWithdrawals = maxWithdrawals;
            this.maxTotalDeposited = maxTotalDeposited;
            this.nativeToken = nativeToken;
            this.decimalScalingFactor = decimalScalingFactor;
        }

        public static NativeConfig fromStackItem(StackItem item) {
            List<StackItem> list = item.getList();
            BigInteger depositFee = list.get(0).getInteger();
            BigInteger minAmount = list.get(1).getInteger();
            BigInteger maxAmount = list.get(2).getInteger();
            int maxWithdrawals = list.get(3).getInteger().intValue();
            BigInteger maxTotalDeposited = list.get(4).getInteger();
            Hash160 nativeToken = Hash160.fromAddress(list.get(5).getAddress());
            int decimalScalingFactor = list.get(6).getInteger().intValue();
            return new NativeConfig(depositFee, minAmount, maxAmount, maxWithdrawals, maxTotalDeposited, nativeToken,
                    decimalScalingFactor);
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NativeConfig that = (NativeConfig) o;
            return fee.equals(that.fee) &&
                    minAmount.equals(that.minAmount) &&
                    maxAmount.equals(that.maxAmount) &&
                    maxWithdrawals == that.maxWithdrawals &&
                    maxTotalDeposited.equals(that.maxTotalDeposited) &&
                    nativeToken.equals(that.nativeToken) &&
                    decimalScalingFactor == that.decimalScalingFactor;
        }

        @Override
        public String toString() {
            return "NativeConfig{" +
                    "depositFee=" + fee +
                    ", minAmount=" + minAmount +
                    ", maxAmount=" + maxAmount +
                    ", maxWithdrawals=" + maxWithdrawals +
                    ", maxTotalDeposited=" + maxTotalDeposited +
                    ", nativeToken=" + nativeToken +
                    ", decimalScalingFactor=" + decimalScalingFactor +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "NativeBridge{" +
                "paused=" + paused +
                ", totalDeposited=" + totalDeposited +
                ", depositState=" + depositState +
                ", withdrawalState=" + withdrawalState +
                ", config=" + config +
                '}';
    }
}
