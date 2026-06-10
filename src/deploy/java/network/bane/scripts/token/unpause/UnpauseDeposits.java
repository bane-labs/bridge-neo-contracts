package network.bane.scripts.token.unpause;

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
import static network.bane.utils.env.EnvVariables.BRIDGE_HASH;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.env.EnvWallets.getGovernorAccountFromEnv;

public class UnpauseDeposits {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        Hash160 bridgeHash = getHash160FromEnvVar(BRIDGE_HASH);

        Account governorAcc = getGovernorAccountFromEnv();

        System.out.println("Unpause deposits...");
        printNetwork(neow3j);
        printSender(governorAcc.getScriptHash());

        unpauseDeposits(neow3j, bridgeHash, governorAcc);
    }

    static void unpauseDeposits(Neow3j neow3j, Hash160 bridgeContractHash, Account governor) throws Throwable {
        SmartContract bridge = new SmartContract(bridgeContractHash, neow3j);
        if (!bridge.callFunctionReturningBool("depositsArePaused")) {
            System.out.println("\nDeposits are not paused - no action needed");
            return;
        }
        System.out.println("\nDeposits are paused, proceeding to unpause...");

        Transaction tx = bridge.invokeFunction("unpauseDeposits")
                .signers(calledByEntry(governor))
                .sign();

        NeoSendRawTransaction rawTxResponse = tx.send();
        if (rawTxResponse.hasError()) {
            throw new Exception("Error unpausing deposits: " + rawTxResponse.getError().getMessage());
        }
        Hash256 txHash = rawTxResponse.getSendRawTransaction().getHash();
        System.out.println("Transaction sent: " + txHash);
        waitUntilTransactionIsExecuted(txHash, neow3j);
        System.out.println("Transaction confirmed in block: " + neow3j.getTransactionHeight(txHash).send().getHeight());

        if (bridge.callFunctionReturningBool("depositsArePaused")) {
            throw new Exception("Deposits are still paused");
        }
        System.out.println("Deposits successfully unpaused");
    }

}
