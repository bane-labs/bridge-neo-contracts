package network.bane.structs;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.annotations.Struct;

@Struct
public class GasBridgePaymentData {
    public Hash160 to;
    public int minBridgeAmount;

    public static boolean isValid(GasBridgePaymentData data) {
        return data.to != null && Hash160.isValid(data.to) && !data.to.isZero() &&
                data.minBridgeAmount > 0;
    }
}
