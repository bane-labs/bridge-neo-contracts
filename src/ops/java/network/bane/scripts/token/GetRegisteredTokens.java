package network.bane.scripts.token;

import io.neow3j.protocol.Neow3j;
import io.neow3j.types.Hash160;
import network.bane.client.BridgeClient;
import network.bane.dto.bridge.TokenBridge;

import java.io.IOException;
import java.util.List;

import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.env.EnvVariables.getBridgeClientFromEnv;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;

/**
 * This script retrieves and prints all registered tokens in the bridge contract along with their token bridge
 * information.
 * <p>
 * Requires the following environment variables to be set:
 * - N3_JSON_RPC: The RPC endpoint of the N3 node
 * - BRIDGE_HASH: Hash of the deployed bridge contract
 * <p>
 * Run with: gradle run -PmainClass=network.bane.scripts.token.GetRegisteredTokens
 */
public class GetRegisteredTokens {

    public static void main(String[] args) throws IOException {
        Neow3j neow3j = getNeow3jFromEnv();
        BridgeClient bridge = getBridgeClientFromEnv(neow3j);

        System.out.println("Get registered tokens...");
        printNetwork(neow3j);

        List<Hash160> registeredTokens = bridge.getRegisteredTokens();

        for (Hash160 token : registeredTokens) {
            TokenBridge tokenBridge = bridge.getTokenBridge(token);
            System.out.println(token);
            System.out.println(tokenBridge);
            System.out.println();
        }
    }
}
