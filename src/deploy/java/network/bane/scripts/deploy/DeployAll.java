package network.bane.scripts.deploy;

import io.neow3j.compiler.CompilationUnit;
import io.neow3j.protocol.Neow3j;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;

import java.math.BigInteger;

import static network.bane.utils.deployment.BridgeCompilation.compileAndPrintBridgeDeploymentTxData;
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
import static network.bane.utils.env.EnvVariables.getBigIntegerFromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnvVar;
import static network.bane.utils.env.EnvVariables.getEnvVariable;
import static network.bane.utils.wallet.LoadWallet.getAccountFromWallet;

/**
 * This class is used to create the deployment transactions for all Bridge contracts.
 * <p>
 * The following contracts are deployed:
 * <ul>
 *  <li>{@link network.bane.management.BridgeManagementContract}<\li>
 *  <li>{@link network.bane.bridge.BridgeContract}<\li>
 *  <li>{@link network.bane.message.MessageBridgeContract}<\li>
 *  <li>{@link network.bane.messageexecution.ExecutionManagerContract}<\li>
 * </ul>
 * <p>
 * Requires the following environment variables to be set:
 * - N3_JSON_RPC: The RPC endpoint of the N3 node
 * - WALLET_FILEPATH_DEPLOYER: the filepath to the deployer wallet
 * - WALLET_PASSWORD_DEPLOYER: the password for the deployer wallet
 * - LINKED_CHAIN_ID: The chain ID of the linked chain
 * <p>
 * Run with: gradle run -PmainClass=network.bane.scripts.deploy.DeployAll
 */
public class DeployAll {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnvVar(N3_JSON_RPC);
        String deployerWalletPath = getEnvVariable(WALLET_FILEPATH_DEPLOYER);
        String deployerWalletPassword = getEnvVariable(WALLET_PASSWORD_DEPLOYER);
        BigInteger linkedChainId = getBigIntegerFromEnvVar(LINKED_CHAIN_ID);

        Account deployerAcc = getAccountFromWallet(deployerWalletPath, deployerWalletPassword);

        // Deploy the BridgeManagement contract
        Hash160 managementContract = compileAndPrintManagementDeploymentTxData(neow3j);

        // Deploy the Bridge contract
        Hash160 bridgeContractHash = compileAndPrintBridgeDeploymentTxData(neow3j, managementContract);

        // Deploy the MessageBridge and the ExecutionManager contracts
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

        System.out.println("\n✅ Contracts deployed successfully!");
        // This summary is for capturing the deployed contract hashes easily from the console.
        System.out.println("-----------------------------------");
        System.out.println("BridgeManagement contract hash: " + managementContract);
        System.out.println("Bridge contract hash:           " + bridgeContractHash);
        System.out.println("MessageBridge contract hash:    " + messageBridgeHash);
        System.out.println("ExecutionManager contract hash: " + executionManagerHash);
        System.out.println("-----------------------------------");
    }
}
