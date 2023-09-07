package network.bane;

import io.neow3j.crypto.ECKeyPair;
import io.neow3j.devpack.Account;
import io.neow3j.devpack.ECPoint;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Helper;
import io.neow3j.devpack.Runtime;
import io.neow3j.devpack.Storage;
import io.neow3j.devpack.StorageContext;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.annotations.DisplayName;
import io.neow3j.devpack.annotations.ManifestExtra;
import io.neow3j.devpack.annotations.OnDeployment;
import io.neow3j.devpack.annotations.OnNEP17Payment;
import io.neow3j.devpack.annotations.Safe;
import io.neow3j.devpack.contracts.GasToken;
import io.neow3j.devpack.contracts.NeoToken;
import io.neow3j.devpack.events.Event4Args;

@DisplayName("FungibleTokenBridge")
@ManifestExtra(key = "author", value = "BaneLabs")
@ManifestExtra(key = "description", value = "Contract for bridging fungible tokens from Neo mainnet to Bane and back.")
public class FungibleTokenBridgeContract {

    private static final StorageContext ctx = Storage.getStorageContext();
    private static final StorageMap adminMap = new StorageMap(ctx, 0x0a);
    private static final byte relayerKey = (byte) 0x01;

    @DisplayName("OnDeposit")
    public static Event4Args<Hash160, Hash160, Hash160, Integer> onDeposit;

    // region deploy and update
    // endregion
    // region ownership and relayer

    // Todo Questions:
    //  Who can set the relayer multi-sig address?
    //  Who can update the contract?
    //  Can the contract be locked by any party? If so, by who?

    @Safe
    public static Hash160 getOwner() {
        ECPoint[] committee = new NeoToken().getCommittee();
        // Todo: Discuss Threshold. Currently: Threshold >50% (len=20->11; len=21->11; len=22->12)
        int threshold = (committee.length / 2) + 1;
        return Account.createMultiSigAccount(threshold, committee);
    }

    private static void checkOwner() {
        if (!Runtime.checkWitness(getOwner())) {
            Helper.abort();
        }
    }

    public static void setRelayer(int m, io.neow3j.devpack.ECPoint[] pubKeys) {
        // Todo: Discuss required relayer properties
        assert m == 5 && pubKeys.length == 7;

        checkOwner();

        Hash160 multiSig = Account.createMultiSigAccount(m, pubKeys);
        if (!Runtime.checkWitness(multiSig)) {
            Helper.abort();
        }
        adminMap.put(relayerKey, multiSig);
    }

    @Safe
    public static Hash160 getRelayer() {
        return adminMap.getHash160(relayerKey);
    }

    // endregion
    // region deposit

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
            Helper.abort();
        }
    }

    // endregion
    // region unlocking
    // endregion

}
