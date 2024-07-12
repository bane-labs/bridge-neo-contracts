package network.bane.testhelper;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Contract;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Helper;
import io.neow3j.devpack.Runtime;
import io.neow3j.devpack.Storage;
import io.neow3j.devpack.StorageContext;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.annotations.*;
import io.neow3j.devpack.constants.CallFlags;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.events.Event3Args;

@DisplayName("BaneBridge")
@Permission(contract = "*")
@ManifestExtra(key = "name", value = "AxLabsToken")
@ManifestExtra(key = "author", value = "AxLabs")
public class Nep17TokenContract {

    static final int contractMapPrefix = 0;
    static final byte[] contractOwnerKey = new byte[]{0x00};
    static final byte[] totalSupplyKey = new byte[]{0x01};

    static final int assetMapPrefix = 1;

    // region deploy, update, destroy

    @OnDeployment
    public static void deploy(Object data, boolean update) {
        if (!update) {
            StorageContext ctx = Storage.getStorageContext();
            // Set the contract owner.
            Hash160 initialOwner = (Hash160) data;
            if (!Hash160.isValid(initialOwner)) Helper.abort("Invalid deployment parameter");
            Storage.put(ctx, contractOwnerKey, initialOwner);
            // Initialize the supply.
            int initialSupply = 200_000_000;
            Storage.put(ctx, totalSupplyKey, initialSupply);
            // Allocate all tokens to the contract owner.
            new StorageMap(ctx, assetMapPrefix).put(initialOwner, initialSupply);
            onTransfer.fire(null, initialOwner, initialSupply);
            if (new ContractManagement().getContract(initialOwner) != null) {
                Contract.call(initialOwner, "onNEP17Payment", CallFlags.All, new Object[]{null, initialSupply, null});
            }
        }
    }


    @OnNEP17Payment
    public static void onNep17Payment(Hash160 from, int amount, Object data) {
    }

    public static void update(ByteString script, String manifest) throws Exception {
        if (!Runtime.checkWitness(contractOwner(Storage.getReadOnlyContext()))) {
            throw new Exception("No authorization");
        }
        new ContractManagement().update(script, manifest);
    }

    // endregion deploy, update, destroy
    // region NEP-17 methods

    @Safe
    public static String symbol() {
        return "ALT";
    }

    @Safe
    public static int decimals() {
        return 2;
    }

    @Safe
    public static int totalSupply() {
        return Storage.getInt(Storage.getReadOnlyContext(), totalSupplyKey);
    }

    public static boolean transfer(Hash160 from, Hash160 to, int amount, Object[] data) throws Exception {
        if (!Hash160.isValid(from) || !Hash160.isValid(to)) {
            throw new Exception("The parameters 'from' and 'to' must be 20-byte addresses.");
        }
        if (amount < 0) {
            throw new Exception("The parameter 'amount' must be greater than or equal to 0.");
        }
        StorageContext ctx = Storage.getStorageContext();
        if (amount > getBalance(ctx, from) || !Runtime.checkWitness(from)) {
            return false;
        }

        if (from != to && amount != 0) {
            deductFromBalance(ctx, from, amount);
            addToBalance(ctx, to, amount);
        }

        onTransfer.fire(from, to, amount);
        if (new ContractManagement().getContract(to) != null) {
            Contract.call(to, "onNEP17Payment", CallFlags.All, new Object[]{from, amount, data});
        }
        return true;
    }

    @Safe
    public static int balanceOf(Hash160 account) throws Exception {
        if (!Hash160.isValid(account)) {
            throw new Exception("The parameter 'account' must be a 20-byte address.");
        }
        return getBalance(Storage.getReadOnlyContext(), account);
    }

    // endregion NEP-17 methods
    // region events

    @DisplayName("Transfer")
    static Event3Args<Hash160, Hash160, Integer> onTransfer;


    private static Hash160 contractOwner(StorageContext ctx) {
        return new StorageMap(ctx, contractMapPrefix).getHash160(contractOwnerKey);
    }

    private static void addToBalance(StorageContext ctx, Hash160 key, int value) {
        new StorageMap(ctx, assetMapPrefix).put(key.toByteArray(), getBalance(ctx, key) + value);
    }

    private static void deductFromBalance(StorageContext ctx, Hash160 key, int value) {
        int oldValue = getBalance(ctx, key);
        new StorageMap(ctx, assetMapPrefix).put(key.toByteArray(), oldValue - value);
    }

    private static int getBalance(StorageContext ctx, Hash160 key) {
        return new StorageMap(ctx, assetMapPrefix).getIntOrZero(key.toByteArray());
    }
}
