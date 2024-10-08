package network.bane.util.structs;

import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;

import java.math.BigInteger;

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

    public static ContractParameter getAsContractParameter(TokenConfig config) {
        return array(
                hash160(config.neoXTokenHash),
                integer(config.decimalScalingFactor),
                integer(config.fee),
                integer(config.minAmount),
                integer(config.maxAmount),
                integer(config.maxWithdrawals)
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
        public Hash160 neoXTokenHash;
        public int decimalScalingFactor;
        public BigInteger fee;
        public BigInteger minAmount;
        public BigInteger maxAmount;
        public int maxWithdrawals;

        public TokenConfig(Hash160 neoXTokenHash, int decimalScalingFactor, BigInteger fee, BigInteger minAmount,
                BigInteger maxAmount, int maxWithdrawals) {
            this.neoXTokenHash = neoXTokenHash;
            this.decimalScalingFactor = decimalScalingFactor;
            this.fee = fee;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.maxWithdrawals = maxWithdrawals;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TokenConfig that = (TokenConfig) o;
            return neoXTokenHash.equals(that.neoXTokenHash) &&
                    decimalScalingFactor == that.decimalScalingFactor &&
                    fee.equals(that.fee) &&
                    minAmount.equals(that.minAmount) &&
                    maxAmount.equals(that.maxAmount) &&
                    maxWithdrawals == that.maxWithdrawals;
        }
    }
}
