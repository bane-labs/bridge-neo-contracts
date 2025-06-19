package network.bane.structs.message;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.annotations.Struct;

@Struct
public class Invocation {
    public Hash160 contract;
    public String method;
    public byte callFlags;
    public Object[] args;

    public Invocation(Hash160 contract, String method, byte callFlags, Object[] args) {
        this.contract = contract;
        this.method = method;
        this.callFlags = callFlags;
        this.args = args;
    }
}
