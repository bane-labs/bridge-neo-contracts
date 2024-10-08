package network.bane.structs;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.annotations.Struct;

@Struct
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

    public static boolean isValid(TokenBridge tokenBridge) {
        return tokenBridge != null &&
                State.isValid(tokenBridge.depositState) &&
                State.isValid(tokenBridge.withdrawalState) &&
                TokenConfig.isValid(tokenBridge.config);
    }

    @Struct
    public static class TokenConfig {
        public Hash160 neoXToken;
        public int decimalScalingFactor;
        public int fee;
        public int minAmount;
        public int maxAmount;
        public int maxWithdrawals;

        public TokenConfig(Hash160 neoXToken, int decimalScalingFactor, int fee, int minAmount, int maxAmount,
                int maxWithdrawals) {
            this.neoXToken = neoXToken;
            this.decimalScalingFactor = decimalScalingFactor;
            this.fee = fee;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.maxWithdrawals = maxWithdrawals;
        }

        public static boolean isValid(TokenConfig config) {
            int minAmount = config.minAmount;
            return config.neoXToken != null &&
                    Hash160.isValid(config.neoXToken) &&
                    !config.neoXToken.isZero() &&
                    config.decimalScalingFactor >= 0 &&
                    config.fee >= 0 &&
                    minAmount >= 0 &&
                    config.maxAmount > minAmount &&
                    config.maxWithdrawals > 0;
        }
    }
}
