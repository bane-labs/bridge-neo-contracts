package network.bane.scripts.deploy;

import io.neow3j.compiler.CompilationUnit;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;

import java.math.BigInteger;

import static network.bane.utils.deployment.BridgeCompilation.compileAndPrintManagementDeploymentTxData;
import static network.bane.utils.deployment.BridgeCompilation.compileExecutionManager;
import static network.bane.utils.deployment.BridgeCompilation.compileMessageBridge;
import static network.bane.utils.deployment.BridgeDeploymentParameters.prepExecManagerDeployParam;
import static network.bane.utils.deployment.BridgeDeploymentParameters.prepMsgBridgeDeployParam;
import static network.bane.utils.deployment.DeploymentHelper.calculateContractHash;
import static network.bane.utils.deployment.DeploymentHelper.deployContract;
import static network.bane.utils.deployment.DeploymentHelper.ensureConsistentState;
import static network.bane.utils.env.EnvVariables.LINKED_CHAIN_ID;
import static network.bane.utils.env.EnvVariables.N3_JSON_RPC;
import static network.bane.utils.env.EnvVariables.WALLET_FILEPATH_DEPLOYER;
import static network.bane.utils.env.EnvVariables.WALLET_PASSWORD_DEPLOYER;
import static network.bane.utils.wallet.LoadWallet.getAccountFromWallet;

public class DeployMessageBridgeWithManagement {

    private static final Neow3j neow3j = Neow3j.build(new HttpService(N3_JSON_RPC, true));
    private static final String deployerWalletPath = WALLET_FILEPATH_DEPLOYER;
    private static final String deployerWalletPassword = WALLET_PASSWORD_DEPLOYER;
    private static final BigInteger linkedChainId = LINKED_CHAIN_ID;

    public static void main(String[] args) throws Throwable {
        Account deployerAcc = getAccountFromWallet(deployerWalletPath, deployerWalletPassword);

        Hash160 managementContract = compileAndPrintManagementDeploymentTxData(neow3j);

        // Compile the contracts
        CompilationUnit msgBridgeCompUnit = compileMessageBridge();
        CompilationUnit execManagerCompUnit = compileExecutionManager();

        // Calculate the contract hashes
        Hash160 messageBridgeHash = calculateContractHash(msgBridgeCompUnit, deployerAcc);
        Hash160 executionManagerHash = calculateContractHash(execManagerCompUnit, deployerAcc);

        // Deploy the Message Bridge Contract
        ContractParameter msgBridgeDeployParam = prepMsgBridgeDeployParam(linkedChainId, managementContract,
                executionManagerHash);
        Hash256 msgBridgeDeployTx = deployContract(neow3j, msgBridgeCompUnit, deployerAcc, msgBridgeDeployParam);
        ensureConsistentState(neow3j, messageBridgeHash, msgBridgeCompUnit, msgBridgeDeployTx);

        // Deploy the Execution Manager Contract
        ContractParameter execManagerDeployParam = prepExecManagerDeployParam(managementContract, messageBridgeHash);
        Hash256 execManagerDeployTx = deployContract(neow3j, execManagerCompUnit, deployerAcc, execManagerDeployParam);
        ensureConsistentState(neow3j, executionManagerHash, execManagerCompUnit, execManagerDeployTx);

        System.out.println("\n✅ All contracts deployed successfully!");
        // This summary is for capturing the deployed contract hashes easily from the console.
        System.out.println("-----------------------------------");
        System.out.println("BridgeManagement contract hash: " + managementContract);
        System.out.println("MessageBridge contract hash:    " + messageBridgeHash);
        System.out.println("ExecutionManager contract hash: " + executionManagerHash);
        System.out.println("-----------------------------------");
    }

}
