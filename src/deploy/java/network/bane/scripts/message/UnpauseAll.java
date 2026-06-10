package network.bane.scripts.message;

import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
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

/**
 * This class makes sure everything in the message bridge contract is unpaused. If something is paused, it sends a
 * transaction unpausing it. This includes:
 * - Unpausing message bridge overall
 * - Unpausing sending messages
 * - Unpausing executing messages
 * <p>
 * Requires the following environment variables to be set:
 * - N3_JSON_RPC: The RPC endpoint of the N3 node
 * - MESSAGE_BRIDGE_HASH: Hash of the deployed message bridge contract
 * - WALLET_FILEPATH_GOVERNOR: the filepath to the governor wallet.
 * - WALLET_PASSWORD_GOVERNOR: the password for the governor wallet
 * <p>
 * Run with: gradle run -PmainClass=network.bane.scripts.message.UnpauseAll
 */
public class UnpauseAll {

    private static final String ALREADY_UNPAUSED = "Already unpaused";
    private static final String SUCCESS = "Successful";

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        SmartContract messageBridge = new SmartContract(getHash160FromEnvVar(MESSAGE_BRIDGE_HASH), neow3j);

        System.out.println("Unpause all components in message bridge...");

        Account governorAcc = getGovernorAccountFromEnv();
        printNetwork(neow3j);
        printSender(governorAcc.getScriptHash());

        String overallPauseState;
        String sendingPauseState;
        String executingPauseState;

        System.out.println("Unpause MessageBridge at address: " + messageBridge.getScriptHash());
        System.out.println("Governor address: " + governorAcc.getScriptHash());

        System.out.println("\nAttempting unpause operations...");
        System.out.println("Note: Operations will be skipped automatically if components are already unpaused.");

        System.out.println("\nOverall bridge pausing");
        if (!messageBridge.callFunctionReturningBool("isPaused")) {
            overallPauseState = ALREADY_UNPAUSED;
            System.out.println("MessageBridge is not paused - no action needed");
        } else {
            System.out.println("MessageBridge is paused - unpausing...");
            NeoSendRawTransaction response = messageBridge.invokeFunction("unpause")
                    .signers(calledByEntry(governorAcc))
                    .sign().send();
            if (response.hasError()) {
                throw new Exception("Error unpausing message bridge: " + response.getError().getMessage());
            }
            Hash256 txHash = response.getSendRawTransaction().getHash();
            System.out.println("Transaction sent: " + txHash);
            waitUntilTransactionIsExecuted(txHash, neow3j);
            if (messageBridge.callFunctionReturningBool("isPaused")) {
                throw new Exception("Unpausing the message bridge contract failed in transaction: " + txHash);
            }
            overallPauseState = SUCCESS;
            System.out.println("Transaction confirmed");
            System.out.println("MessageBridge unpaused successfully");
        }

        // Unpause sending
        System.out.println("\nUnpausing sending");
        if (!messageBridge.callFunctionReturningBool("sendingIsPaused")) {
            sendingPauseState = ALREADY_UNPAUSED;
            System.out.println("Sending is not paused - no action needed");
        } else {
            System.out.println("Sending is paused - unpausing...");
            NeoSendRawTransaction response = messageBridge.invokeFunction("unpauseSending")
                    .signers(calledByEntry(governorAcc))
                    .sign().send();
            if (response.hasError()) {
                throw new Exception("Error unpausing sending: " + response.getError().getMessage());
            }
            Hash256 txHash = response.getSendRawTransaction().getHash();
            System.out.println("Transaction sent: " + txHash);
            waitUntilTransactionIsExecuted(txHash, neow3j);
            if (messageBridge.callFunctionReturningBool("sendingIsPaused")) {
                throw new Exception("Unpausing sending failed in transaction: " + txHash);
            }
            sendingPauseState = SUCCESS;
            System.out.println("Transaction confirmed");
            System.out.println("Sending unpaused successfully");
        }

        // Unpause executing
        System.out.println("\nUnpausing executing");
        if (!messageBridge.callFunctionReturningBool("executingIsPaused")) {
            executingPauseState = ALREADY_UNPAUSED;
            System.out.println("Executing is not paused - no action needed");
        } else {
            System.out.println("Executing is paused - unpausing...");
            NeoSendRawTransaction response = messageBridge.invokeFunction("unpauseExecuting")
                    .signers(calledByEntry(governorAcc))
                    .sign().send();
            if (response.hasError()) {
                throw new Exception("Error unpausing executing: " + response.getError().getMessage());
            }
            Hash256 txHash = response.getSendRawTransaction().getHash();
            System.out.println("Transaction sent: " + txHash);
            waitUntilTransactionIsExecuted(txHash, neow3j);
            if (!messageBridge.callFunctionReturningBool("executingIsPaused")) {
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
