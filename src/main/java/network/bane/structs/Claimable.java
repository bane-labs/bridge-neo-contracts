package network.bane.structs;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.annotations.Struct;

@Struct
public class Claimable {
    public Hash160 to;
    public int amount;

    public Claimable(Hash160 to, int amount) {
        this.to = to;
        this.amount = amount;
    }
}
