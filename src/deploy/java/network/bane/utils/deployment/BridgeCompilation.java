package network.bane.utils.deployment;

import io.neow3j.compiler.CompilationUnit;
import io.neow3j.compiler.Compiler;
import io.neow3j.contract.ContractManagement;
import io.neow3j.contract.SmartContract;
import io.neow3j.crypto.ECKeyPair;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoApplicationLog;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.types.NeoVMStateType;
import network.bane.bridge.BridgeContract;
import network.bane.management.BridgeManagementContract;
import network.bane.message.MessageBridgeContract;
import network.bane.messageexecution.ExecutionManagerContract;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static network.bane.utils.deployment.BridgeDeploymentParameters.prepareBridgeDeployParameter;
import static network.bane.utils.deployment.BridgeDeploymentParameters.prepareManagementDeployParameter;
import static network.bane.utils.deployment.DeploymentHelper.ensureConsistentState;
import static network.bane.utils.env.EnvVariables.MAX_NR_VALIDATORS;
import static network.bane.utils.env.EnvVariables.deployerAcc;
import static network.bane.utils.env.EnvVariables.governor;
import static network.bane.utils.env.EnvVariables.linkedChainId;
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
        substitutions.put("BridgeManagementName", "BridgeManagementContract");
        CompilationUnit managementCompUnit = new io.neow3j.compiler.Compiler().compile(
                BridgeManagementContract.class.getCanonicalName(), substitutions);

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

        Hash160 managementContractHash = SmartContract.calcContractHash(deployerAcc.getScriptHash(),
                managementCompUnit.getNefFile().getCheckSumAsInteger(), managementCompUnit.getManifest().getName());

        NeoSendRawTransaction managementDeploymentTxResponse = deployContractFromCompilationUnit(neow3j,
                managementCompUnit, managementDeployParameter, managementContractHash);
        Hash256 managementDeploymentTxHash = getHashIfExecutionSuccessful(neow3j, managementDeploymentTxResponse);

        ensureConsistentState(neow3j, managementContractHash, managementCompUnit, managementDeploymentTxHash);

        return managementContractHash;
    }

    public static Hash160 compileAndPrintBridgeDeploymentTxData(Neow3j neow3j, Hash160 managementContractHash)
            throws Throwable {
        HashMap<String, String> substitutions = new HashMap<>();
        substitutions.put("BridgeName", "BridgeContract");
        CompilationUnit bridgeCompUnit = new Compiler().compile(BridgeContract.class.getCanonicalName(), substitutions);

        ContractParameter bridgeDeploymentParameter = prepareBridgeDeployParameter(linkedChainId,
                managementContractHash);

        Hash160 bridgeContractHash = SmartContract.calcContractHash(deployerAcc.getScriptHash(),
                bridgeCompUnit.getNefFile().getCheckSumAsInteger(), bridgeCompUnit.getManifest().getName());

        NeoSendRawTransaction bridgeDeploymentTxResponse = deployContractFromCompilationUnit(neow3j, bridgeCompUnit,
                bridgeDeploymentParameter, bridgeContractHash);
        Hash256 bridgeDeploymentTxHash = getHashIfExecutionSuccessful(neow3j, bridgeDeploymentTxResponse);

        ensureConsistentState(neow3j, bridgeContractHash, bridgeCompUnit, bridgeDeploymentTxHash);
        return bridgeContractHash;
    }

    @NotNull
    private static NeoSendRawTransaction deployContractFromCompilationUnit(Neow3j neow3j,
            CompilationUnit compilationUnit, ContractParameter deployParameter, Hash160 allowedContractHash)
            throws Throwable {
        // Build the deployment transaction
        Transaction deploymentTx = new ContractManagement(neow3j).deploy(compilationUnit.getNefFile(),
                compilationUnit.getManifest(), deployParameter).signers(AccountSigner.none(deployerAcc),
                AccountSigner.none(ownerAcc).setAllowedContracts(allowedContractHash)).sign();
        BigDecimal totalFee = new BigDecimal(deploymentTx.getNetworkFee() + deploymentTx.getSystemFee());
        System.out.printf("💰 Fee of $GAS %s for deployment transaction of %s contract%n",
                totalFee.divide(BigDecimal.valueOf(100_000_000)), compilationUnit.getManifest().getName());
        NeoSendRawTransaction deploymentTxResponse = deploymentTx.send();
        if (deploymentTxResponse.hasError()) {
            throw new Exception(
                    "Sent transaction resulted in an error: " + deploymentTxResponse.getError().getMessage());
        }
        return deploymentTxResponse;
    }

    private static Hash256 getHashIfExecutionSuccessful(Neow3j neow3j, NeoSendRawTransaction DeploymentTxResponse)
            throws Exception {
        Hash256 deploymentTxHash = DeploymentTxResponse.getResult().getHash();
        waitUntilTransactionIsExecuted(deploymentTxHash, neow3j);
        NeoApplicationLog deployLog = neow3j.getApplicationLog(deploymentTxHash).send().getApplicationLog();
        if (deployLog.getExecutions().get(0).getState().equals(NeoVMStateType.FAULT)) {
            throw new Exception(format("Failed to deploy contract. NeoVM error message: %s",
                    deployLog.getExecutions().get(0).getException()));
        }
        return deploymentTxHash;
    }

    public static CompilationUnit compileMessageBridge() throws Throwable {
        return new Compiler().compile(MessageBridgeContract.class.getCanonicalName());
    }

    public static CompilationUnit compileExecutionManager() throws Throwable {
        return new Compiler().compile(ExecutionManagerContract.class.getCanonicalName());
    }

}
