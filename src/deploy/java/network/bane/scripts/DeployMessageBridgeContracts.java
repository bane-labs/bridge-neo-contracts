package network.bane.scripts;

import io.neow3j.compiler.CompilationUnit;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;

import java.math.BigInteger;

import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static network.bane.utils.deployment.BridgeCompilation.*;
import static network.bane.utils.deployment.DeploymentHelper.calculateContractHash;
import static network.bane.utils.deployment.DeploymentHelper.deployContract;
import static network.bane.utils.deployment.DeploymentHelper.ensureConsistentState;
import static network.bane.utils.env.EnvVariables.NODE;
import static network.bane.utils.env.EnvVariables.deployerAcc;
import static network.bane.utils.env.EnvVariables.linkedChainId;

public class DeployMessageBridgeContracts {

    private static final Account deploymentAccount = deployerAcc;

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = Neow3j.build(new HttpService(NODE, true));
        Hash160 managementContract = compileAndPrintManagementDeploymentTxData(neow3j);

        // Compile the contracts
        CompilationUnit msgBridgeCompUnit = compileMessageBridge();
        CompilationUnit execManagerCompUnit = compileExecutionManager();

        // Calculate the contract hashes
        Hash160 messageBridgeHash = calculateContractHash(msgBridgeCompUnit, deploymentAccount);
        Hash160 executionManagerHash = calculateContractHash(execManagerCompUnit, deploymentAccount);

        // Deploy the Message Bridge Contract
        ContractParameter msgBridgeDeployParam = prepMsgBridgeDeployParam(linkedChainId, managementContract,
                executionManagerHash);
        Hash256 msgBridgeDeployTx = deployContract(neow3j, msgBridgeCompUnit, deploymentAccount, msgBridgeDeployParam);
        ensureConsistentState(neow3j, messageBridgeHash, msgBridgeCompUnit, msgBridgeDeployTx);

        // Deploy the Execution Manager Contract
        ContractParameter execManagerDeployParam = prepExecManagerDeployParam(managementContract, messageBridgeHash);
        Hash256 execManagerDeployTx = deployContract(neow3j, execManagerCompUnit, deploymentAccount,
                execManagerDeployParam);
        ensureConsistentState(neow3j, executionManagerHash, execManagerCompUnit, execManagerDeployTx);

        System.out.println("\n✅ All contracts deployed successfully!");
        // This summary is for capturing the deployed contract hashes easily from the console.
        System.out.println("-----------------------------------");
        System.out.println("BridgeManagement contract hash: " + managementContract);
        System.out.println("MessageBridge contract hash:    " + messageBridgeHash);
        System.out.println("ExecutionManager contract hash: " + executionManagerHash);
        System.out.println("-----------------------------------");
    }

    private static ContractParameter prepMsgBridgeDeployParam(BigInteger linkedChain, Hash160 managementContract,
            Hash160 execManagerHash) {
        return array(integer(linkedChain), hash160(managementContract), hash160(execManagerHash));
    }

    private static ContractParameter prepExecManagerDeployParam(Hash160 managementContract, Hash160 messageBridgeHash) {
        return array(hash160(managementContract), hash160(messageBridgeHash));
    }

}
