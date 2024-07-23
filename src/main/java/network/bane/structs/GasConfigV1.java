package network.bane.structs;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.List;
import io.neow3j.devpack.Map;
import io.neow3j.devpack.annotations.Struct;

@Struct
public class GasConfigV1 {

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

    public GasConfigV1(int depositFee, int minAmount, int maxAmount, int maxWithdrawals) {
        this.depositFee = depositFee;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.maxWithdrawals = maxWithdrawals;
    }

    public static boolean isValid(GasConfigV1 config) {
        return config != null &&
                config.depositFee >= 0 &&
                config.minAmount >= 0 &&
                config.maxAmount > config.minAmount &&
                config.maxWithdrawals > 0;
    }

}
