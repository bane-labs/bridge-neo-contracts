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

public class Unpause {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        MessageBridgeClient messageBridge = getMessageBridgeClientFromEnv(neow3j);
        Account governor = getGovernorAccountFromEnv();

        System.out.println("Unpause message bridge...");
        printNetwork(neow3j);
        printSender(governor.getScriptHash());

        if (!messageBridge.isPaused()) {
            System.out.println("\nMessageBridge is not paused - no action needed");
            return;
        }
        System.out.println("\nMessageBridge is paused, proceeding to unpause...");

        messageBridge.unpause().withSigners(calledByEntry(governor)).signSendAndAwait();

        if (messageBridge.isPaused()) {
            throw new Exception("MessageBridge is still paused");
        }
        System.out.println("MessageBridge successfully unpaused");
    }

}
