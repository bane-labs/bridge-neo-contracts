package network.bane.structs;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.annotations.Struct;
import network.bane.bridge.BridgeContract;

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

        /**
         * The hash of the corresponding token on Neo X.
         */
        public Hash160 neoXToken;

        /**
         * The fee (in GAS) that is charged for a deposit.
         */
        public int fee;

        /**
         * The minimum amount of tokens that can be deposited in one transaction.
         */
        public int minAmount;

        /**
         * The maximum amount of tokens that can be deposited in one transaction.
         */
        public int maxAmount;

        /**
         * The maximum number of withdrawals that can be made in one transaction by the relayer. Additionally, this
         * value in combination with blocks is used as a consensus for validators to know which root they need to sign.
         */
        public int maxWithdrawals;

        /**
         * The decimal scaling factor is used to mitigate the difference in decimals between the tokens on the
         * source and the target chain. Since the bridge amount is hashed and the bridge on the target chain
         * needs to verify the amount with the hash, the amount used for hashing must be the same on both chains.
         * Therefore, this bridge uses the lower number of decimals as the common denominator. Hence, the following
         * rules apply:
         * <ul>
         *     <li> if the decimals of the tokens are the same on both chains, the factors on both chains should
         *     be 0. </li>
         *     <li> if the decimals are different, the difference in decimals (positive) should be the decimal
         *     scaling factor on the chain where the token has more decimals. </li>
         * </ul>
         */
        public int decimalScalingFactor;

        public TokenConfig(Hash160 neoXToken, int fee, int minAmount, int maxAmount, int maxWithdrawals,
                int decimalScalingFactor) {
            this.neoXToken = neoXToken;
            this.fee = fee;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.maxWithdrawals = maxWithdrawals;
            this.decimalScalingFactor = decimalScalingFactor;
        }

        public static boolean isValid(TokenConfig config) {
            int minAmount = config.minAmount;
            return config.neoXToken != null &&
                    Hash160.isValid(config.neoXToken) &&
                    !config.neoXToken.isZero() &&
                    config.fee >= 0 &&
                    minAmount >= 0 &&
                    config.maxAmount > minAmount &&
                    config.maxAmount <= BridgeContract.MAX_TRANSFER_AMOUNT_LIMIT &&
                    config.maxWithdrawals > 0 &&
                    config.decimalScalingFactor >= 0;
        }
    }
}
