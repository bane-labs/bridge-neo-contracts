package network.bane.structs;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.annotations.Struct;

@Struct
public class TokenInfo {
    public String name;
    public Hash160 remoteToken;
    public int minDeposit;

    public TokenInfo(String name, Hash160 remoteToken, int minDeposit) {
        this.name = name;
        this.remoteToken = remoteToken;
        this.minDeposit = minDeposit;
    }

    public boolean isValid() {
        return name != null &&
                Hash160.isValid(remoteToken) &&
                minDeposit >= 0;
    }
}
