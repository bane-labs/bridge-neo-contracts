package network.bane.dto.bridge;

import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import network.bane.dto.State;

import java.math.BigInteger;
import java.util.List;

import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;

public class TokenBridge {
    public boolean paused;
    public State depositState;
    public State withdrawalState;
    public TokenConfig config;

    public TokenBridge(boolean paused, State depositState, State withdrawalState, TokenConfig config) {
        this.paused = paused;
        this.depositState = depositState;
        this.withdrawalState = withdrawalState;
        this.config = config;
    }

    public static TokenBridge fromStackItem(StackItem item) {
        List<StackItem> list = item.getList();
        boolean paused = list.get(0).getBoolean();
        State depositState = State.fromStackItem(list.get(1));
        State withdrawalState = State.fromStackItem(list.get(2));
        TokenConfig config = TokenConfig.fromStackItem(list.get(3));
        return new TokenBridge(paused, depositState, withdrawalState, config);
    }

    public static ContractParameter getAsContractParameter(TokenConfig config) {
        return array(
                hash160(config.tokenOnDestination),
                integer(config.fee),
                integer(config.minAmount),
                integer(config.maxAmount),
                integer(config.maxWithdrawals),
                integer(config.decimalScalingFactor)
        );
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenBridge that = (TokenBridge) o;
        return paused == that.paused &&
                depositState.equals(that.depositState) &&
                withdrawalState.equals(that.withdrawalState) &&
                config.equals(that.config);
    }

    public static class TokenConfig {
        public Hash160 tokenOnDestination;
        public BigInteger fee;
        public BigInteger minAmount;
        public BigInteger maxAmount;
        public int maxWithdrawals;
        public int decimalScalingFactor;

        public TokenConfig(Hash160 tokenOnDestination, BigInteger fee, BigInteger minAmount, BigInteger maxAmount,
                int maxWithdrawals, int decimalScalingFactor) {
            this.tokenOnDestination = tokenOnDestination;
            this.fee = fee;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.maxWithdrawals = maxWithdrawals;
            this.decimalScalingFactor = decimalScalingFactor;
        }

        public static TokenConfig fromStackItem(StackItem item) {
            List<StackItem> list = item.getList();
            Hash160 tokenOnDestination = Hash160.fromAddress(list.get(0).getAddress());
            BigInteger depositFee = list.get(1).getInteger();
            BigInteger minAmount = list.get(2).getInteger();
            BigInteger maxAmount = list.get(3).getInteger();
            int maxWithdrawals = list.get(4).getInteger().intValue();
            int decimalScalingFactor = list.get(5).getInteger().intValue();
            return new TokenConfig(tokenOnDestination, depositFee, minAmount, maxAmount, maxWithdrawals,
                    decimalScalingFactor);
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TokenConfig that = (TokenConfig) o;
            return tokenOnDestination.equals(that.tokenOnDestination) &&
                    fee.equals(that.fee) &&
                    minAmount.equals(that.minAmount) &&
                    maxAmount.equals(that.maxAmount) &&
                    maxWithdrawals == that.maxWithdrawals &&
                    decimalScalingFactor == that.decimalScalingFactor;
        }

        @Override
        public String toString() {
            return "TokenConfig{" +
                    "tokenOnDestination=" + tokenOnDestination +
                    ", fee=" + fee +
                    ", minAmount=" + minAmount +
                    ", maxAmount=" + maxAmount +
                    ", maxWithdrawals=" + maxWithdrawals +
                    ", decimalScalingFactor=" + decimalScalingFactor +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "TokenBridge{" +
                "paused=" + paused +
                ", depositState=" + depositState +
                ", withdrawalState=" + withdrawalState +
                ", config=" + config +
                '}';
    }

}
