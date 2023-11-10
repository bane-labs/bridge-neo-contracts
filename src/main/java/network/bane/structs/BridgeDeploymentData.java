package network.bane.structs;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.annotations.Struct;

@Struct
public class BridgeDeploymentData {
    public Hash160 bridgeManagementContractHash;
    public int depositPrice;
    public int minDeposit;
    public int maxDeposit;
    public int maxWithdrawalPerRootUpdate;

    public static boolean isValid(BridgeDeploymentData bridgeDeploymentData) {
        return Hash160.isValid(bridgeDeploymentData.bridgeManagementContractHash) &&
                bridgeDeploymentData.depositPrice >= 0 &&
                bridgeDeploymentData.minDeposit >= 0 &&
                bridgeDeploymentData.maxDeposit >= bridgeDeploymentData.minDeposit &&
                bridgeDeploymentData.maxWithdrawalPerRootUpdate >= 0;
    }

}
