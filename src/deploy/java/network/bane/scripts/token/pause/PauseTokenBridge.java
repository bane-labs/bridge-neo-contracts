package network.bane.scripts.token.pause;

import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.PrintHelper.printSender;
import static network.bane.utils.env.EnvVariables.BRIDGE_HASH;
import static network.bane.utils.env.EnvVariables.PAUSE_TOKEN_HASH;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.env.EnvWallets.getGovernorAccountFromEnv;

public class PauseTokenBridge {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        Hash160 bridgeHash = getHash160FromEnvVar(BRIDGE_HASH);
        Hash160 tokenHash = getHash160FromEnvVar(PAUSE_TOKEN_HASH);

        Account governorAcc = getGovernorAccountFromEnv();

        System.out.println("Pause token bridge...");
        printNetwork(neow3j);
        printSender(governorAcc.getScriptHash());
        System.out.println("Token: " + tokenHash);

        pauseTokenBridge(neow3j, bridgeHash, tokenHash, governorAcc);
    }

    private static void pauseTokenBridge(Neow3j neow3j, Hash160 bridgeContractHash, Hash160 tokenHash,
            Account governor) throws Throwable {

        SmartContract bridge = new SmartContract(bridgeContractHash, neow3j);
        if (!bridge.callFunctionReturningBool("isRegisteredToken", hash160(tokenHash))) {
            System.out.printf("\nProvided token '%s' is not registered - no action needed\n", tokenHash);
            return;
        }
        boolean isPaused = bridge.callInvokeFunction("getTokenBridge", asList(hash160(tokenHash))).getInvocationResult()
                .getFirstStackItem().getList().get(0).getBoolean();
        if (isPaused) {
            System.out.printf("\nToken bridge '%s' is already paused - no action needed\n", tokenHash);
            return;
        }
        System.out.printf("\nToken bridge '%s' is not paused, proceeding to pause...\n", tokenHash);

        Transaction tx = bridge.invokeFunction("pauseTokenBridge", hash160(tokenHash))
                .signers(calledByEntry(governor))
                .sign();

        NeoSendRawTransaction rawTxResponse = tx.send();
        if (rawTxResponse.hasError()) {
            throw new Exception(format("Error pausing token bridge '%s': %s", tokenHash,
                    rawTxResponse.getError().getMessage()));
        }
        Hash256 txHash = rawTxResponse.getSendRawTransaction().getHash();
        System.out.println("Transaction sent: " + txHash);
        waitUntilTransactionIsExecuted(txHash, neow3j);
        System.out.println("Transaction confirmed in block: " + neow3j.getTransactionHeight(txHash).send().getHeight());

        isPaused = bridge.callInvokeFunction("getTokenBridge", asList(hash160(tokenHash))).getInvocationResult()
                .getFirstStackItem().getList().get(0).getBoolean();
        if (!isPaused) {
            throw new Exception(format("Token bridge '%s' is still not paused", tokenHash));
        }
        System.out.println("Token bridge successfully paused");
    }

}
