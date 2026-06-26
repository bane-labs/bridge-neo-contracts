package network.bane.scripts.message.unpause;

import io.neow3j.protocol.Neow3j;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;
import network.bane.client.MessageBridgeClient;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.PrintHelper.printSender;
import static network.bane.utils.env.EnvVariables.getMessageBridgeClientFromEnv;
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
 * Run with: ./gradlew runOps -PmainClass=network.bane.scripts.message.unpause.UnpauseAll
 */
public class UnpauseAll {

    private static final String ALREADY_UNPAUSED = "Already unpaused";
    private static final String SUCCESS = "Successful";

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        MessageBridgeClient messageBridge = getMessageBridgeClientFromEnv(neow3j);
        Account governor = getGovernorAccountFromEnv();

        System.out.println("Unpause all components in message bridge...");
        printNetwork(neow3j);
        printSender(governor.getScriptHash());

        String overallPauseState;
        String sendingPauseState;
        String executingPauseState;

        System.out.println("Unpause MessageBridge at address: " + messageBridge.getScriptHash());
        System.out.println("Governor address: " + governor.getScriptHash());

        System.out.println("\nAttempting unpause operations...");
        System.out.println("Note: Operations will be skipped automatically if components are already unpaused.");

        System.out.println("\nOverall bridge pausing");
        if (!messageBridge.isPaused()) {
            overallPauseState = ALREADY_UNPAUSED;
            System.out.println("MessageBridge is not paused - no action needed");
        } else {
            System.out.println("MessageBridge is paused - unpausing...");
            Hash256 txHash = messageBridge.unpause().withSigners(calledByEntry(governor)).signSendAndAwait(System.out);
            if (messageBridge.isPaused()) {
                throw new Exception("Unpausing the message bridge contract failed in transaction: " + txHash);
            }
            overallPauseState = SUCCESS;
            System.out.println("MessageBridge unpaused successfully");
        }

        // Unpause sending
        System.out.println("\nUnpausing sending");
        if (!messageBridge.sendingIsPaused()) {
            sendingPauseState = ALREADY_UNPAUSED;
            System.out.println("Sending is not paused - no action needed");
        } else {
            System.out.println("Sending is paused - unpausing...");
            Hash256 txHash = messageBridge.unpauseSending().withSigners(calledByEntry(governor))
                    .signSendAndAwait(System.out);
            if (messageBridge.sendingIsPaused()) {
                throw new Exception("Unpausing sending failed in transaction: " + txHash);
            }
            sendingPauseState = SUCCESS;
            System.out.println("Sending unpaused successfully");
        }

        // Unpause executing
        System.out.println("\nUnpausing executing");
        if (!messageBridge.executingIsPaused()) {
            executingPauseState = ALREADY_UNPAUSED;
            System.out.println("Executing is not paused - no action needed");
        } else {
            System.out.println("Executing is paused - unpausing...");
            Hash256 txHash = messageBridge.unpauseExecuting().withSigners(calledByEntry(governor))
                    .signSendAndAwait(System.out);
            if (messageBridge.executingIsPaused()) {
                throw new Exception("Unpausing executing failed in transaction: " + txHash);
            }
            executingPauseState = SUCCESS;
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
