package network.bane.structs;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.annotations.Struct;

@Struct
public class BridgeDeploymentData {
    public Hash160 bridgeManagementContract;
    public int gasDepositFee;
    public int minGasDeposit;
    public int maxGasDeposit;
}
