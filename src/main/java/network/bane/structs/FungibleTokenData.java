package network.bane.structs;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.annotations.Struct;

@Struct
public class FungibleTokenData {
    public Hash160 to;
    public int minGasLimit;
    public Object extraData;

    public FungibleTokenData(Hash160 to, int minGasLimit, Object extraData) {
        this.to = to;
        this.minGasLimit = minGasLimit;
        this.extraData = extraData;
    }

    public boolean isValid() {
        return Hash160.isValid(to) && minGasLimit >= 0;
        // todo: define extraData format and add corresponding checks
    }

}
