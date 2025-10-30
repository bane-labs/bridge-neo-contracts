package network.bane.scripts.token;

import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static network.bane.utils.env.EnvVariables.BRIDGE_HASH;
import static network.bane.utils.env.EnvVariables.N3_JSON_RPC;
import static network.bane.utils.env.EnvVariables.WALLET_PASSWORD_GOVERNOR;
import static network.bane.utils.env.EnvVariables.WALLET_FILEPATH_GOVERNOR;
import static network.bane.utils.wallet.LoadWallet.getAccountFromWallet;

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
 * Run with: gradle run -PmainClass=network.bane.scripts.token.UnpauseAll
 */
public class UnpauseAll {

    // The following are the required env variables for running this script
    private static final Neow3j neow3j = Neow3j.build(new HttpService(N3_JSON_RPC));
    private static final SmartContract bridge = new SmartContract(BRIDGE_HASH, neow3j);
    private static final String governorWalletPath = WALLET_FILEPATH_GOVERNOR;
    private static final String governorWalletPassword = WALLET_PASSWORD_GOVERNOR;

    private static final String ALREADY_UNPAUSED = "Already unpaused";
    private static final String SUCCESS = "Successful";
    private static final String NOT_SET = "Not set - nothing to unpause";

    public static void main(String[] args) throws Throwable {
        String overallPauseState;
        String nativePauseState;
        String depositsPauseState;

        Account governor = getAccountFromWallet(governorWalletPath, governorWalletPassword);

        System.out.println("Unpausing BridgeContract");
        System.out.println("Unpausing BridgeContract at address: " + bridge.getScriptHash());
        System.out.println("Governor address: " + governor.getScriptHash());

        System.out.println("\nAttempting unpause operations...");
        System.out.println("Note: Operations will be skipped automatically if components are already unpaused.");

        System.out.println("\nOverall bridge pausing");
        if (!bridge.callFunctionReturningBool("isPaused")) {
            overallPauseState = ALREADY_UNPAUSED;
            System.out.println("Bridge is not paused - no action needed");
        } else {
            System.out.println("Bridge is paused - unpausing...");
            Transaction tx = bridge.invokeFunction("unpauseBridge").signers(calledByEntry(governor)).sign();
            NeoSendRawTransaction response = tx.send();
            if (response.hasError()) {
                throw new Exception("Error unpausing bridge: " + response.getError().getMessage());
            }
            Hash256 txHash = response.getSendRawTransaction().getHash();
            System.out.println("Transaction sent: " + txHash);
            waitUntilTransactionIsExecuted(txHash, neow3j);
            if (bridge.callFunctionReturningBool("isPaused")) {
                throw new Exception("Unpausing the bridge contract failed in transaction: " + txHash);
            }
            overallPauseState = SUCCESS;
            System.out.println("Transaction confirmed");
            System.out.println("Bridge unpaused successfully");
        }

        // Unpause native bridge
        System.out.println("\nNative bridge pause status");
        if (!bridge.callFunctionReturningBool("nativeBridgeIsSet")) {
            nativePauseState = NOT_SET;
            System.out.println("Native bridge not set - no need to unpause it");
        } else {
            if (!nativeBridgeIsPaused(bridge)) {
                nativePauseState = ALREADY_UNPAUSED;
                System.out.println("Native bridge is not paused - no action needed");
            } else {
                System.out.println("Native bridge is paused - unpausing...");
                Transaction tx = bridge.invokeFunction("unpauseNativeBridge").signers(calledByEntry(governor)).sign();
                NeoSendRawTransaction response = tx.send();
                if (response.hasError()) {
                    throw new Exception("Error unpausing native bridge: " + response.getError().getMessage());
                }
                Hash256 txHash = response.getSendRawTransaction().getHash();
                System.out.println("Transaction sent: " + txHash);
                waitUntilTransactionIsExecuted(txHash, neow3j);
                if (nativeBridgeIsPaused(bridge)) {
                    throw new Exception("Unpausing native bridge failed in transaction " + txHash);
                }
                nativePauseState = SUCCESS;
                System.out.println("Transaction confirmed");
                System.out.println("Native bridge unpaused successfully");
            }
        }

        // Unpause all token bridges
        System.out.println("\nToken bridges pause status");
        List<Hash160> registeredTokens = bridge.callInvokeFunction("getRegisteredTokens").getInvocationResult()
                .getFirstStackItem().getList().stream().map(s -> Hash160.fromAddress(s.getAddress()))
                .collect(Collectors.toList());
        System.out.println("Found " + registeredTokens.size() + " registered tokens");
        HashMap<Hash160, String> stateMap = new HashMap<>();
        for (Hash160 tokenHash : registeredTokens) {
            boolean paused = tokenBridgeIsPaused(bridge, tokenHash);
            if (!paused) {
                stateMap.put(tokenHash, ALREADY_UNPAUSED);
                System.out.println("Token bridge for token " + tokenHash + " is not paused - no action needed");
                continue;
            } else {
                System.out.println("Token bridge for token " + tokenHash + " is paused - unpausing...");
                NeoSendRawTransaction response = bridge.invokeFunction("unpauseTokenBridge", hash160(tokenHash))
                        .signers(calledByEntry(governor)).sign().send();
                Hash256 txHash = response.getSendRawTransaction().getHash();
                waitUntilTransactionIsExecuted(txHash, neow3j);
                if (tokenBridgeIsPaused(bridge, tokenHash)) {
                    throw new Exception(format("Unpausing token bridge for token %s failed in transaction %s",
                            tokenHash, txHash));
                }
                stateMap.put(tokenHash, SUCCESS);
                System.out.println("Transaction confirmed");
                System.out.println("Token bridge for token " + tokenHash + " unpaused successfully");
            }
        }
        if (!registeredTokens.isEmpty()) {
            System.out.println("All token bridges unpaused");
        }

        // Unpause deposits
        System.out.println("\nDeposits pause status");
        if (!bridge.callFunctionReturningBool("depositsArePaused")) {
            depositsPauseState = ALREADY_UNPAUSED;
            System.out.println("Deposits are not paused - no action needed");
        } else {
            System.out.println("Deposits are paused - unpausing...");
            NeoSendRawTransaction response = bridge.invokeFunction("unpauseDeposits")
                    .signers(calledByEntry(governor)).sign().send();
            Hash256 txHash = response.getSendRawTransaction().getHash();
            System.out.println("Transaction sent: " + txHash);
            waitUntilTransactionIsExecuted(txHash, neow3j);
            if (bridge.callFunctionReturningBool("depositsArePaused")) {
                throw new Exception(format("Unpausing deposits failed in transaction %s",
                        txHash));
            }
            depositsPauseState = SUCCESS;
            System.out.println("Transaction confirmed");
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

    static boolean nativeBridgeIsPaused(SmartContract bridge) throws IOException {
        return bridge.callInvokeFunction("getNativeBridge", asList()).getInvocationResult().getFirstStackItem()
                .getList().get(0).getList().get(0).getBoolean();
    }

    static boolean tokenBridgeIsPaused(SmartContract bridge, Hash160 tokenHash) throws IOException {
        return bridge.callInvokeFunction("getTokenBridge", asList(hash160(tokenHash))).getInvocationResult()
                .getFirstStackItem().getList().get(0).getList().get(0).getBoolean();
    }

}
