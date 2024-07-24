package network.bane;

import io.neow3j.compiler.CompilationUnit;
import io.neow3j.compiler.Compiler;
import io.neow3j.contract.ContractManagement;
import io.neow3j.contract.SmartContract;
import io.neow3j.crypto.ECKeyPair;
import io.neow3j.crypto.ECKeyPair.ECPublicKey;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.ContractState;
import io.neow3j.protocol.core.response.NeoApplicationLog;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.types.NeoVMStateType;
import io.neow3j.utils.Await;
import io.neow3j.wallet.Account;
import network.bane.bridge.BridgeContract;
import network.bane.management.BridgeManagementContract;

import java.math.BigInteger;
import java.util.List;

import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.types.ContractParameter.publicKey;
import static java.lang.String.format;
import static java.util.Arrays.asList;

/**
 * This class is used to create the deployment transactions for the Bridge Management and Bridge in a private net.
 */
public class Deployment {

    private static final String NODE = getEnvVariableOrDefault("NEON3_JSON_RPC", "http://127.0.0.1:40332");

    private static final Account deployerAcc = Account.fromWIF(getEnvVariableOrDefault("NEON3_DEPLOYER_WIF", ""));
    private static final Hash160 deployer = deployerAcc.getScriptHash();

    private static final Account ownerAcc = Account.fromWIF(getEnvVariableOrDefault("NEON3_OWNER_WIF", ""));
    private static final Hash160 owner = ownerAcc.getScriptHash();

    private static final Hash160 relayer = new Hash160(
            getEnvVariableOrDefault("NEON3_RELAYER", "0x77ddabed5b98bef73b1592cb588118d2b8ab4a4c"));

    private static final ECPublicKey validator_1 = new ECPublicKey(
            getEnvVariableOrDefault("NEON3_VALIDATOR1_PUBKEY", "021fed0d208f2c4b2fe425571c36aea1d465159c93af349581896fe38553dffae1"));
    private static final ECPublicKey validator_2 = new ECPublicKey(
            getEnvVariableOrDefault("NEON3_VALIDATOR2_PUBKEY", "021986f90b2596322c9f681c42814237555732411e8c517100c702b91cd404f090"));
    private static final ECPublicKey validator_3 = new ECPublicKey(
            getEnvVariableOrDefault("NEON3_VALIDATOR3_PUBKEY", "021387fa5748344658778d9a3de2bd99f2133b96a3fe5ab2cda007bce3531f4d9f"));
    private static final ECPublicKey validator_4 = new ECPublicKey(
            getEnvVariableOrDefault("NEON3_VALIDATOR4_PUBKEY", "02108e50c32a9b4c12b90013b145370530a413a605ced93491ba2523d903295f45"));
    private static final ECPublicKey validator_5 = new ECPublicKey(
            getEnvVariableOrDefault("NEON3_VALIDATOR5_PUBKEY", "030187e4b19cddfa93f282c0bba9759446094c006a9f91c5e80963b2d9f8c4b568"));
    private static final ECPublicKey validator_6 = new ECPublicKey(
            getEnvVariableOrDefault("NEON3_VALIDATOR6_PUBKEY", "037c94e4ef2445f283ab7b02a1c26783ab39527404c157f4eb686edbdd0e651ade"));
    private static final ECPublicKey validator_7 = new ECPublicKey(
            getEnvVariableOrDefault("NEON3_VALIDATOR7_PUBKEY", "02189efe8f9d4fc34cceaae9fa79346525810143d5f198070594c7da12facfd4ab"));

    private static final int validator_threshold = Integer.parseInt(getEnvVariableOrDefault("NEON3_VALIDATOR_THRESHOLD", "5"));

    private static final Hash160 governor = new Hash160(
            getEnvVariableOrDefault("NEON3_GOVERNOR", "0x6d992a5e281480ba61dee66d3d008d774fa761ac"));

    private static final Hash160 securityGuard = new Hash160(
            getEnvVariableOrDefault("NEON3_SECURITYGUARD", "0x84bd782e66c5fde259f000f7208de674f8ceaf00"));

    // Parameters

    private static final BigInteger depositFee = new BigInteger(getEnvVariableOrDefault("NEON3_DEPOSIT_FEE", "10000000"));
    private static final BigInteger minDepositAmount = new BigInteger(getEnvVariableOrDefault("NEON3_MIN_DEPOSIT_AMOUNT", "100000000"));
    private static final BigInteger maxDepositAmount = new BigInteger(getEnvVariableOrDefault("NEON3_MAX_DEPOSIT_AMOUNT", "1000000000000"));

    public static String getEnvVariableOrDefault(String variableName, String defaultValue) {
        String value = System.getenv(variableName);
        return value != null ? value : defaultValue;
    }

    public static ContractParameter prepareManagementDeployParameter(
            Hash160 owner,
            Hash160 relayer,
            List<ECKeyPair.ECPublicKey> validators,
            Integer threshold,
            Hash160 governor,
            Hash160 securityGuard
    ) {
        return array(
                hash160(owner),
                hash160(relayer),
                array(
                        publicKey(validators.get(0)),
                        publicKey(validators.get(1)),
                        publicKey(validators.get(2)),
                        publicKey(validators.get(3)),
                        publicKey(validators.get(4)),
                        publicKey(validators.get(5)),
                        publicKey(validators.get(6))
                ),
                integer(threshold),
                hash160(governor),
                hash160(securityGuard)
        );
    }

    private static ContractParameter prepareBridgeDeployParameter(Hash160 managementContractHash,
            BigInteger depositFee, BigInteger minDeposit, BigInteger maxDeposit) {
        return array(
                hash160(managementContractHash),
                array(
                        integer(depositFee),
                        integer(minDeposit),
                        integer(maxDeposit),
                        integer(100)
                )
        );
    }

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = Neow3j.build(new HttpService(NODE, true));

        Hash160 managementHash = compileAndPrintManagementDeploymentTxData(neow3j);
        compileAndPrintBridgeDeploymentTxData(neow3j, managementHash);
    }

    private static Hash160 compileAndPrintManagementDeploymentTxData(Neow3j neow3j) throws Throwable {
        // Compile the Bridge Management contract
        CompilationUnit managementCompUnit = new Compiler().compile(BridgeManagementContract.class.getCanonicalName());

        // Prepare the deployment parameter for the bridge management contract
        ContractParameter managementDeployParameter = prepareManagementDeployParameter(
                owner,
                relayer,
                asList(
                        validator_1,
                        validator_2,
                        validator_3,
                        validator_4,
                        validator_5,
                        validator_6,
                        validator_7
                ),
                validator_threshold,
                governor,
                securityGuard
        );

        Hash160 managementContractHash = SmartContract.calcContractHash(deployer,
                managementCompUnit.getNefFile().getCheckSumAsInteger(),
                managementCompUnit.getManifest().getName());

        // Deploy the management contract
        Transaction managementDeploymentTx = new ContractManagement(neow3j)
                .deploy(
                        managementCompUnit.getNefFile(),
                        managementCompUnit.getManifest(),
                        managementDeployParameter)
                .signers(
                        AccountSigner.none(deployerAcc),
                        AccountSigner.none(ownerAcc).setAllowedContracts(managementContractHash)
                )
                .sign();
        NeoSendRawTransaction managementDeploymentTxResponse = managementDeploymentTx.send();
        if (managementDeploymentTxResponse.hasError()) {
            throw new Exception("Sent transaction resulted in an error: " +
                    managementDeploymentTxResponse.getError().getMessage());
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

        System.out.println("Management contract deployed successfully in transaction: " + managementDeploymentTxHash);
        System.out.println("Management contract hash: " + managementContractHash);
        return managementContractHash;
    }

    private static void compileAndPrintBridgeDeploymentTxData(Neow3j neow3j, Hash160 managementContractHash) throws Throwable {
        // Compile the Bridge Management contract
        CompilationUnit bridgeCompUnit = new Compiler().compile(BridgeContract.class.getCanonicalName());

        // Prepare the deployment parameter for the bridge contract
        ContractParameter bridgeDeploymentParameter = prepareBridgeDeployParameter(
                managementContractHash,
                depositFee,
                minDepositAmount,
                maxDepositAmount
        );

        Hash160 bridgeContractHash = SmartContract.calcContractHash(deployer,
                bridgeCompUnit.getNefFile().getCheckSumAsInteger(),
                bridgeCompUnit.getManifest().getName());
        // Build the deployment transaction
        Transaction bridgeDeploymentTx = new ContractManagement(neow3j)
                .deploy(bridgeCompUnit.getNefFile(), bridgeCompUnit.getManifest(), bridgeDeploymentParameter)
                .signers(
                        AccountSigner.none(deployerAcc),
                        AccountSigner.none(ownerAcc).setAllowedContracts(bridgeContractHash)
                )
                .sign();

        NeoSendRawTransaction bridgeDeploymentTxResponse = bridgeDeploymentTx.send();
        if (bridgeDeploymentTxResponse.hasError()) {
            throw new Exception("Sent transaction resulted in an error: " +
                    bridgeDeploymentTxResponse.getError().getMessage());
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
        if (!bridgeContractState.getNef().getChecksum().equals(bridgeCompUnit.getNefFile().getCheckSumAsInteger())) {
            throw new Exception("Bridge contract NEF checksum mismatch");
        }
        if (!bridgeContractHash.equals(bridgeContractState.getHash())) {
            throw new Exception("Bridge contract hash mismatch");
        }

        System.out.println("Bridge contract deployed successfully in transaction: " + bridgeDeploymentTxHash);
        System.out.println("Bridge Contract Script Hash: " + bridgeContractHash);
    }

}
