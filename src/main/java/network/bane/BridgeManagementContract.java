package network.bane;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.ECPoint;
import io.neow3j.devpack.Iterator;
import io.neow3j.devpack.List;
import io.neow3j.devpack.Map;
import io.neow3j.devpack.Storage;
import io.neow3j.devpack.StorageContext;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.annotations.DisplayName;
import io.neow3j.devpack.annotations.ManifestExtra;
import io.neow3j.devpack.annotations.OnDeployment;
import io.neow3j.devpack.annotations.Permission;
import io.neow3j.devpack.annotations.Safe;
import io.neow3j.devpack.constants.FindOptions;
import io.neow3j.devpack.constants.NativeContract;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.events.Event1Arg;
import io.neow3j.devpack.events.Event2Args;
import network.bane.structs.ManagementDeploymentData;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Runtime.checkWitness;

@DisplayName("NeoXBridgeManagement")
@Permission(nativeContract = NativeContract.ContractManagement, methods = "update")
@ManifestExtra(key = "Author", value = "BaneLabs")
@ManifestExtra(key = "Description", value = "Contract for managing roles in the Neo X bridge contract")
public class BridgeManagementContract {

    private static final StorageContext ctx = Storage.getStorageContext();

    private static final byte prefix_base = 0x0a;
    private static final StorageMap baseMap = new StorageMap(ctx, prefix_base);
    private static final byte prefix_validator = 0x0b;
    private static final StorageMap validatorMap = new StorageMap(ctx, prefix_validator);

    private static final int key_owner = 0x00;
    private static final int key_relayer = 0x01;
    private static final int key_governor = 0x02;
    private static final int key_securityguard = 0x03;

    private static final int key_validator_threshold = 0x10;

    private static final int const_max_validators = 21;

    // region events

    @DisplayName("SetOwner")
    public static Event1Arg<ECPoint> onOwnerSet;

    @DisplayName("SetRelayer")
    public static Event1Arg<ECPoint> onRelayerSet;

    @DisplayName("SetValidators")
    public static Event2Args<List<ECPoint>, Integer> onValidatorsSet;

    @DisplayName("SetGovernor")
    public static Event1Arg<ECPoint> onGovernorSet;

    @DisplayName("SetSecurityGuard")
    public static Event1Arg<ECPoint> onSecurityGuardSet;

    // endregion
    // region deployment

    @OnDeployment
    public static void deploy(Object data, boolean isUpdate) {
        if (!isUpdate) {
            ManagementDeploymentData deploymentData = (ManagementDeploymentData) data;

            if (!ECPoint.isValid(deploymentData.owner)) abort("Invalid public key provided for owner.");
            if (!ECPoint.isValid(deploymentData.relayer)) abort("Invalid public key provided for relayer.");
            List<ECPoint> validators = deploymentData.validators;
            int validatorSize = validators.size();
            if (validatorSize > const_max_validators) abort("Too many validators provided.");
            if (validatorSize < deploymentData.validatorThreshold) abort("Not enough validators.");

            baseMap.put(key_owner, deploymentData.owner);
            baseMap.put(key_relayer, deploymentData.relayer);
            for (int i = 0; i < validatorSize; i++) {
                ECPoint validator = validators.get(i);
                if (!ECPoint.isValid(validator)) abort("Invalid public key provided for validator.");
                validatorMap.put(validator, true);
            }
            baseMap.put(key_validator_threshold, deploymentData.validatorThreshold);
            baseMap.put(key_governor, deploymentData.governor);
            baseMap.put(key_securityguard, deploymentData.securityGuard);
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
        if (hasDuplicates(validators)) abort("Duplicate validators provided.");
        if (validators.size() < threshold) abort("Not enough validators.");
        if (threshold <= 0) abort("Threshold must be greater than 0.");
        Iterator<ByteString> it = validatorMap.find(FindOptions.RemovePrefix | FindOptions.KeysOnly);
        while (it.next()) {
            ByteString key = it.get();
            validatorMap.delete(key);
        }
        for (int i = 0; i < validators.size(); i++) {
            ECPoint validator = validators.get(i);
            if (!ECPoint.isValid(validator)) abort("Invalid validator public key provided.");
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

    public static void setGovernor(ECPoint governor) {
        onlyOwner();
        baseMap.put(key_governor, governor);
        onGovernorSet.fire(governor);
    }

    public static void setSecurityGuard(ECPoint securityGuard) {
        onlyOwner();
        baseMap.put(key_securityguard, securityGuard);
        onSecurityGuardSet.fire(securityGuard);
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

    @Safe
    public static ECPoint governor() {
        return baseMap.getECPoint(key_governor);
    }

    @Safe
    public static ECPoint securityGuard() {
        return baseMap.getECPoint(key_securityguard);
    }

    // endregion
    // region restrictions

    private static void onlyOwner() {
        if (!checkWitness(owner())) abort("No authorization.");
    }

    // endregion
    // region update

    public static void update(ByteString nef, String manifest) {
        onlyOwner();
        new ContractManagement().update(nef, manifest);
    }

    // endregion

}
