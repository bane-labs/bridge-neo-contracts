package network.bane.scripts.token.unpause;

import io.neow3j.protocol.Neow3j;
import io.neow3j.types.Hash160;
import io.neow3j.wallet.Account;
import network.bane.client.BridgeClient;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static java.lang.String.format;
import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.PrintHelper.printSender;
import static network.bane.utils.env.EnvVariables.UNPAUSE_TOKEN_HASH;
import static network.bane.utils.env.EnvVariables.getBridgeClientFromEnv;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.env.EnvWallets.getGovernorAccountFromEnv;

public class UnpauseTokenBridge {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        BridgeClient bridge = getBridgeClientFromEnv(neow3j);
        Hash160 tokenHash = getHash160FromEnvVar(UNPAUSE_TOKEN_HASH);
        Account governor = getGovernorAccountFromEnv();

        System.out.println("Unpause token bridge...");
        printNetwork(neow3j);
        printSender(governor.getScriptHash());
        System.out.println("Token: " + tokenHash);

        if (!bridge.isRegisteredToken(tokenHash)) {
            System.out.printf("\nProvided token '%s' is not registered - no action needed\n", tokenHash);
            return;
        }
        if (!bridge.getTokenBridge(tokenHash).paused) {
            System.out.printf("\nToken bridge '%s' is not paused - no action needed\n", tokenHash);
            return;
        }
        System.out.printf("\nToken bridge '%s' is paused, proceeding to unpause...\n", tokenHash);

        bridge.unpauseTokenBridge(tokenHash).withSigners(calledByEntry(governor)).signSendAndAwait(System.out);

        if (bridge.getTokenBridge(tokenHash).paused) {
            throw new Exception(format("Token bridge '%s' is still paused", tokenHash));
        }
        System.out.println("Token bridge successfully unpaused");
    }

}
