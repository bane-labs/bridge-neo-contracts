package network.bane.structs;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.annotations.Struct;

@Struct
public class Withdrawal {
    public Integer nonce;
    public Hash160 to;
    public Integer amount;

    public static boolean isValid(Withdrawal w) {
        return w.nonce != null &&
                w.amount != null &&
                w.to != null &&
                Hash160.isValid(w.to) &&
                !w.to.isZero();
    }
}
