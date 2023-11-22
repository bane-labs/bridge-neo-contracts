package network.bane;

import io.neow3j.devpack.Runtime;
import io.neow3j.devpack.*;
import io.neow3j.devpack.annotations.DisplayName;
import io.neow3j.devpack.annotations.ManifestExtra;
import io.neow3j.devpack.annotations.OnDeployment;
import io.neow3j.devpack.annotations.Safe;
import io.neow3j.devpack.constants.FindOptions;
import io.neow3j.devpack.events.Event1Arg;
import io.neow3j.devpack.events.Event2Args;
import network.bane.structs.ManagementDeploymentData;

@DisplayName("BridgeManagement")
@ManifestExtra(key = "author", value = "BaneLabs")
@ManifestExtra(
        key = "description",
        value = "Contract for managing ownership and rights for interacting with the bridge contract."
)
public class BridgeManagementContract {

    private static final StorageContext ctx = Storage.getStorageContext();

    private static final byte prefix_base = 0x0a;
    private static final StorageMap baseMap = new StorageMap(ctx, prefix_base);

    private static final int key_owner = 0x00;
    private static final int key_relayer = 0x01;
    private static final int key_validator_threshold = 0x02;

    private static final byte prefix_validator = 0x0b;
    private static final StorageMap validatorMap = new StorageMap(ctx, prefix_validator);

    private static final int const_max_validators = 21;

    // region events

    @DisplayName("SetOwner")
    public static Event1Arg<ECPoint> onOwnerSet;

    @DisplayName("SetRelayer")
    public static Event1Arg<ECPoint> onRelayerSet;

    @DisplayName("SetValidators")
    public static Event2Args<List<ECPoint>, Integer> onValidatorsSet;

    // endregion
    // region deployment

    @OnDeployment
    public static void deploy(Object data, boolean isUpdate) {
        if (!isUpdate) {
            ManagementDeploymentData deploymentData = (ManagementDeploymentData) data;
            assert ECPoint.isValid(deploymentData.owner);
            assert ECPoint.isValid(deploymentData.relayer);
            List<ECPoint> validators = deploymentData.validators;
            int validatorSize = validators.size();
            assert validatorSize <= const_max_validators;
            assert validatorSize >= deploymentData.validatorThreshold;

            baseMap.put(key_owner, deploymentData.owner);
            baseMap.put(key_relayer, deploymentData.relayer);
            baseMap.put(key_validator_threshold, deploymentData.validatorThreshold);

            for (int i = 0; i < validatorSize; i++) {
                ECPoint validator = validators.get(i);
                assert ECPoint.isValid(validator);
                validatorMap.put(validator, true);
            }
        }
    }

    // endregion
    // region setters

    public static void setOwner(ECPoint owner) {
        onlyOwner();
        baseMap.put(key_owner, owner);
        onOwnerSet.fire(owner);
    }

    public static void setRelayer(ECPoint relayer) {
        onlyOwner();
        baseMap.put(key_relayer, relayer);
        onRelayerSet.fire(relayer);
    }

    public static void setValidators(List<ECPoint> validators, int threshold) {
        onlyOwner();
        assert !hasDuplicates(validators) : "Duplicate validators provided.";
        assert validators.size() >= threshold : "Not enough validators.";
        Iterator<ByteString> it = validatorMap.find(FindOptions.RemovePrefix | FindOptions.KeysOnly);
        while (it.next()) {
            ByteString key = it.get();
            validatorMap.delete(key);
        }
        for (int i = 0; i < validators.size(); i++) {
            ECPoint validator = validators.get(i);
            assert ECPoint.isValid(validator) : "Invalid validator public key provided.";
            validatorMap.put(validator, true);
        }
        baseMap.put(key_validator_threshold, threshold);
        onValidatorsSet.fire(validators, threshold);
    }

    private static boolean hasDuplicates(List<ECPoint> validators) {
        Map<ECPoint, Boolean> map = new Map<>();
        for (int i = 0; i < validators.size(); i++) {
            map.put(validators.get(i), true);
        }
        return map.keys().length != validators.size();
    }

    // endregion
    // region getters

    @Safe
    public static ECPoint owner() {
        return baseMap.getECPoint(key_owner);
    }

    @Safe
    public static ECPoint relayer() {
        return baseMap.getECPoint(key_relayer);
    }

    @Safe
    public static List<ECPoint> validators() {
        Iterator<ByteString> it = validatorMap.find(FindOptions.RemovePrefix | FindOptions.KeysOnly);
        List<ECPoint> validators = new List<>();
        while (it.next()) {
            validators.add(new ECPoint(it.get()));
        }
        return validators;
    }

    @Safe
    public static int validatorThreshold() {
        return baseMap.getInt(key_validator_threshold);
    }

    // endregion
    // region restrictions

    private static void onlyOwner() {
        if (!Runtime.checkWitness(owner())) {
            Helper.abort("No authorization.");
        }
    }

    // endregion
    // region migrate

    // Todo: Add code for updating contract (call to contract management and storage changes)

    // endregion

}
