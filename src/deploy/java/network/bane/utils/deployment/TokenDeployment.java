package network.bane.utils.deployment;

import io.neow3j.compiler.CompilationUnit;
import io.neow3j.contract.ContractManagement;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;

import static io.neow3j.transaction.AccountSigner.none;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static network.bane.utils.env.EnvVariables.WALLET_FILEPATH_DEPLOYER;
import static network.bane.utils.env.EnvVariables.WALLET_PASSWORD_DEPLOYER;
import static network.bane.utils.wallet.LoadWallet.getAccountFromWallet;

public class TokenDeployment {

    private static final String deployerWalletFilepath = WALLET_FILEPATH_DEPLOYER;
    private static final String deployerWalletPassword = WALLET_PASSWORD_DEPLOYER;

    public static void deployContract(Neow3j neow3j, CompilationUnit compUnit) throws Throwable {
        Account deployerAcc = getAccountFromWallet(deployerWalletFilepath, deployerWalletPassword);

        Transaction tx = new ContractManagement(neow3j)
                .deploy(compUnit.getNefFile(), compUnit.getManifest(), hash160(deployerAcc))
                .signers(none(deployerAcc))
                .sign();
        NeoSendRawTransaction rawTxResponse = tx.send();
        if (rawTxResponse.hasError()) {
            throw new Exception("Error deploying contract: " + rawTxResponse.getError().getMessage());
        }
        Hash256 txHash = rawTxResponse.getSendRawTransaction().getHash();

        waitUntilTransactionIsExecuted(txHash, neow3j);
        System.out.println("Contract deployed successfully.");
        System.out.println("Transaction hash: " + txHash);

        Hash160 tokenHash = tx.getApplicationLog().getFirstExecution().getFirstNotification().getContract();
        System.out.println("Contract hash: " + tokenHash);
    }

}
