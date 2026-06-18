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

public class PauseNativeBridge {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        BridgeClient bridge = getBridgeClientFromEnv(neow3j);
        Account governor = getGovernorAccountFromEnv();

        System.out.println("Pause native bridge...");
        printNetwork(neow3j);
        printSender(governor.getScriptHash());

        if (!bridge.nativeBridgeIsSet()) {
            System.out.println("\nNative bridge is not set - no action needed");
            return;
        }
        if (bridge.getNativeBridge().paused) {
            System.out.println("\nNative bridge is already paused - no action needed");
            return;
        }
        System.out.println("\nNative bridge is not paused, proceeding to pause...");

        bridge.pauseNativeBridge().withSigners(calledByEntry(governor)).signSendAndAwait();

        if (!bridge.getNativeBridge().paused) {
            throw new Exception("Native bridge is still not paused");
        }
        System.out.println("Native bridge successfully paused");
    }

}
