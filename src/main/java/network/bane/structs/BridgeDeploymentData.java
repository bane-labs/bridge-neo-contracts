package network.bane.structs;

import io.neow3j.devpack.Hash160;

public class BridgeDeploymentData {
    public Hash160 bridgeManagementContractHash;
    public int minDeposit;
    public int maxDeposit;
    public int depositPrice;

    public boolean isValid() {
        return Hash160.isValid(bridgeManagementContractHash) &&
                minDeposit > 0 &&
                maxDeposit > minDeposit &&
                depositPrice > 0;
    }

}
