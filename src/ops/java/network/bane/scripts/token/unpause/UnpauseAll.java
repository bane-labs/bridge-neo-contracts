package network.bane.scripts.token.unpause;

import io.neow3j.protocol.Neow3j;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;
import network.bane.client.BridgeClient;

import java.util.HashMap;
import java.util.List;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static java.lang.String.format;
import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.PrintHelper.printSender;
import static network.bane.utils.env.EnvVariables.getBridgeClientFromEnv;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.env.EnvWallets.getGovernorAccountFromEnv;

/**
 * This class makes sure everything in the bridge contract is unpaused. If something is paused, it sends a
 * transaction unpausing it. This includes:
 * - Unpausing bridge overall
 * - Unpausing native bridge
 * - Unpausing token bridges
 * - Unpausing deposits
 * <p>
 * Requires the following environment variables to be set:
 * - N3_JSON_RPC: The RPC endpoint of the N3 node
 * - BRIDGE_HASH: Hash of the deployed bridge contract
 * - WALLET_FILEPATH_GOVERNOR: the filepath to the governor wallet
 * - WALLET_PASSWORD_GOVERNOR: the password for the governor wallet
 * <p>
 * Run with: ./gradlew runOps -PmainClass=network.bane.scripts.token.unpause.UnpauseAll
 */
public class UnpauseAll {

    private static final String ALREADY_UNPAUSED = "Already unpaused";
    private static final String SUCCESS = "Successful";
    private static final String NOT_SET = "Not set - nothing to unpause";

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        BridgeClient bridge = getBridgeClientFromEnv(neow3j);
        Account governor = getGovernorAccountFromEnv();

        System.out.println("Unpause all components of the bridge...");
        printNetwork(neow3j);
        printSender(governor.getScriptHash());

        String overallPauseState;
        String nativePauseState;
        String depositsPauseState;

        System.out.println("Unpause BridgeContract");
        System.out.println("Unpause BridgeContract at address: " + bridge.getScriptHash());
        System.out.println("Governor address: " + governor.getScriptHash());

        System.out.println("\nAttempt unpause operations...");
        System.out.println("Note: Operations will be skipped automatically if components are already unpaused.");

        System.out.println("\nOverall bridge pausing");
        if (!bridge.isPaused()) {
            overallPauseState = ALREADY_UNPAUSED;
            System.out.println("Bridge is not paused - no action needed");
        } else {
            System.out.println("Bridge is paused - unpausing...");
            Hash256 txHash = bridge.unpauseBridge().withSigners(calledByEntry(governor)).signSendAndAwait(System.out);
            if (bridge.isPaused()) {
                throw new Exception("Unpausing the bridge contract failed in transaction: " + txHash);
            }
            overallPauseState = SUCCESS;
            System.out.println("Bridge unpaused successfully");
        }

        // Unpause native bridge
        System.out.println("\nNative bridge pause status");
        if (!bridge.nativeBridgeIsSet()) {
            nativePauseState = NOT_SET;
            System.out.println("Native bridge not set - no need to unpause it");
        } else {
            if (!bridge.getNativeBridge().paused) {
                nativePauseState = ALREADY_UNPAUSED;
                System.out.println("Native bridge is not paused - no action needed");
            } else {
                System.out.println("Native bridge is paused - unpausing...");
                Hash256 txHash = bridge.unpauseNativeBridge().withSigners(calledByEntry(governor))
                        .signSendAndAwait(System.out);
                if (bridge.getNativeBridge().paused) {
                    throw new Exception("Unpausing native bridge failed in transaction " + txHash);
                }
                nativePauseState = SUCCESS;
                System.out.println("Native bridge unpaused successfully");
            }
        }

        // Unpause all token bridges
        System.out.println("\nToken bridges pause status");
        List<Hash160> registeredTokens = bridge.getRegisteredTokens();
        System.out.println("Found " + registeredTokens.size() + " registered tokens");
        HashMap<Hash160, String> stateMap = new HashMap<>();
        for (Hash160 tokenHash : registeredTokens) {
            if (!bridge.getTokenBridge(tokenHash).paused) {
                stateMap.put(tokenHash, ALREADY_UNPAUSED);
                System.out.println("Token bridge for token " + tokenHash + " is not paused - no action needed");
                continue;
            } else {
                System.out.println("Token bridge for token " + tokenHash + " is paused - unpausing...");
                Hash256 txHash = bridge.unpauseTokenBridge(tokenHash).withSigners(calledByEntry(governor))
                        .signSendAndAwait(System.out);
                if (bridge.getTokenBridge(tokenHash).paused) {
                    throw new Exception(format("Unpausing token bridge for token %s failed in transaction %s",
                            tokenHash, txHash));
                }
                stateMap.put(tokenHash, SUCCESS);
                System.out.println("Token bridge for token " + tokenHash + " unpaused successfully");
            }
        }
        if (!registeredTokens.isEmpty()) {
            System.out.println("All token bridges unpaused");
        }

        // Unpause deposits
        System.out.println("\nDeposits pause status");
        if (!bridge.depositsArePaused()) {
            depositsPauseState = ALREADY_UNPAUSED;
            System.out.println("Deposits are not paused - no action needed");
        } else {
            System.out.println("Deposits are paused - unpausing...");
            Hash256 txHash = bridge.unpauseDeposits().withSigners(calledByEntry(governor)).signSendAndAwait(System.out);
            if (bridge.depositsArePaused()) {
                throw new Exception(format("Unpausing deposits failed in transaction %s", txHash));
            }
            depositsPauseState = SUCCESS;
            System.out.println("Deposits unpaused successfully");
        }
        System.out.println("Complete unpausing process completed");

        System.out.println();
        System.out.println("==================================================");
        System.out.println("BRIDGE UNPAUSE OPERATIONS SUMMARY");
        System.out.println("==================================================");
        System.out.println();
        System.out.println("Overall unpause state: " + overallPauseState);
        System.out.println("Native unpause state: " + nativePauseState);
        System.out.printf("Token unpause state: %s tokens registered\n", registeredTokens.size());
        for (int i = 0; i < registeredTokens.size(); i++) {
            Hash160 token = registeredTokens.get(i);
            System.out.printf(" - %s/%s %s: %s\n", i + 1, registeredTokens.size(), stateMap.get(token), token);
        }
        System.out.println("Deposits unpause state: " + depositsPauseState);
    }

}
