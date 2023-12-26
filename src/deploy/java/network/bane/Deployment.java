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

import java.math.BigInteger;
import java.util.List;

import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.types.ContractParameter.publicKey;
import static java.lang.String.format;
import static java.util.Arrays.asList;

public class Deployment {

    private static final String NODE = "http://localhost:50012";

    private static final Account deployer = Account.fromWIF("");

    private static final ECPublicKey owner = new ECPublicKey("0x");

    private static final ECPublicKey relayer = new ECPublicKey("");

    private static final ECPublicKey validator_1 = new ECPublicKey("0x");
    private static final ECPublicKey validator_2 = new ECPublicKey("0x");
    private static final ECPublicKey validator_3 = new ECPublicKey("0x");
    private static final ECPublicKey validator_4 = new ECPublicKey("0x");
    private static final ECPublicKey validator_5 = new ECPublicKey("0x");
    private static final ECPublicKey validator_6 = new ECPublicKey("0x");
    private static final ECPublicKey validator_7 = new ECPublicKey("0x");

    private static final int validator_threshold = 5;

    private static final ECPublicKey recoverer = new ECPublicKey("0x");

    // Parameters

    private static final BigInteger depositFee = new BigInteger("10000000");
    private static final BigInteger minDepositAmount = new BigInteger("100000000");
    private static final BigInteger maxDepositAmount = new BigInteger("1000000000000");

    public static ContractParameter prepareManagementDeployParameter(
            ECKeyPair.ECPublicKey owner,
            ECKeyPair.ECPublicKey relayer,
            List<ECKeyPair.ECPublicKey> validators,
            Integer threshold,
            ECKeyPair.ECPublicKey recoverer
    ) {
        return array(
                publicKey(owner),
                publicKey(relayer),
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
                publicKey(recoverer)
        );
    }

    private static ContractParameter prepareBridgeDeployParameter(Hash160 managementContractHash,
            BigInteger depositFee, BigInteger minDeposit, BigInteger maxDeposit) {
        return array(
                hash160(managementContractHash),
                integer(depositFee),
                integer(minDeposit),
                integer(maxDeposit)
        );
    }

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = Neow3j.build(new HttpService(NODE));

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
                recoverer
        );

        // Deploy the management contract
        Transaction managementDeploymentTx = new ContractManagement(neow3j)
                .deploy(
                        managementCompUnit.getNefFile(),
                        managementCompUnit.getManifest(),
                        managementDeployParameter)
                .signers(AccountSigner.none(deployer))
                .sign();
        NeoSendRawTransaction managementDeploymentTxResponse = managementDeploymentTx.send();
        if (managementDeploymentTxResponse.hasError()) {
            throw new Exception("Sent transaction resulted in an error: " + managementDeploymentTxResponse.getError().getMessage());
        }

        Hash256 managementDeploymentTxHash = managementDeploymentTxResponse.getResult().getHash();
        Await.waitUntilTransactionIsExecuted(managementDeploymentTxHash, neow3j);
        NeoApplicationLog bridgeManagementDeployLog = neow3j.getApplicationLog(managementDeploymentTxHash).send().getApplicationLog();
        if (bridgeManagementDeployLog.getExecutions().get(0).getState().equals(NeoVMStateType.FAULT)) {
            throw new Exception(format("Failed to deploy contract. NeoVM error message: %s",
                    bridgeManagementDeployLog.getExecutions().get(0).getException()));
        }
        System.out.println("Management contract deployed successfully in transaction: " + managementDeploymentTxHash);

        // Get the contract hash from the deployment transaction
        Hash160 managementHash =
                SmartContract.calcContractHash(deployer.getScriptHash(), managementCompUnit.getNefFile().getCheckSumAsInteger(), managementCompUnit.getManifest().getName());
        System.out.println("Management contract hash: " + managementHash);
        ContractState managementState = neow3j.getContractState(managementHash).send().getContractState();
        if (!managementState.getNef().getChecksum().equals(managementCompUnit.getNefFile().getCheckSumAsInteger())) {
            throw new Exception("Management contract NEF checksum mismatch");
        }
        if (!managementHash.equals(managementState.getHash())) {
            throw new Exception("Management contract hash mismatch");
        }

        // Compile the Bridge Management contract
        CompilationUnit bridgeCompUnit = new Compiler().compile(BridgeContract.class.getCanonicalName());

        // Prepare the deployment parameter for the bridge contract
        ContractParameter bridgeDeploymentParameter = prepareBridgeDeployParameter(
                managementHash,
                depositFee,
                minDepositAmount,
                maxDepositAmount
        );


        // Build the deployment transaction
        Transaction bridgeDeploymentTx = new ContractManagement(neow3j)
                .deploy(bridgeCompUnit.getNefFile(), bridgeCompUnit.getManifest(), bridgeDeploymentParameter)
                .signers(AccountSigner.none(deployer))
                .sign();
        NeoSendRawTransaction bridgeDeploymentTxResponse = bridgeDeploymentTx.send();
        if (bridgeDeploymentTxResponse.hasError()) {
            throw new Exception("Sent transaction resulted in an error: " + bridgeDeploymentTxResponse.getError().getMessage());
        }
        Hash256 bridgeDeploymentTxHash = bridgeDeploymentTxResponse.getResult().getHash();
        Await.waitUntilTransactionIsExecuted(bridgeDeploymentTxHash, neow3j);
        NeoApplicationLog bridgeDeployLog = neow3j.getApplicationLog(bridgeDeploymentTxHash).send().getApplicationLog();
        if (bridgeDeployLog.getExecutions().get(0).getState().equals(NeoVMStateType.FAULT)) {
            throw new Exception(format("Failed to deploy contract. NeoVM error message: %s",
                    bridgeDeployLog.getExecutions().get(0).getException()));
        }

        // Get the contract hash from the deployment transaction
        Hash160 bridgeHash =
                SmartContract.calcContractHash(deployer.getScriptHash(), bridgeCompUnit.getNefFile().getCheckSumAsInteger(), bridgeCompUnit.getManifest().getName());
        System.out.println("Bridge contract hash: " + bridgeHash);
        ContractState bridgeState = neow3j.getContractState(bridgeHash).send().getContractState();
        if (!bridgeState.getNef().getChecksum().equals(bridgeCompUnit.getNefFile().getCheckSumAsInteger())) {
            throw new Exception("Bridge contract NEF checksum mismatch");
        }
        if (!bridgeHash.equals(bridgeState.getHash())) {
            throw new Exception("Bridge contract hash mismatch");
        }

        System.out.println("Bridge contract deployed successfully in transaction: " + bridgeDeploymentTxHash);
    }

}
