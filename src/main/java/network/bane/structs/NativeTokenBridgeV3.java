package network.bane.structs;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.List;
import io.neow3j.devpack.Map;
import io.neow3j.devpack.annotations.Struct;
import network.bane.lib.BridgeLib;

import static io.neow3j.devpack.Helper.abort;

@Struct
public class NativeTokenBridgeV3 {
    public boolean paused;
    // Note: If a future version of the bridge contract should contain a verify method, the computation of this value
    // should be kept in mind (and adapted), since otherwise the actual balance might differ from this accounting here.
    public int totalDeposited;
    public State depositState;
    public State withdrawalState;
    public NativeTokenConfigV3 config;

    public NativeTokenBridgeV3(boolean paused, Integer totalDeposited, State depositState, State withdrawalState,
            NativeTokenConfigV3 config) {
        this.paused = paused;
        this.totalDeposited = totalDeposited;
        this.depositState = depositState;
        this.withdrawalState = withdrawalState;
        this.config = config;
    }

    public static boolean isValid(NativeTokenBridgeV3 nativeTokenBridge) {
        return nativeTokenBridge != null &&
                nativeTokenBridge.totalDeposited >= 0 &&
                State.isValid(nativeTokenBridge.depositState) &&
                State.isValid(nativeTokenBridge.withdrawalState) &&
                NativeTokenConfigV3.isValid(nativeTokenBridge.config);
    }

    static public class NativeTokenConfigV3 {

        /**
         * The fee that is charged for each deposit.
         */
        public int depositFee;

        /**
         * The minimum amount that can be deposited in a single deposit.
         */
        public int minAmount;

        /**
         * The maximum amount that can be deposited in a single deposit.
         */
        public int maxAmount;

        /**
         * The maximum number of withdrawals that can be batched together by the relayer in a single invocation of
         * {@link network.bane.bridge.BridgeContract#withdrawToken(Hash160, ByteString, Map, List)}.
         */
        public int maxWithdrawals;

        /**
         * The maximum total amount that can be deposited to the bridge contract and thus to Neo X. This value is
         * intended to be aligned with the total amount of Gas funded to the bridge contract on Neo X, or at least never
         * exceed that value, so that no deposits can be made if they cannot be distributed on Neo X.
         * <p>
         * If this value is to be updated, it may not be set lower than the current total (already) deposited amount.
         */
        public int maxTotalDeposited;

        /**
         * The token used for the native bridge.
         */
        public Hash160 nativeToken;

        /**
         * The decimal scaling factor is used to mitigate the difference in decimals between the tokens on the source
         * and the target chain. Since the bridge amount is hashed and the bridge on the target chain needs to verify
         * the amount with the hash, the amount used for hashing must be the same on both chains.
         */
        public int decimalScalingFactor;

        public NativeTokenConfigV3(Hash160 tokenForNativeBridge, int decimalsOnLinkedChain, int depositFee,
                int minAmount, int maxAmount, int maxWithdrawals, int maxTotalDeposited) {

            this.depositFee = depositFee;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.maxWithdrawals = maxWithdrawals;
            this.maxTotalDeposited = maxTotalDeposited;

            this.nativeToken = tokenForNativeBridge;

            if (decimalsOnLinkedChain < 0) abort("Invalid decimals");
            if (decimalsOnLinkedChain > 32) abort("Max decimals supported exceeded");
            this.decimalScalingFactor = BridgeLib.calculateDecimalScalingFactor(tokenForNativeBridge,
                    decimalsOnLinkedChain);
        }

        public static boolean isValid(NativeTokenConfigV3 config) {
            return config != null &&
                    config.depositFee >= 0 &&
                    config.minAmount >= 0 &&
                    config.maxAmount > config.minAmount &&
                    config.maxWithdrawals > 0 &&
                    config.maxTotalDeposited > 0 &&
                    config.maxTotalDeposited > config.maxAmount &&
                    config.nativeToken != null && Hash160.isValid(config.nativeToken) && !config.nativeToken.isZero() &&
                    config.decimalScalingFactor >= 0;
        }
    }
}
