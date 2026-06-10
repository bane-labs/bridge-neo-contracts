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
import io.neow3j.wallet.Account;
import network.bane.bridge.BridgeContract;
import network.bane.management.BridgeManagementContract;
import network.bane.message.MessageBridgeContract;
import network.bane.messageexecution.ExecutionManagerContract;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static network.bane.utils.deployment.BridgeDeploymentParameters.prepareBridgeDeployParameter;
import static network.bane.utils.deployment.BridgeDeploymentParameters.prepareManagementDeployParameter;
import static network.bane.utils.deployment.DeploymentHelper.ensureConsistentState;
import static network.bane.utils.env.EnvVariables.BRIDGE_CONTRACT_NAME;
import static network.bane.utils.env.EnvVariables.MANAGEMENT_CONTRACT_NAME;
import static network.bane.utils.env.EnvVariables.ROLE_OWNER_ADDRESS;
import static network.bane.utils.env.EnvVariables.WALLET_FILEPATH_OWNER;
import static network.bane.utils.env.EnvVariables.WALLET_PASSWORD_DEPLOYER;
import static network.bane.utils.env.EnvVariables.WALLET_FILEPATH_DEPLOYER;
import static network.bane.utils.env.EnvVariables.LINKED_CHAIN_ID;
import static network.bane.utils.env.EnvVariables.MANAGEMENT_NUMBER_OF_VALIDATORS;
import static network.bane.utils.env.EnvVariables.MANAGEMENT_VALIDATOR_THRESHOLD;
import static network.bane.utils.env.EnvVariables.ROLE_GOVERNOR_ADDRESS;
import static network.bane.utils.env.EnvVariables.ROLE_RELAYER_ADDRESS;
import static network.bane.utils.env.EnvVariables.ROLE_SECURITY_GUARD_ADDRESS;
import static network.bane.utils.env.EnvVariables.ROLE_VALIDATOR_01_PUBLIC_KEY;
import static network.bane.utils.env.EnvVariables.ROLE_VALIDATOR_02_PUBLIC_KEY;
import static network.bane.utils.env.EnvVariables.ROLE_VALIDATOR_03_PUBLIC_KEY;
import static network.bane.utils.env.EnvVariables.ROLE_VALIDATOR_04_PUBLIC_KEY;
import static network.bane.utils.env.EnvVariables.ROLE_VALIDATOR_05_PUBLIC_KEY;
import static network.bane.utils.env.EnvVariables.ROLE_VALIDATOR_06_PUBLIC_KEY;
import static network.bane.utils.env.EnvVariables.ROLE_VALIDATOR_07_PUBLIC_KEY;
import static network.bane.utils.env.EnvVariables.WALLET_PASSWORD_OWNER;
import static network.bane.utils.env.EnvVariables.getAddressFromEnv;
import static network.bane.utils.env.EnvVariables.getBigIntegerFromEnvVar;
import static network.bane.utils.env.EnvVariables.getEnvVariable;
import static network.bane.utils.env.EnvVariables.getIntegerFromEnvVar;
import static network.bane.utils.env.EnvVariables.getPublicKeyFromEnvVar;
import static network.bane.utils.env.EnvWallets.getDeployerAccountFromEnv;
import static network.bane.utils.wallet.LoadWallet.getAccountFromWallet;

public class BridgeCompilation {

    private static final int MAX_NR_VALIDATORS = 7;

    public static Hash160 compileAndPrintManagementDeploymentTxData(Neow3j neow3j) throws Throwable {
        String managementContractName = getEnvVariable(MANAGEMENT_CONTRACT_NAME);

        int validatorThreshold = getIntegerFromEnvVar(MANAGEMENT_VALIDATOR_THRESHOLD);
        int nrValidators = getIntegerFromEnvVar(MANAGEMENT_NUMBER_OF_VALIDATORS);
        Hash160 owner = getAddressFromEnv(ROLE_OWNER_ADDRESS);
        Hash160 relayer = getAddressFromEnv(ROLE_RELAYER_ADDRESS);
        Hash160 governor = getAddressFromEnv(ROLE_GOVERNOR_ADDRESS);
        Hash160 securityGuard = getAddressFromEnv(ROLE_SECURITY_GUARD_ADDRESS);

        // Compile the Bridge Management contract
        HashMap<String, String> substitutions = new HashMap<>();
        substitutions.put("BridgeManagementName", managementContractName);
        CompilationUnit managementCompUnit = new io.neow3j.compiler.Compiler().compile(
                BridgeManagementContract.class.getCanonicalName(), substitutions);

        if (MAX_NR_VALIDATORS > 7) {
            throw new Exception("Maximum number of validators cannot be greater than 7");
        }
        if (validatorThreshold > nrValidators) {
            throw new Exception("Validator threshold cannot be greater than the number of validators");
        }
        if (validatorThreshold < 2) {
            throw new Exception("Validator threshold cannot be less than 2");
        }
        List<String> validatorEnvVars = asList(
                ROLE_VALIDATOR_01_PUBLIC_KEY,
                ROLE_VALIDATOR_02_PUBLIC_KEY,
                ROLE_VALIDATOR_03_PUBLIC_KEY,
                ROLE_VALIDATOR_04_PUBLIC_KEY,
                ROLE_VALIDATOR_05_PUBLIC_KEY,
                ROLE_VALIDATOR_06_PUBLIC_KEY,
                ROLE_VALIDATOR_07_PUBLIC_KEY
        );
        List<ECKeyPair.ECPublicKey> validatorList = new ArrayList<>();
        // Only use the first n validators
        for (int i = 0; i < nrValidators; i++) {
            validatorList.add(getPublicKeyFromEnvVar(validatorEnvVars.get(i)));
        }

        Account deployerAcc = getDeployerAccountFromEnv();

        // Prepare the deployment parameter for the bridge management contract
        ContractParameter managementDeployParameter = prepareManagementDeployParameter(owner,
                relayer, validatorList, validatorThreshold, governor, securityGuard);

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
        String deployerWalletPath = getEnvVariable(WALLET_FILEPATH_DEPLOYER);
        String deployerWalletPassword = getEnvVariable(WALLET_PASSWORD_DEPLOYER);
        String bridgeContractName = getEnvVariable(BRIDGE_CONTRACT_NAME);
        BigInteger linkedChainId = getBigIntegerFromEnvVar(LINKED_CHAIN_ID);

        HashMap<String, String> substitutions = new HashMap<>();
        substitutions.put("BridgeName", bridgeContractName);
        CompilationUnit bridgeCompUnit = new Compiler().compile(BridgeContract.class.getCanonicalName(), substitutions);

        ContractParameter bridgeDeploymentParameter = prepareBridgeDeployParameter(linkedChainId,
                managementContractHash);

        Account deployerAcc = getAccountFromWallet(deployerWalletPath, deployerWalletPassword);
        Hash160 bridgeContractHash = SmartContract.calcContractHash(deployerAcc.getScriptHash(),
                bridgeCompUnit.getNefFile().getCheckSumAsInteger(), bridgeCompUnit.getManifest().getName());

        NeoSendRawTransaction bridgeDeploymentTxResponse = deployContractFromCompilationUnit(neow3j, bridgeCompUnit,
                bridgeDeploymentParameter, bridgeContractHash);
        Hash256 bridgeDeploymentTxHash = getHashIfExecutionSuccessful(neow3j, bridgeDeploymentTxResponse);

        ensureConsistentState(neow3j, bridgeContractHash, bridgeCompUnit, bridgeDeploymentTxHash);
        return bridgeContractHash;
    }

    @NotNull
    private static NeoSendRawTransaction deployContractFromCompilationUnit(Neow3j neow3j, CompilationUnit compUnit,
            ContractParameter deployParameter, Hash160 allowedContractHash) throws Throwable {
        String ownerWalletPath = getEnvVariable(WALLET_FILEPATH_OWNER);
        String ownerWalletPassword = getEnvVariable(WALLET_PASSWORD_OWNER);
        String deployerWalletPath = getEnvVariable(WALLET_FILEPATH_DEPLOYER);
        String deployerWalletPassword = getEnvVariable(WALLET_PASSWORD_DEPLOYER);

        Account ownerAcc = getAccountFromWallet(ownerWalletPath, ownerWalletPassword);
        Account deployerAcc = getAccountFromWallet(deployerWalletPath, deployerWalletPassword);
        // Build the deployment transaction
        Transaction deploymentTx = new ContractManagement(neow3j).deploy(compUnit.getNefFile(),
                compUnit.getManifest(), deployParameter).signers(AccountSigner.none(deployerAcc),
                AccountSigner.none(ownerAcc).setAllowedContracts(allowedContractHash)).sign();
        BigDecimal totalFee = new BigDecimal(deploymentTx.getNetworkFee() + deploymentTx.getSystemFee());
        System.out.printf("💰 Fee of $GAS %s for deployment transaction of %s contract%n",
                totalFee.divide(BigDecimal.valueOf(100_000_000)), compUnit.getManifest().getName());
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
