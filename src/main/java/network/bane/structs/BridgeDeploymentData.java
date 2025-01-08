package network.bane.structs;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.annotations.Struct;

@Struct
public class BridgeDeploymentData {
    public Integer targetChainId;
    public Hash160 bridgeManagementContract;
    public GasConfig gasConfig;

    public static boolean isValid(BridgeDeploymentData deploymentData) {
        return deploymentData.targetChainId != null && deploymentData.targetChainId > 0 &&
                deploymentData.bridgeManagementContract != null &&
                Hash160.isValid(deploymentData.bridgeManagementContract) &&
                GasConfig.isValid(deploymentData.gasConfig);
    }
}
