package network.bane.management;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.ECPoint;
import io.neow3j.devpack.Iterator;
import io.neow3j.devpack.List;
import io.neow3j.devpack.Map;
import io.neow3j.devpack.Runtime;
import io.neow3j.devpack.Storage;
import io.neow3j.devpack.StorageContext;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.annotations.DisplayName;
import io.neow3j.devpack.annotations.EventParameterNames;
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

    @DisplayName("OwnerChange")
    @EventParameterNames({"NewOwner"})
    public static Event1Arg<ECPoint> onOwnerSet;

    @DisplayName("RelayerChange")
    @EventParameterNames({"NewRelayer"})
    public static Event1Arg<ECPoint> onRelayerSet;

    @DisplayName("ValidatorsChange")
    @EventParameterNames({"NewValidators", "NewThreshold"})
    public static Event2Args<List<ECPoint>, Integer> onValidatorsSet;

    @DisplayName("GovernorChange")
    @EventParameterNames({"NewGovernor"})
    public static Event1Arg<ECPoint> onGovernorSet;

    @DisplayName("SecurityGuardChange")
    @EventParameterNames({"NewSecurityGuard"})
    public static Event1Arg<ECPoint> onSecurityGuardSet;

    // endregion
    // region deployment

    @OnDeployment
    public static void deploy(Object data, boolean isUpdate) {
        if (!isUpdate) {
            ManagementDeploymentData deploymentData = (ManagementDeploymentData) data;

            ECPoint owner = deploymentData.owner;
            if (owner == null || !ECPoint.isValid(owner)) abort("Invalid public key provided for owner.");
            ECPoint relayer = deploymentData.relayer;
            if (relayer == null || !ECPoint.isValid(relayer)) abort("Invalid public key provided for relayer.");
            List<ECPoint> validators = deploymentData.validators;
            int validatorSize = validators.size();
            if (validatorSize > const_max_validators) abort("Too many validators provided.");
            int validatorThreshold = deploymentData.validatorThreshold;
            if (validatorSize < validatorThreshold) abort("Not enough validators.");
            ECPoint governor = deploymentData.governor;
            if (governor == null || !ECPoint.isValid(governor)) abort("Invalid public key provided for governor.");
            ECPoint securityGuard = deploymentData.securityGuard;
            if (securityGuard == null || !ECPoint.isValid(securityGuard)) abort("Invalid public key provided for security guard.");

            baseMap.put(key_owner, owner);
            baseMap.put(key_relayer, relayer);
            for (int i = 0; i < validatorSize; i++) {
                ECPoint validator = validators.get(i);
                if (!ECPoint.isValid(validator)) abort("Invalid public key provided for validator.");
                validatorMap.put(validator, true);
            }
            baseMap.put(key_validator_threshold, validatorThreshold);
            baseMap.put(key_governor, governor);
            baseMap.put(key_securityguard, securityGuard);
        }
    }

    // endregion
    // region setters

    public static void setOwner(ECPoint newOwner) {
        onlyOwner();
        if (newOwner == null || !ECPoint.isValid(newOwner)) abort("Invalid public key provided.");
        if (!Runtime.checkWitness(newOwner)) abort("New owner must be witness to the transaction.");
        baseMap.put(key_owner, newOwner);
        onOwnerSet.fire(newOwner);
    }

    public static void setRelayer(ECPoint newRelayer) {
        onlyOwner();
        if (newRelayer == null || !ECPoint.isValid(newRelayer)) abort("Invalid public key provided.");
        baseMap.put(key_relayer, newRelayer);
        onRelayerSet.fire(newRelayer);
    }

    public static void setValidators(List<ECPoint> validators, int threshold) {
        onlyOwner();
        if (hasDuplicates(validators)) abort("Duplicate validators provided.");
        int validatorsSize = validators.size();
        if (threshold <= 0) abort("Threshold must be greater than 0.");
        if (validatorsSize < threshold) abort("Not enough validators.");
        Iterator<ByteString> it = validatorMap.find(FindOptions.RemovePrefix | FindOptions.KeysOnly);
        while (it.next()) {
            ByteString key = it.get();
            validatorMap.delete(key);
        }
        for (int i = 0; i < validatorsSize; i++) {
            ECPoint validator = validators.get(i);
            if (validator == null || !ECPoint.isValid(validator)) abort("Invalid validator public key provided.");
            validatorMap.put(validator, true);
        }
        baseMap.put(key_validator_threshold, threshold);
        onValidatorsSet.fire(validators, threshold);
    }

    private static boolean hasDuplicates(List<ECPoint> validators) {
        Map<ECPoint, Boolean> map = new Map<>();
        int validatorsSize = validators.size();
        for (int i = 0; i < validatorsSize; i++) {
            map.put(validators.get(i), true);
        }
        return map.keys().length != validatorsSize;
    }

    public static void setGovernor(ECPoint newGovernor) {
        onlyOwner();
        if (newGovernor == null || !ECPoint.isValid(newGovernor)) abort("Invalid public key provided.");
        baseMap.put(key_governor, newGovernor);
        onGovernorSet.fire(newGovernor);
    }

    public static void setSecurityGuard(ECPoint newSecurityGuard) {
        onlyOwner();
        if (newSecurityGuard == null || !ECPoint.isValid(newSecurityGuard)) abort("Invalid public key provided.");
        baseMap.put(key_securityguard, newSecurityGuard);
        onSecurityGuardSet.fire(newSecurityGuard);
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
