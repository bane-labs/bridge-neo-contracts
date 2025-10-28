package network.bane.scripts.message;

import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static network.bane.utils.env.EnvVariables.NODE;
import static network.bane.utils.env.GetEnv.getEnvVariable;
import static network.bane.utils.wallet.LoadWallet.getGovernorAccountFromWallet;

/**
 * This class makes sure everything in the message bridge contract is unpaused. If something is paused, it sends a
 * transaction unpausing it. This includes:
 * - Unpausing message bridge overall
 * - Unpausing sending messages
 * - Unpausing executing messages
 */
public class UnpauseAll {

    private static final String ALREADY_UNPAUSED = "Already unpaused";
    private static final String SUCCESS = "Successful";

    public static void main(String[] args) throws Throwable {
        String overallPauseState;
        String sendingPauseState;
        String executingPauseState;

        System.out.println("Unpausing MessageBridge");

        Neow3j neow3j = Neow3j.build(new HttpService(NODE));

        Hash160 msgBridgeHash = new Hash160(getEnvVariable("MESSAGE_BRIDGE_HASH"));
        System.out.println("Unpausing MessageBridge at address: " + msgBridgeHash);
        Account governor = getGovernorAccountFromWallet();
        System.out.println("Governor address: " + governor.getScriptHash());

        System.out.println("\nAttempting unpause operations...");
        System.out.println("Note: Operations will be skipped automatically if components are already unpaused.");

        SmartContract msgBridge = new SmartContract(msgBridgeHash, neow3j);
        System.out.println("\nOverall bridge pausing");
        if (!msgBridge.callFunctionReturningBool("isPaused")) {
            overallPauseState = ALREADY_UNPAUSED;
            System.out.println("MessageBridge is not paused - no action needed");
        } else {
            System.out.println("MessageBridge is paused - unpausing...");
            Transaction tx = msgBridge.invokeFunction("unpause").signers(calledByEntry(governor)).sign();
            NeoSendRawTransaction response = tx.send();
            if (response.hasError()) {
                throw new Exception("Error unpausing message bridge: " + response.getError().getMessage());
            }
            Hash256 txHash = response.getSendRawTransaction().getHash();
            System.out.println("Transaction sent: " + txHash);
            waitUntilTransactionIsExecuted(txHash, neow3j);
            if (msgBridge.callFunctionReturningBool("isPaused")) {
                throw new Exception("Unpausing the message bridge contract failed in transaction: " + txHash);
            }
            overallPauseState = SUCCESS;
            System.out.println("Transaction confirmed");
            System.out.println("MessageBridge unpaused successfully");
        }

        // Unpause sending
        System.out.println("\nUnpausing sending");
        if (!msgBridge.callFunctionReturningBool("sendingIsPaused")) {
            sendingPauseState = ALREADY_UNPAUSED;
            System.out.println("Sending is not paused - no action needed");
        } else {
            System.out.println("Sending is paused - unpausing...");
            Transaction tx = msgBridge.invokeFunction("unpauseSending").signers(calledByEntry(governor)).sign();
            NeoSendRawTransaction response = tx.send();
            if (response.hasError()) {
                throw new Exception("Error unpausing sending: " + response.getError().getMessage());
            }
            Hash256 txHash = response.getSendRawTransaction().getHash();
            System.out.println("Transaction sent: " + txHash);
            waitUntilTransactionIsExecuted(txHash, neow3j);
            if (msgBridge.callFunctionReturningBool("sendingIsPaused")) {
                throw new Exception("Unpausing sending failed in transaction: " + txHash);
            }
            sendingPauseState = SUCCESS;
            System.out.println("Transaction confirmed");
            System.out.println("Sending unpaused successfully");
        }

        // Unpause executing
        System.out.println("\nUnpausing executing");
        if (!msgBridge.callFunctionReturningBool("executingIsPaused")) {
            executingPauseState = ALREADY_UNPAUSED;
            System.out.println("Executing is not paused - no action needed");
        } else {
            System.out.println("Executing is paused - unpausing...");
            Transaction tx = msgBridge.invokeFunction("unpauseExecuting").signers(calledByEntry(governor)).sign();
            NeoSendRawTransaction response = tx.send();
            if (response.hasError()) {
                throw new Exception("Error unpausing executing: " + response.getError().getMessage());
            }
            Hash256 txHash = response.getSendRawTransaction().getHash();
            System.out.println("Transaction sent: " + txHash);
            waitUntilTransactionIsExecuted(txHash, neow3j);
            if (!msgBridge.callFunctionReturningBool("executingIsPaused")) {
                throw new Exception("Unpausing executing failed in transaction: " + txHash);
            }
            executingPauseState = SUCCESS;
            System.out.println("Transaction confirmed");
            System.out.println("Executing unpaused successfully");
        }

        System.out.println();
        System.out.println("==================================================");
        System.out.println("MESSAGE BRIDGE UNPAUSE OPERATIONS SUMMARY");
        System.out.println("==================================================");
        System.out.println();
        System.out.println("Overall unpause state: " + overallPauseState);
        System.out.println("Sending unpause state: " + sendingPauseState);
        System.out.println("Executing unpause state: " + executingPauseState);
    }

}
