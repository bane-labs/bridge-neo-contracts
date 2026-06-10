package network.bane.scripts.message.unpause;

import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.PrintHelper.printSender;
import static network.bane.utils.env.EnvVariables.MESSAGE_BRIDGE_HASH;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.env.EnvWallets.getGovernorAccountFromEnv;

public class UnpauseExecuting {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        Hash160 bridgeHash = getHash160FromEnvVar(MESSAGE_BRIDGE_HASH);

        Account governorAcc = getGovernorAccountFromEnv();

        System.out.println("Unpause executing in message bridge...");
        printNetwork(neow3j);
        printSender(governorAcc.getScriptHash());

        unpauseExecuting(neow3j, bridgeHash, governorAcc);
    }

    static void unpauseExecuting(Neow3j neow3j, Hash160 contractHash, Account governor) throws Throwable {
        SmartContract bridge = new SmartContract(contractHash, neow3j);
        if (!bridge.callFunctionReturningBool("executingIsPaused")) {
            System.out.println("\nMessage Executing is not paused - no action needed");
            return;
        }
        System.out.println("\nMessage Executing is paused, proceeding to unpause...");

        Transaction tx = bridge.invokeFunction("unpauseExecuting")
                .signers(calledByEntry(governor))
                .sign();

        NeoSendRawTransaction rawTxResponse = tx.send();
        if (rawTxResponse.hasError()) {
            throw new Exception("Error unpausing Message Executing: " + rawTxResponse.getError().getMessage());
        }
        Hash256 txHash = rawTxResponse.getSendRawTransaction().getHash();
        System.out.println("Transaction sent: " + txHash);
        waitUntilTransactionIsExecuted(txHash, neow3j);
        System.out.println("Transaction confirmed in block: " + neow3j.getTransactionHeight(txHash).send().getHeight());

        if (bridge.callFunctionReturningBool("executingIsPaused")) {
            throw new Exception("Message Executing is still paused");
        }
        System.out.println("Message Executing successfully unpaused");
    }

}
