package network.bane.scripts.token.pause;

import io.neow3j.protocol.Neow3j;
import io.neow3j.wallet.Account;
import network.bane.client.BridgeClient;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.PrintHelper.printSender;
import static network.bane.utils.env.EnvVariables.getBridgeClientFromEnv;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.env.EnvWallets.getGovernorAccountFromEnv;

public class Pause {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        BridgeClient bridge = getBridgeClientFromEnv(neow3j);
        Account governor = getGovernorAccountFromEnv();

        System.out.println("Pause bridge...");
        printNetwork(neow3j);
        printSender(governor.getScriptHash());

        if (bridge.isPaused()) {
            System.out.println("\nBridge is already paused - no action needed");
            return;
        }
        System.out.println("\nBridge is not paused, proceeding to pause...");

        bridge.pauseBridge().withSigners(calledByEntry(governor)).signSendAndAwait(System.out);

        if (!bridge.isPaused()) {
            throw new Exception("Bridge is still not paused");
        }
        System.out.println("Bridge successfully paused");
    }

}
