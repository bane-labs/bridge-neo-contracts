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

public class NativeBridgeLib {

    public static ByteString hashNativeBridgeOp(CryptoLib cryptoLib, int nonce, Hash160 to, int amount) {
        return cryptoLib.keccak256(concatNativeBridgeOpData(nonce, to, amount));
    }

    private static ByteString concatNativeBridgeOpData(int nonce, Hash160 to, int amount) {
        byte[] nonceP = padToBytes(toByteArray(nonce), UINT256_SIZE);
        byte[] amountP = padToBytes(toByteArray(amount), UINT256_SIZE);
        byte[] concatenated = concat(concat(amountP, to.toByteString()), nonceP);
        reverse(concatenated);
        return new ByteString(concatenated);
    }

    public static ByteString computeNewTopRoot(CryptoLib cryptoLib, ByteString formerRoot, List<Withdrawal> withdrawals) {
        ByteString parent = formerRoot;
        for (int i = 0; i < withdrawals.size(); i++) {
            Withdrawal withdrawal = withdrawals.get(i);
            if (!Withdrawal.isValid(withdrawal)) abort("Invalid withdrawal provided.");
            ByteString withdrawalHash = hashNativeBridgeOp(cryptoLib, withdrawal.nonce, withdrawal.to, withdrawal.amount);
            parent = computeNewRoot(cryptoLib, parent, withdrawalHash);
        }
        return parent;
    }

}
