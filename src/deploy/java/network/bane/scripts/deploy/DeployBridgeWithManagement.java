package network.bane.scripts.deploy;

import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.types.Hash160;

import static network.bane.utils.deployment.BridgeCompilation.compileAndPrintBridgeDeploymentTxData;
import static network.bane.utils.deployment.BridgeCompilation.compileAndPrintManagementDeploymentTxData;
import static network.bane.utils.env.EnvVariables.N3_JSON_RPC;

/**
 * This class is used to create the deployment transactions for the Bridge Management and Bridge in a private net.
 */
public class DeployBridgeWithManagement {

    private static final Neow3j neow3j = Neow3j.build(new HttpService(N3_JSON_RPC, true));

    public static void main(String[] args) throws Throwable {
        Hash160 managementHash = compileAndPrintManagementDeploymentTxData(neow3j);
        compileAndPrintBridgeDeploymentTxData(neow3j, managementHash);
    }

}
