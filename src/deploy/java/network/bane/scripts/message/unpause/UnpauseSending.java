package network.bane.scripts.message.unpause;

import io.neow3j.protocol.Neow3j;
import io.neow3j.wallet.Account;
import network.bane.client.MessageBridgeClient;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.PrintHelper.printSender;
import static network.bane.utils.env.EnvVariables.getMessageBridgeClientFromEnv;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.env.EnvWallets.getGovernorAccountFromEnv;

public class UnpauseSending {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        MessageBridgeClient messageBridge = getMessageBridgeClientFromEnv(neow3j);
        Account governor = getGovernorAccountFromEnv();

        System.out.println("Unpause message sending...");
        printNetwork(neow3j);
        printSender(governor.getScriptHash());

        if (!messageBridge.sendingIsPaused()) {
            System.out.println("\nMessage sending is not paused - no action needed");
            return;
        }
        System.out.println("\nMessage sending is paused, proceeding to unpause...");

        messageBridge.unpauseSending().withSigners(calledByEntry(governor)).signSendAndAwait();

        if (messageBridge.sendingIsPaused()) {
            throw new Exception("Message sending is still paused");
        }
        System.out.println("Message sending successfully unpaused");
    }

}
