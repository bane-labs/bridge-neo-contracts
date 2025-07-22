package network.bane.message;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.annotations.Struct;
import network.bane.structs.BridgeDeploymentData;

@Struct
public class MessageDeploymentData {
    public Integer linkedChainId;
    public Hash160 bridgeManagementContract;

    public static boolean isValid(BridgeDeploymentData deploymentData) {
        return deploymentData.linkedChainId != null && deploymentData.linkedChainId > 0 &&
                deploymentData.bridgeManagementContract != null &&
                Hash160.isValid(deploymentData.bridgeManagementContract);
    }
}
