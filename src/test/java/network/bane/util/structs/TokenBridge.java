package network.bane.util.structs;

import io.neow3j.types.Hash160;

import java.math.BigInteger;

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

    public static class TokenConfig {
        public Hash160 neoXTokenHash;
        public BigInteger fee;
        public BigInteger minAmount;
        public BigInteger maxAmount;
        public BigInteger maxWithdrawals;
        public BigInteger executionType;

        public TokenConfig(Hash160 neoXTokenHash, BigInteger fee, BigInteger minAmount, BigInteger maxAmount, BigInteger maxWithdrawals,
                BigInteger executionType) {
            this.neoXTokenHash = neoXTokenHash;
            this.fee = fee;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.maxWithdrawals = maxWithdrawals;
            this.executionType = executionType;
        }
    }
}
