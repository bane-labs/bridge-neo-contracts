package network.bane.structs.message;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.annotations.Struct;

@Struct
public class N3MethodCall {
    public Hash160 target;
    public String method;
    public byte callFlags;
    public Object[] args;

    public N3MethodCall(Hash160 target, String method, byte callFlags, Object[] args) {
        this.target = target;
        this.method = method;
        this.callFlags = callFlags;
        this.args = args;
    }

    public static boolean isValid(N3MethodCall call) {
        if (call == null) {
            return false;
        }
        boolean targetIsValid = call.target != null && Hash160.isValid(call.target) && !call.target.isZero();
        // Note: String#length() works while String#isEmpty() does not work in the devpack as it embeds additional
        // logic rather than just getting the size of the string.
        boolean methodIsValid = call.method != null && call.method.length() > 0;
        return targetIsValid && methodIsValid;
    }
}
