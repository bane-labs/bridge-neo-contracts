package network.bane.scripts.token.unpause;

import io.neow3j.protocol.Neow3j;
import io.neow3j.wallet.Account;
import network.bane.client.BridgeClient;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.PrintHelper.printSender;
import static network.bane.utils.env.EnvVariables.getBridgeClientFromEnv;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.env.EnvWallets.getGovernorAccountFromEnv;

public class Unpause {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        BridgeClient bridge = getBridgeClientFromEnv(neow3j);
        Account governor = getGovernorAccountFromEnv();

        System.out.println("Unpause bridge...");
        printNetwork(neow3j);
        printSender(governor.getScriptHash());

        if (!bridge.isPaused()) {
            System.out.println("\nBridge is not paused - no action needed");
            return;
        }
        System.out.println("\nBridge is paused, proceeding to unpause...");

        bridge.unpauseBridge().withSigners(calledByEntry(governor)).signSendAndAwait();

        if (bridge.isPaused()) {
            throw new Exception("Bridge is still paused");
        }
        System.out.println("Bridge successfully unpaused");
    }

}
