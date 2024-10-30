package network.bane.scripts;

import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;

import java.math.BigInteger;

class TokenBridge {
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

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenBridge that = (TokenBridge) o;
        return paused == that.paused &&
                depositState.equals(that.depositState) &&
                withdrawalState.equals(that.withdrawalState) &&
                config.equals(that.config);
    }

    static class TokenConfig {
        public Hash160 neoXTokenHash;
        public BigInteger fee;
        public BigInteger minAmount;
        public BigInteger maxAmount;
        public int maxWithdrawals;
        public int decimalScalingFactor;

        public TokenConfig(Hash160 neoXTokenHash, BigInteger fee, BigInteger minAmount, BigInteger maxAmount,
                int maxWithdrawals, int decimalScalingFactor) {
            this.neoXTokenHash = neoXTokenHash;
            this.fee = fee;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.maxWithdrawals = maxWithdrawals;
            this.decimalScalingFactor = decimalScalingFactor;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TokenConfig that = (TokenConfig) o;
            return neoXTokenHash.equals(that.neoXTokenHash) &&
                    fee.equals(that.fee) &&
                    minAmount.equals(that.minAmount) &&
                    maxAmount.equals(that.maxAmount) &&
                    maxWithdrawals == that.maxWithdrawals &&
                    decimalScalingFactor == that.decimalScalingFactor;
        }

        @Override
        public String toString() {
            return "TokenConfig{" +
                    "neoXTokenHash=" + neoXTokenHash +
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
    static class State {
        public BigInteger nonce;
        public Hash256 root;

        public State(BigInteger nonce, Hash256 root) {
            this.nonce = nonce;
            this.root = root;
        }

        public static State newState() {
            return new State(BigInteger.ZERO, Hash256.ZERO);
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            State state = (State) o;
            return nonce.equals(state.nonce) &&
                    root.equals(state.root);
        }

        @Override
        public String toString() {
            return "State{" +
                    "nonce=" + nonce +
                    ", root=" + root +
                    '}';
        }
    }
}