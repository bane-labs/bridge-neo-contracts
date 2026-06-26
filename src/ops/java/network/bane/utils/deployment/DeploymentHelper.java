package network.bane.utils.deployment;

import io.neow3j.compiler.CompilationUnit;
import io.neow3j.contract.ContractManagement;
import io.neow3j.contract.NefFile;
import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.ContractManifest;
import io.neow3j.protocol.core.response.ContractState;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.protocol.core.response.Notification;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;

import java.math.BigDecimal;
import java.util.Optional;

import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static java.lang.String.format;

public class DeploymentHelper {

    public static Hash160 calculateContractHash(CompilationUnit execManagerCompUnit, Account deploymentAccount) {
        String contractName = execManagerCompUnit.getManifest().getName();
        long nefChecksum = execManagerCompUnit.getNefFile().getCheckSumAsInteger();
        return SmartContract.calcContractHash(deploymentAccount.getScriptHash(), nefChecksum, contractName);
    }

    public static Hash256 deployContract(Neow3j neow3j, CompilationUnit compUnit, Account deploymentAccount,
            ContractParameter deployParam) throws Throwable {
        NefFile nefFile = compUnit.getNefFile();
        ContractManifest manifest = compUnit.getManifest();
        System.out.printf("\n📝 Deploying contract: %s%n", manifest.getName());
        Transaction tx = new ContractManagement(neow3j).deploy(nefFile, manifest, deployParam)
                .signers(AccountSigner.none(deploymentAccount))
                .sign();
        BigDecimal totalFee = new BigDecimal(tx.getNetworkFee() + tx.getSystemFee());
        System.out.printf("💰 Fee of $GAS %s for deployment transaction of %s contract%n",
                totalFee.divide(BigDecimal.valueOf(100_000_000)), manifest.getName());
        NeoSendRawTransaction response = tx.send();
        if (response.hasError()) {
            throw new Exception("Sent transaction resulted in an error: " + response.getError().getMessage());
        }
        System.out.println("⏳ Deployment transaction sent - waiting for confirmation...");
        Hash256 txHash = response.getSendRawTransaction().getHash();
        waitUntilTransactionIsExecuted(txHash, neow3j);
        System.out.println("☑️ Transaction " + txHash + " executed");
        return txHash;
    }

    public static void ensureConsistentState(Neow3j neow3j, Hash160 expectedHash, CompilationUnit compUnit,
            Hash256 deploymentTxHash) throws Exception {
        System.out.printf("🔍 Ensuring consistent state for contract '%s' at address %s%n",
                compUnit.getManifest().getName(), expectedHash);

        // Get the contract hash from the deployment transaction
        Optional<Notification> deployEvent = neow3j.getApplicationLog(deploymentTxHash).send().getApplicationLog()
                .getFirstExecution().getNotifications().stream()
                .filter(n -> n.getContract().equals(ContractManagement.SCRIPT_HASH))
                .filter(n -> n.getEventName().equals("Deploy"))
                .findFirst();
        if (!deployEvent.isPresent()) {
            throw new Exception("️‼️ No Deploy event found in the deployment transaction logs");
        }
        Hash160 actualDeployedContractHash = Hash160.fromAddress(
                deployEvent.get().getState().getList().get(0).getAddress());
        if (!expectedHash.equals(actualDeployedContractHash)) {
            throw new Exception(format("‼️ Deployed contract hash %s does not match expected hash %s",
                    actualDeployedContractHash, expectedHash));
        }

        ContractState contractState = neow3j.getContractState(actualDeployedContractHash).send().getContractState();
        // Check the contract name
        String contractName = compUnit.getManifest().getName();
        if (contractName.equals(contractState.getManifest().getName())) {
            System.out.println("✅ Contract name matches compiled contract name");
        } else {
            throw new Exception("‼️ Deployed contract name DOES NOT match compiled contract name!");
        }

        // Check the NEF checksum
        Long checksum = contractState.getNef().getChecksum();
        if (checksum.equals(compUnit.getNefFile().getCheckSumAsInteger())) {
            System.out.println("✅ NEF checksum matches compiled NEF checksum");
        } else {
            throw new Exception("‼️ Deployed contract NEF checksum DOES NOT match compiled NEF checksum!");
        }
    }

}
