package network.bane;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Helper;
import io.neow3j.devpack.Runtime;
import io.neow3j.devpack.annotations.DisplayName;
import io.neow3j.devpack.annotations.ManifestExtra;
import io.neow3j.devpack.annotations.OnNEP17Payment;
import io.neow3j.devpack.contracts.GasToken;
import io.neow3j.devpack.events.Event4Args;

@DisplayName("FungibleTokenBridge")
@ManifestExtra(key = "author", value = "BaneLabs")
@ManifestExtra(key = "description", value = "Contract for bridging fungible tokens from Neo mainnet to Bane and back.")
public class FungibleTokenBridgeContract {

    @DisplayName("OnDeposit")
    public static Event4Args<Hash160, Hash160, Hash160, Integer> onDeposit;

    @OnNEP17Payment
    public static void deposit(Hash160 from, int amount, Object data) {
        Hash160 callingScriptHash = Runtime.getCallingScriptHash();
        checkCallingScriptHash(callingScriptHash);

        if (!Hash160.isValid(data)) {
            Helper.abort();
        }

        Hash160 to = (Hash160) data;

        onDeposit.fire(callingScriptHash, from, to, amount);
    }

    /**
     * Aborts if calling script hash is not accepted.
     * @param callingScriptHash the script hash of the caller.
     */
    private static void checkCallingScriptHash(Hash160 callingScriptHash) {
        // todo: create a whitelist for accepted tokens
        if (callingScriptHash != new GasToken().getHash()) {
            // todo: abort with message
            Helper.abort();
        }
    }

}
