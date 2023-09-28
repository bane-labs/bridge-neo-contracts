package network.bane;

import io.neow3j.devpack.ECPoint;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Helper;
import io.neow3j.devpack.Runtime;
import io.neow3j.devpack.Storage;
import io.neow3j.devpack.StorageContext;
import io.neow3j.devpack.annotations.DisplayName;
import io.neow3j.devpack.annotations.ManifestExtra;
import io.neow3j.devpack.annotations.OnDeployment;
import io.neow3j.devpack.annotations.Safe;
import io.neow3j.devpack.events.Event1Arg;
import network.bane.structs.ManagementDeploymentData;

import static io.neow3j.devpack.Account.createMultiSigAccount;

@DisplayName("BridgeManagement")
@ManifestExtra(key = "author", value = "BaneLabs")
@ManifestExtra(key = "description", value = "Contract for managing ownership and rights for interacting with the " +
        "bridge contract.")
public class BridgeManagementContract {

    private static final StorageContext ctx = Storage.getStorageContext();

    private static final byte key_owner = 0x10;
    private static final byte key_relayer = 0x20;

    private static final int owners_count = 7;
    private static final int owner_threshold = 5;
    private static final int relayers_count = 7;
    private static final int relayer_threshold = 5;

    // region events

    @DisplayName("SetOwner")
    public static Event1Arg<Hash160> onOwnerSet;

    @DisplayName("SetRelayer")
    public static Event1Arg<Hash160> onRelayerSet;

    // endregion
    // region deployment

    @OnDeployment
    public static void _deploy(Object data, boolean isUpdate) {
        if (!isUpdate) {
            ManagementDeploymentData deploymentData = (ManagementDeploymentData) data;
            initOwner(deploymentData.owners);
            initRelayer(deploymentData.relayers);
        }
    }

    private static void initOwner(ECPoint[] owners) {
        checkValidMultiSigProperties(owners, owners_count);
        Hash160 owner = createMultiSigAccount(owner_threshold, owners);
        internalSetOwner(owner);
        onOwnerSet.fire(owner);
    }

    private static void initRelayer(ECPoint[] relayers) {
        checkValidMultiSigProperties(relayers, relayers_count);
        Hash160 relayer = createMultiSigAccount(relayer_threshold, relayers);
        internalSetRelayer(relayer);
        onRelayerSet.fire(relayer);
    }

    // endregion
    // region setters

    public static void setRelayer(ECPoint[] relayers) {
        onlyOwner();
        checkValidMultiSigProperties(relayers, relayers_count);

        Hash160 relayer = createMultiSigAccount(relayer_threshold, relayers);
        internalSetRelayer(relayer);
        onRelayerSet.fire(relayer);
    }

    public static void setOwner(ECPoint[] owners) {
        onlyOwner();
        checkValidMultiSigProperties(owners, owners_count);

        Hash160 owner = createMultiSigAccount(owner_threshold, owners);
        internalSetOwner(owner);
        onOwnerSet.fire(owner);
    }

    private static void checkValidMultiSigProperties(ECPoint[] pubKeys, int count) {
        assert pubKeys.length == count;
        for (ECPoint pubKey : pubKeys) {
            assert ECPoint.isValid(pubKey);
        }
    }

    private static void internalSetOwner(Hash160 owner) {
        Storage.put(ctx, key_owner, owner);
    }

    private static void internalSetRelayer(Hash160 relayer) {
        Storage.put(ctx, key_relayer, relayer);
    }

    // endregion
    // region getters

    @Safe
    public static Hash160 owner() {
        return Storage.getHash160(ctx, key_owner);
    }

    @Safe
    public static Hash160 relayer() {
        return Storage.getHash160(ctx, key_relayer);
    }

    // endregion
    // region restrictions

    private static void onlyOwner() {
        if (!Runtime.checkWitness(owner())) {
            Helper.abort();
        }
    }

    private static void onlyRelayer() {
        if (!Runtime.checkWitness(relayer())) {
            Helper.abort();
        }
    }

    // endregion
    // region migrate

    // Todo: Add code for updating contract (call to contract management and storage changes)

    // endregion

}
