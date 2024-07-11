package network.bane.testhelper;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.contracts.CryptoLib;
import network.bane.lib.TokenBridgeLib;

public class TokenTestContract {

    public static ByteString concatTokenBridgeOpData(Hash160 token, Hash160 neoXToken, int nonce, Hash160 to,
            int value) {
        return TokenBridgeLib.concatTokenBridgeOpData(token, neoXToken, nonce, to, value);
    }

    public static ByteString hashTokenBridge(Hash160 token, Hash160 neoXToken, int nonce, Hash160 to, int value) {
        return TokenBridgeLib.hashTokenBridgeOp(new CryptoLib(), token, neoXToken, nonce, to, value);
    }
}
