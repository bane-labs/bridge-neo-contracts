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

public class UnpauseDeposits {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        BridgeClient bridge = getBridgeClientFromEnv(neow3j);
        Account governor = getGovernorAccountFromEnv();

        System.out.println("Unpause deposits...");
        printNetwork(neow3j);
        printSender(governor.getScriptHash());

        if (!bridge.depositsArePaused()) {
            System.out.println("\nDeposits are not paused - no action needed");
            return;
        }
        System.out.println("\nDeposits are paused, proceeding to unpause...");

        bridge.unpauseDeposits().withSigners(calledByEntry(governor)).signSendAndAwait(System.out);

        if (bridge.depositsArePaused()) {
            throw new Exception("Deposits are still paused");
        }
        System.out.println("Deposits successfully unpaused");
    }

}
