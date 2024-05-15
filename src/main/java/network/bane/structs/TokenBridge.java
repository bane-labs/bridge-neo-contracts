package network.bane.structs;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.annotations.Struct;
import network.bane.bridge.TokenTypeConstants;

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
        public Hash160 neoXTokenHash;
        public int fee;
        public int minAmount;
        public int maxAmount;
        public int maxWithdrawals;
        public int tokenType;
        public int addDecimals;

        public TokenConfig(Hash160 neoXTokenHash, int fee, int minAmount, int maxAmount, int maxWithdrawals,
                int tokenType, int addDecimals) {
            this.neoXTokenHash = neoXTokenHash;
            this.fee = fee;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.maxWithdrawals = maxWithdrawals;
            this.tokenType = tokenType;
            this.addDecimals = addDecimals;
        }

        public static boolean isValid(TokenConfig config) {
            int minAmount = config.minAmount;
            int tokenType = config.tokenType;
            return config.neoXTokenHash != null &&
                    config.neoXTokenHash.isZero() &&
                    Hash160.isValid(config.neoXTokenHash) &&
                    config.fee >= 0 &&
                    minAmount >= 0 &&
                    config.maxAmount > minAmount &&
                    config.maxWithdrawals > 0 &&
                    tokenType >= 0 &&
                    tokenType <= TokenTypeConstants.MAX_TOKEN_TYPE_INT_VALUE;
        }
    }
}
