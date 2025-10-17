package network.bane.scripts.deploy;

import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.types.Hash160;

import static network.bane.utils.deployment.BridgeCompilation.compileAndPrintBridgeDeploymentTxData;
import static network.bane.utils.deployment.BridgeCompilation.compileAndPrintManagementDeploymentTxData;
import static network.bane.utils.env.EnvVariables.NODE;

/**
 * This class is used to create the deployment transactions for the Bridge Management and Bridge in a private net.
 */
public class DeployBridgeContracts {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = Neow3j.build(new HttpService(NODE, true));

        Hash160 managementHash = compileAndPrintManagementDeploymentTxData(neow3j);
        compileAndPrintBridgeDeploymentTxData(neow3j, managementHash);
    }

}
