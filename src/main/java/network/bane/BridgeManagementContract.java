package network.bane;

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
    private static final StorageContext ctx_readOnly = Storage.getReadOnlyContext();

    private static final int prefix_baseMap = 0xf0;

    private static final byte key_owner = 0x10;
    private static final byte key_relayer = 0x20;

    private static final int owners_count = 7;
    private static final int owner_threshold = 5;
    private static final int relayers_count = 7;
    private static final int relayer_threshold = 5;

    // region events

    public static Event1Arg<Hash160> onOwnerSet;
    public static Event1Arg<Hash160> onRelayerSet;

    // endregion
    // region deployment

    @OnDeployment
    public static void _deploy(Object data, boolean isUpdate) {
        if (!isUpdate) {
            ManagementDeploymentData deploymentData = (ManagementDeploymentData) data;
            initOwners(deploymentData.owners);
            initRelayers(deploymentData.relayers);
        }
    }

    // endregion
    // region init

    private static void initOwners(ECPoint[] owners) {
        assert owners.length == owners_count;
        for (ECPoint owner : owners) {
            assert ECPoint.isValid(owner);
        }
        Hash160 owner = createMultiSigAccount(owner_threshold, owners);
        internalSetOwner(owner);
        onOwnerSet.fire(owner);
    }

    private static void initRelayers(ECPoint[] relayers) {
        assert relayers.length == relayers_count;
        for (ECPoint relayer : relayers) {
            assert ECPoint.isValid(relayer);
        }
        Hash160 relayer = createMultiSigAccount(relayer_threshold, relayers);
        internalSetRelayer(relayer);
        onRelayerSet.fire(relayer);
    }

    // endregion
    // region setters

    private static void internalSetOwner(Hash160 owner) {
        new StorageMap(ctx, prefix_baseMap).put(key_owner, owner);
    }

    private static void internalSetRelayer(Hash160 multiSigAccount) {
        new StorageMap(ctx, prefix_baseMap).put(key_relayer, multiSigAccount);
    }

    // endregion
    // region getters

    @Safe
    public static Hash160 owner() {
        return new StorageMap(ctx_readOnly, prefix_baseMap).getHash160(key_owner);
    }

    @Safe
    public static Hash160 relayer() {
        return new StorageMap(ctx_readOnly, prefix_baseMap).getHash160(key_relayer);
    }

    // endregion
    // region restrictions

    public static void onlyOwner() {
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
