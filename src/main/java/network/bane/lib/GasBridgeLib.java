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
import static network.bane.TestPaddingContract.padToBytes;
import static network.bane.lib.BridgeLib.UINT256_SIZE;
import static network.bane.lib.BridgeLib.computeNewRoot;

public class GasBridgeLib {

    public static ByteString hashGasBridgeOp(CryptoLib cryptoLib, int nonce, int amount, Hash160 to) {
        return cryptoLib.sha256(concatGasBridgeOpData(nonce, amount, to));
    }

    private static ByteString concatGasBridgeOpData(int nonce, int amount, Hash160 to) {
        byte[] nonceP = padToBytes(toByteArray(nonce), UINT256_SIZE);
        byte[] amountP = padToBytes(toByteArray(amount), UINT256_SIZE);
        byte[] concatenated = concat(concat(to.toByteArray(), amountP), nonceP);
        reverse(concatenated);
        return new ByteString(concatenated);
    }

    public static ByteString computeNewTopRoot(CryptoLib cryptoLib, ByteString formerRoot, List<Withdrawal> withdrawals) {
        ByteString parent = formerRoot;
        for (int i = 0; i < withdrawals.size(); i++) {
            Withdrawal withdrawal = withdrawals.get(i);
            if (!Withdrawal.isValid(withdrawal)) abort("Invalid withdrawal provided.");
            ByteString withdrawalHash = hashGasBridgeOp(cryptoLib, withdrawal.nonce, withdrawal.amount, withdrawal.to);
            parent = computeNewRoot(cryptoLib, parent, withdrawalHash);
        }
        return parent;
    }
}
