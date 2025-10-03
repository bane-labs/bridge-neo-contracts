package network.bane.scripts;

import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.types.Hash160;

import static network.bane.utils.deployment.BridgeCompilation.*;
import static network.bane.utils.env.EnvVariables.NODE;

public class DeployMessageBridgeContracts {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = Neow3j.build(new HttpService(NODE, true));
        Hash160 managementContract = compileAndPrintManagementDeploymentTxData(neow3j);
        compileAndPrintMessageBridgeDeploymentTxData(neow3j, managementContract);
    }
}
