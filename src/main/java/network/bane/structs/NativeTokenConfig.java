package network.bane.structs;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.List;
import io.neow3j.devpack.Map;
import io.neow3j.devpack.annotations.Struct;

@Struct
public class NativeTokenConfig {

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

    public NativeTokenConfig(int depositFee, int minAmount, int maxAmount, int maxWithdrawals, int maxTotalDeposited) {
        this.depositFee = depositFee;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.maxWithdrawals = maxWithdrawals;
        this.maxTotalDeposited = maxTotalDeposited;
    }

    public static boolean isValid(NativeTokenConfig config) {
        return config != null &&
                config.depositFee >= 0 &&
                config.minAmount >= 0 &&
                config.maxAmount > config.minAmount &&
                config.maxWithdrawals > 0 &&
                config.maxTotalDeposited > 0 &&
                config.maxTotalDeposited > config.maxAmount;
    }
}
