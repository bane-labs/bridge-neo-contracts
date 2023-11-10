package network.bane.structs;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.annotations.Struct;

@Struct
public class WithdrawalWithProof {
    public Hash160 to;
    public Integer amount;
    public Integer nonce;
    public Integer path;
    public ByteString[] proof;

    public static boolean isValid(WithdrawalWithProof w) {
        return w.path != null &&
                w.proof != null &&
                w.nonce >= 0 &&
                w.amount > 0 &&
                Hash160.isValid(w.to);
    }

}
