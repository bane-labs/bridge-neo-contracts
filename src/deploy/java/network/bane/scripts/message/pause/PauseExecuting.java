package network.bane.scripts.message.pause;

import io.neow3j.protocol.Neow3j;
import io.neow3j.wallet.Account;
import network.bane.client.MessageBridgeClient;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.PrintHelper.printSender;
import static network.bane.utils.env.EnvVariables.getMessageBridgeClientFromEnv;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.env.EnvWallets.getGovernorAccountFromEnv;

public class PauseExecuting {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        MessageBridgeClient messageBridge = getMessageBridgeClientFromEnv(neow3j);
        Account governor = getGovernorAccountFromEnv();

        System.out.println("Pause executing in message bridge...");
        printNetwork(neow3j);
        printSender(governor.getScriptHash());

        if (messageBridge.executingIsPaused()) {
            System.out.println("\nMessage Executing is already paused - no action needed");
            return;
        }

        messageBridge.pauseExecuting().withSigners(calledByEntry(governor)).signSendAndAwait();

        if (!messageBridge.executingIsPaused()) {
            throw new Exception("Message Executing is still not paused");
        }
        System.out.println("Message Executing successfully paused");
    }

}
