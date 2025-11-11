package network.bane.scripts.message.pause;

import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static network.bane.utils.env.EnvVariables.MESSAGE_BRIDGE_HASH;
import static network.bane.utils.env.EnvVariables.WALLET_FILEPATH_GOVERNOR;
import static network.bane.utils.env.EnvVariables.WALLET_PASSWORD_GOVERNOR;
import static network.bane.utils.env.EnvVariables.getEnvVariable;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.wallet.LoadWallet.getAccountFromWallet;

public class Pause {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        Hash160 bridgeHash = getHash160FromEnvVar(MESSAGE_BRIDGE_HASH);

        String governorWalletFilepath = getEnvVariable(WALLET_FILEPATH_GOVERNOR);
        String governorWalletPassword = getEnvVariable(WALLET_PASSWORD_GOVERNOR);

        Account governor = getAccountFromWallet(governorWalletFilepath, governorWalletPassword);

        pause(neow3j, bridgeHash, governor);
    }

    private static void pause(Neow3j neow3j, Hash160 contractHash, Account governor) throws Throwable {
        SmartContract bridge = new SmartContract(contractHash, neow3j);
        if (bridge.callFunctionReturningBool("isPaused")) {
            System.out.println("\nMessageBridge is already paused - no action needed");
            return;
        }
        System.out.println("\nMessageBridge is not paused, proceeding to pause...");

        Transaction tx = bridge.invokeFunction("pause")
                .signers(calledByEntry(governor))
                .sign();

        NeoSendRawTransaction rawTxResponse = tx.send();
        if (rawTxResponse.hasError()) {
            throw new Exception("Error pausing MessageBridge: " + rawTxResponse.getError().getMessage());
        }
        Hash256 txHash = rawTxResponse.getSendRawTransaction().getHash();
        System.out.println("Transaction sent: " + txHash);
        waitUntilTransactionIsExecuted(txHash, neow3j);
        System.out.println("Transaction confirmed in block: " + neow3j.getTransactionHeight(txHash).send().getHeight());

        if (!bridge.callFunctionReturningBool("isPaused")) {
            throw new Exception("MessageBridge is still not paused");
        }
        System.out.println("MessageBridge successfully paused");
    }

}
