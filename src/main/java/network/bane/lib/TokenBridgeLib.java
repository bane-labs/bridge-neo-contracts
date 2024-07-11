package network.bane.lib;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.List;
import io.neow3j.devpack.contracts.CryptoLib;
import network.bane.structs.Withdrawal;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Helper.concat;
import static io.neow3j.devpack.Helper.reverse;
import static io.neow3j.devpack.Helper.toByteArray;
import static network.bane.lib.BridgeLib.UINT256_SIZE;
import static network.bane.lib.BridgeLib.computeNewRoot;
import static network.bane.lib.BridgeLib.padToBytes;

public class TokenBridgeLib {

    public static ByteString hashTokenBridgeOp(CryptoLib cryptoLib, Hash160 token, Hash160 neoXToken, int nonce,
            Hash160 to, int amount) {
        return cryptoLib.sha256(concatTokenBridgeOpData(token, neoXToken, nonce, to, amount));
    }

    public static ByteString concatTokenBridgeOpData(Hash160 token, Hash160 neoXToken, int nonce, Hash160 to,
            int value) {
        byte[] nonceP = padToBytes(toByteArray(nonce), UINT256_SIZE);
        byte[] valueP = padToBytes(toByteArray(value), UINT256_SIZE);
        byte[] concatenated = concat(concat(concat(
                                concat(valueP, to.toByteString()),
                                nonceP),
                        neoXToken.toByteString()),
                token.toByteString());
        reverse(concatenated);
        return new ByteString(concatenated);
    }

    public static ByteString computeNewTopRoot(CryptoLib cryptoLib, ByteString formerRoot, Hash160 token,
            Hash160 neoXToken, List<Withdrawal> withdrawals) {
        ByteString parent = formerRoot;
        for (int i = 0; i < withdrawals.size(); i++) {
            Withdrawal withdrawal = withdrawals.get(i);
            if (!Withdrawal.isValid(withdrawal)) abort("Invalid withdrawal provided.");
            ByteString withdrawalHash = hashTokenBridgeOp(cryptoLib, token, neoXToken, withdrawal.nonce, withdrawal.to,
                    withdrawal.amount);
            parent = computeNewRoot(cryptoLib, parent, withdrawalHash);
        }
        return parent;
    }

}
