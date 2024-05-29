package network.bane.structs;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.annotations.Struct;
import network.bane.bridge.ExecutionTypes;

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
        public int fee;
        public int minAmount;
        public int maxAmount;
        public int maxWithdrawals;
        public int executionType;
        public int addDecimals;

        public TokenConfig(Hash160 neoXToken, int fee, int minAmount, int maxAmount, int maxWithdrawals,
                int executionType, int addDecimals) {
            this.neoXToken = neoXToken;
            this.fee = fee;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.maxWithdrawals = maxWithdrawals;
            this.executionType = executionType;
            this.addDecimals = addDecimals;
        }

        public static boolean isValid(TokenConfig config) {
            int minAmount = config.minAmount;
            int executionType = config.executionType;
            return config.neoXToken != null &&
                    config.neoXToken.isZero() &&
                    Hash160.isValid(config.neoXToken) &&
                    config.fee >= 0 &&
                    minAmount >= 0 &&
                    config.maxAmount > minAmount &&
                    config.maxWithdrawals > 0 &&
                    executionType >= 0 &&
                    executionType <= ExecutionTypes.MAX_EXECUTION_TYPE_VALUE;
        }
    }
}
