package network.bane.utils.deployment;

import io.neow3j.compiler.CompilationUnit;
import io.neow3j.compiler.Compiler;
import io.neow3j.contract.ContractManagement;
import io.neow3j.contract.NefFile;
import io.neow3j.contract.SmartContract;
import io.neow3j.crypto.ECKeyPair;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.ContractManifest;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static network.bane.utils.deployment.BridgeDeploymentParameters.prepareBridgeDeployParameter;
import static network.bane.utils.deployment.BridgeDeploymentParameters.prepareManagementDeployParameter;
import static network.bane.utils.env.EnvVariables.MAX_NR_VALIDATORS;
import static network.bane.utils.env.EnvVariables.bridgeContractName;
import static network.bane.utils.env.EnvVariables.deployerAcc;
import static network.bane.utils.env.EnvVariables.depositFee;
import static network.bane.utils.env.EnvVariables.governor;
import static network.bane.utils.env.EnvVariables.managementContractName;
import static network.bane.utils.env.EnvVariables.maxDepositAmount;
import static network.bane.utils.env.EnvVariables.maxTotalDeposited;
import static network.bane.utils.env.EnvVariables.minDepositAmount;
import static network.bane.utils.env.EnvVariables.nrValidators;
import static network.bane.utils.env.EnvVariables.ownerAcc;
import static network.bane.utils.env.EnvVariables.relayer;
import static network.bane.utils.env.EnvVariables.securityGuard;
import static network.bane.utils.env.EnvVariables.validator_1;
import static network.bane.utils.env.EnvVariables.validator_2;
import static network.bane.utils.env.EnvVariables.validator_3;
import static network.bane.utils.env.EnvVariables.validator_4;
import static network.bane.utils.env.EnvVariables.validator_5;
import static network.bane.utils.env.EnvVariables.validator_6;
import static network.bane.utils.env.EnvVariables.validator_7;
import static network.bane.utils.env.EnvVariables.validator_threshold;

public class BridgeCompilation {

    public static Hash160 compileAndPrintManagementDeploymentTxData(Neow3j neow3j) throws Throwable {
        // Compile the Bridge Management contract
        HashMap<String, String> substitutions = new HashMap<>();
        substitutions.put("ContractName", managementContractName);
        CompilationUnit managementCompUnit = new io.neow3j.compiler.Compiler()
                .compile(BridgeManagementContract.class.getCanonicalName(), substitutions);

        if (MAX_NR_VALIDATORS > 7) {
            throw new Exception("Maximum number of validators cannot be greater than 7");
        }
        if (validator_threshold > nrValidators) {
            throw new Exception("Validator threshold cannot be greater than the number of validators");
        }
        if (validator_threshold < 2) {
            throw new Exception("Validator threshold cannot be less than 2");
        }
        List<ECKeyPair.ECPublicKey> completeValidatorList = asList(validator_1, validator_2, validator_3, validator_4,
                validator_5, validator_6, validator_7);
        List<ECKeyPair.ECPublicKey> validatorList = new ArrayList<>();
        // Only use the first n validators
        for (int i = 0; i < nrValidators; i++) {
            validatorList.add(completeValidatorList.get(i));
        }

        // Prepare the deployment parameter for the bridge management contract
        ContractParameter managementDeployParameter = prepareManagementDeployParameter(ownerAcc.getScriptHash(),
                relayer, validatorList, validator_threshold, governor, securityGuard);

        Hash160 managementContractHash =
                SmartContract.calcContractHash(deployerAcc.getScriptHash(),
                        managementCompUnit.getNefFile().getCheckSumAsInteger(),
                        managementCompUnit.getManifest().getName());

        // Deploy the management contract
        Transaction managementDeploymentTx = new ContractManagement(neow3j)
                .deploy(
                        managementCompUnit.getNefFile(),
                        managementCompUnit.getManifest(),
                        managementDeployParameter
                ).signers(
                        AccountSigner.none(deployerAcc),
                        AccountSigner.none(ownerAcc).setAllowedContracts(managementContractHash)
                ).sign();
        NeoSendRawTransaction managementDeploymentTxResponse = managementDeploymentTx.send();
        if (managementDeploymentTxResponse.hasError()) {
            throw new Exception(
                    "Sent transaction resulted in an error: " + managementDeploymentTxResponse.getError().getMessage());
        }

        Hash256 managementDeploymentTxHash = managementDeploymentTxResponse.getResult().getHash();
        Await.waitUntilTransactionIsExecuted(managementDeploymentTxHash, neow3j);
        NeoApplicationLog bridgeManagementDeployLog =
                neow3j.getApplicationLog(managementDeploymentTxHash).send().getApplicationLog();
        if (bridgeManagementDeployLog.getExecutions().get(0).getState().equals(NeoVMStateType.FAULT)) {
            throw new Exception(format("Failed to deploy contract. NeoVM error message: %s",
                    bridgeManagementDeployLog.getExecutions().get(0).getException()));
        }

        // Get the contract hash from the deployment transaction
        ContractState managementState = neow3j.getContractState(managementContractHash).send().getContractState();
        if (!managementState.getNef().getChecksum().equals(managementCompUnit.getNefFile().getCheckSumAsInteger())) {
            throw new Exception("Management contract NEF checksum mismatch");
        }
        if (!managementContractHash.equals(managementState.getHash())) {
            throw new Exception("Management contract hash mismatch");
        }

        System.out.printf("\nManagement contract (%s) deployed successfully in transaction: %s\n",
                managementContractName, managementDeploymentTxHash);
        System.out.println("Management contract hash: " + managementContractHash);
        return managementContractHash;
    }

    public static void compileAndPrintBridgeDeploymentTxData(Neow3j neow3j, Hash160 managementContractHash) throws Throwable {
        // Compile the Bridge contract
        HashMap<String, String> substitutions = new HashMap<>();
        substitutions.put("ContractName", bridgeContractName);
        CompilationUnit bridgeCompUnit = new Compiler().compile(BridgeContract.class.getCanonicalName(), substitutions);

        // Prepare the deployment parameter for the bridge contract
        ContractParameter bridgeDeploymentParameter = prepareBridgeDeployParameter(managementContractHash, depositFee,
                minDepositAmount, maxDepositAmount, maxTotalDeposited);

        NefFile nefFile = bridgeCompUnit.getNefFile();
        ContractManifest bridgeManifest = bridgeCompUnit.getManifest();
        Hash160 bridgeContractHash =
                SmartContract.calcContractHash(deployerAcc.getScriptHash(), nefFile.getCheckSumAsInteger(),
                        bridgeManifest.getName());
        // Build the deployment transaction
        Transaction bridgeDeploymentTx = new ContractManagement(neow3j)
                .deploy(nefFile, bridgeManifest, bridgeDeploymentParameter)
                .signers(
                        AccountSigner.none(deployerAcc),
                        AccountSigner.none(ownerAcc).setAllowedContracts(bridgeContractHash)
                ).sign();

        NeoSendRawTransaction bridgeDeploymentTxResponse = bridgeDeploymentTx.send();
        if (bridgeDeploymentTxResponse.hasError()) {
            throw new Exception(
                    "Sent transaction resulted in an error: " + bridgeDeploymentTxResponse.getError().getMessage());
        }

        Hash256 bridgeDeploymentTxHash = bridgeDeploymentTxResponse.getResult().getHash();
        Await.waitUntilTransactionIsExecuted(bridgeDeploymentTxHash, neow3j);
        NeoApplicationLog bridgeDeployLog = neow3j.getApplicationLog(bridgeDeploymentTxHash).send().getApplicationLog();
        if (bridgeDeployLog.getExecutions().get(0).getState().equals(NeoVMStateType.FAULT)) {
            throw new Exception(format("Failed to deploy contract. NeoVM error message: %s",
                    bridgeDeployLog.getExecutions().get(0).getException()));
        }

        // Get the contract hash from the deployment transaction
        ContractState bridgeContractState = neow3j.getContractState(bridgeContractHash).send().getContractState();
        if (!bridgeContractState.getNef().getChecksum().equals(nefFile.getCheckSumAsInteger())) {
            throw new Exception("Bridge contract NEF checksum mismatch");
        }
        if (!bridgeContractHash.equals(bridgeContractState.getHash())) {
            throw new Exception("Bridge contract hash mismatch");
        }

        System.out.printf("Bridge contract (%s) deployed successfully in transaction: %s\n", bridgeContractName,
                bridgeDeploymentTxHash);
        System.out.println("Bridge Contract Script Hash: " + bridgeContractHash);
    }

}
