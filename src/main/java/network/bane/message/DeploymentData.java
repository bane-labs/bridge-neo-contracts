package network.bane.message;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.annotations.Struct;

@Struct
class DeploymentData {
    public Integer linkedChainId;
    public Hash160 managementContract;
    public Hash160 executionManager;

    public static boolean isValid(DeploymentData deployData) {
        return deployData.linkedChainId != null && deployData.linkedChainId > 0 &&
                deployData.managementContract != null && Hash160.isValid(deployData.managementContract) &&
                deployData.executionManager != null && Hash160.isValid(deployData.executionManager);
    }

}
