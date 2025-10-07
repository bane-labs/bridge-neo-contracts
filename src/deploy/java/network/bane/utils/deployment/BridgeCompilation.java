package network.bane.utils.deployment;

import io.neow3j.compiler.CompilationUnit;
import io.neow3j.compiler.Compiler;
import io.neow3j.contract.ContractManagement;
import io.neow3j.contract.SmartContract;
import io.neow3j.crypto.ECKeyPair;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.ContractState;
import io.neow3j.protocol.core.response.NeoApplicationLog;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.types.NeoVMStateType;
import io.neow3j.utils.Await;
import network.bane.bridge.BridgeContract;
import network.bane.management.BridgeManagementContract;
import network.bane.message.MessageBridgeContract;
import network.bane.messageexecution.ExecutionManagerContract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static network.bane.utils.deployment.BridgeDeploymentParameters.prepareBridgeDeployParameter;
import static network.bane.utils.deployment.BridgeDeploymentParameters.prepareExecutionManagerDeployParameter;
import static network.bane.utils.deployment.BridgeDeploymentParameters.prepareManagementDeployParameter;
import static network.bane.utils.deployment.BridgeDeploymentParameters.prepareMessageBridgeDeployParameter;
import static network.bane.utils.env.EnvVariables.*;

public class BridgeCompilation {

    public static Hash160 compileAndPrintManagementDeploymentTxData(Neow3j neow3j) throws Throwable {
        // Compile the Bridge Management contract
        CompilationUnit managementCompUnit = new io.neow3j.compiler.Compiler()
                .compile(BridgeManagementContract.class.getCanonicalName());

        if (MAX_NR_VALIDATORS > 7) {
            throw new Exception("Maximum number of validators cannot be greater than 7");
        }
        if (validator_threshold > nrValidators) {
            throw new Exception("Validator threshold cannot be greater than the number of validators");
        }
        if (validator_threshold < 2) {
            throw new Exception("Validator threshold cannot be less than 2");
        }
        List<ECKeyPair.ECPublicKey> completeValidatorList = asList(
                validator_1, validator_2, validator_3, validator_4, validator_5, validator_6, validator_7
        );
        List<ECKeyPair.ECPublicKey> validatorList = new ArrayList<>();
        // Only use the first n validators
        for (int i = 0; i < nrValidators; i++) {
            validatorList.add(completeValidatorList.get(i));
        }

        // Prepare the deployment parameter for the bridge management contract
        ContractParameter managementDeployParameter = prepareManagementDeployParameter(
                ownerAcc.getScriptHash(), relayer, validatorList, validator_threshold, governor, securityGuard
        );

        Hash160 managementContractHash = SmartContract.calcContractHash(
                deployerAcc.getScriptHash(),
                managementCompUnit.getNefFile().getCheckSumAsInteger(),
                managementCompUnit.getManifest().getName()
        );

        NeoSendRawTransaction managementDeploymentTxResponse = deployContractFromCompilationUnit(
                neow3j, managementCompUnit, managementDeployParameter, managementContractHash
        );
        Hash256 managementDeploymentTxHash = getHashIfExecutionSuccessful(neow3j, managementDeploymentTxResponse);

        ensureConsistentState(neow3j, managementContractHash, managementCompUnit, managementDeploymentTxHash);

        return managementContractHash;
    }

    public static void compileAndPrintBridgeDeploymentTxData(
            Neow3j neow3j,
            Hash160 managementContractHash
    ) throws Throwable {
        CompilationUnit bridgeCompUnit = new Compiler().compile(BridgeContract.class.getCanonicalName());

        ContractParameter bridgeDeploymentParameter = prepareBridgeDeployParameter(
                managementContractHash, depositFee, minDepositAmount, maxDepositAmount, maxTotalDeposited
        );

        Hash160 bridgeContractHash = SmartContract.calcContractHash(
                deployerAcc.getScriptHash(),
                bridgeCompUnit.getNefFile().getCheckSumAsInteger(),
                bridgeCompUnit.getManifest().getName()
        );

        NeoSendRawTransaction bridgeDeploymentTxResponse = deployContractFromCompilationUnit(
                neow3j, bridgeCompUnit, bridgeDeploymentParameter, bridgeContractHash
        );
        Hash256 bridgeDeploymentTxHash = getHashIfExecutionSuccessful(neow3j, bridgeDeploymentTxResponse);

        ensureConsistentState(neow3j, bridgeContractHash, bridgeCompUnit, bridgeDeploymentTxHash);
    }

    public static void compileAndPrintMessageBridgeDeploymentTxData(
            Neow3j neow3j,
            Hash160 managementContractHash
    ) throws Throwable {
        CompilationUnit msgBridgeCompUnit = new Compiler().compile(MessageBridgeContract.class.getCanonicalName());

        Hash160 msgBridgeContractHash = SmartContract.calcContractHash(
                deployerAcc.getScriptHash(),
                msgBridgeCompUnit.getNefFile().getCheckSumAsInteger(),
                msgBridgeCompUnit.getManifest().getName()
        );

        Hash160 executionManagerContractHash = compileAndPrintExecutionManagerDeploymentTxData(
                neow3j, managementContractHash, msgBridgeContractHash
        );

        ContractParameter bridgeDeploymentParameter = prepareMessageBridgeDeployParameter(
                linkedChainId, managementContractHash, executionManagerContractHash
        );

        NeoSendRawTransaction msgBridgeDeploymentTxResponse = deployContractFromCompilationUnit(
                neow3j, msgBridgeCompUnit, bridgeDeploymentParameter, msgBridgeContractHash
        );

        Hash256 msgBridgeDeploymentTxHash = getHashIfExecutionSuccessful(neow3j, msgBridgeDeploymentTxResponse);

        ensureConsistentState(neow3j, msgBridgeContractHash, msgBridgeCompUnit, msgBridgeDeploymentTxHash);
    }

    public static Hash160 compileAndPrintExecutionManagerDeploymentTxData(
            Neow3j neow3j,
            Hash160 managementContractHash,
            Hash160 messageBridgeContractHash
    ) throws Throwable {
        CompilationUnit execManagerCompUnit = new io.neow3j.compiler.Compiler().compile(
                ExecutionManagerContract.class.getCanonicalName()
        );

        Hash160 execManagerContractHash = SmartContract.calcContractHash(
                deployerAcc.getScriptHash(),
                execManagerCompUnit.getNefFile().getCheckSumAsInteger(),
                execManagerCompUnit.getManifest().getName()
        );

        ContractParameter execManagerDeployParameter = prepareExecutionManagerDeployParameter(
                managementContractHash, messageBridgeContractHash
        );

        NeoSendRawTransaction managementDeploymentTxResponse = deployContractFromCompilationUnit(
                neow3j, execManagerCompUnit, execManagerDeployParameter, execManagerContractHash
        );

        Hash256 managementDeploymentTxHash = getHashIfExecutionSuccessful(neow3j, managementDeploymentTxResponse);

        ensureConsistentState(neow3j, execManagerContractHash, execManagerCompUnit, managementDeploymentTxHash);
        return execManagerContractHash;
    }

    @NotNull
    private static NeoSendRawTransaction deployContractFromCompilationUnit(
            Neow3j neow3j,
            CompilationUnit compilationUnit,
            ContractParameter deployParameter,
            Hash160 allowedContractHash
    ) throws Throwable {
        // Build the deployment transaction
        Transaction deploymentTx = new ContractManagement(neow3j)
                .deploy(
                        compilationUnit.getNefFile(),
                        compilationUnit.getManifest(),
                        deployParameter
                ).signers(
                        AccountSigner.none(deployerAcc),
                        AccountSigner.none(ownerAcc).setAllowedContracts(allowedContractHash)
                ).sign();
        NeoSendRawTransaction deploymentTxResponse = deploymentTx.send();
        if (deploymentTxResponse.hasError()) {
            throw new Exception(
                    "Sent transaction resulted in an error: " + deploymentTxResponse.getError().getMessage());
        }
        return deploymentTxResponse;
    }

    private static Hash256 getHashIfExecutionSuccessful(
            Neow3j neow3j,
            NeoSendRawTransaction DeploymentTxResponse
    ) throws Exception {
        Hash256 deploymentTxHash = DeploymentTxResponse.getResult().getHash();
        Await.waitUntilTransactionIsExecuted(deploymentTxHash, neow3j);
        NeoApplicationLog deployLog = neow3j.getApplicationLog(deploymentTxHash).send().getApplicationLog();
        if (deployLog.getExecutions().get(0).getState().equals(NeoVMStateType.FAULT)) {
            throw new Exception(
                    format(
                            "Failed to deploy contract. NeoVM error message: %s",
                            deployLog.getExecutions().get(0).getException()
                    )
            );
        }
        return deploymentTxHash;
    }

    private static void ensureConsistentState(
            Neow3j neow3j,
            Hash160 contractHash,
            CompilationUnit compilationUnit,
            Hash256 deploymentTxHash
    ) throws Exception {
        // Get the contract hash from the deployment transaction
        ContractState managementState = neow3j.getContractState(contractHash).send().getContractState();
        String contractName = compilationUnit.getManifest().getName();
        if (!managementState.getNef().getChecksum().equals(compilationUnit.getNefFile().getCheckSumAsInteger())) {
            throw new Exception("Contract " + contractName + " NEF checksum mismatch");
        }
        if (!contractHash.equals(managementState.getHash())) {
            throw new Exception("Contract " + contractName + " hash mismatch");
        }

        System.out.printf("\nContract (%s) deployed successfully in transaction: %s\n", contractName, deploymentTxHash);
        System.out.println("Contract hash: " + contractHash);
    }
}
