package network.bane.scripts.message;

import io.neow3j.protocol.Neow3j;
import io.neow3j.types.CallFlags;
import network.bane.client.MessageBridgeClient;

import static io.neow3j.utils.Numeric.toHexString;
import static java.util.Arrays.asList;
import static network.bane.utils.env.EnvVariables.getMessageBridgeClientFromEnv;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;

/**
 * Generates an executable hex script for calling isPaused() on the message bridge contract
 * <p>
 * This script generates the hex code that can be used as MESSAGE_SEND_EXECUTABLE_MESSAGE
 * to call the isPaused() method on the message bridge contract.
 * <p>
 * Run with: gradle run -PmainClass=network.bane.scripts.message.GenerateIsPausedHex
 */
public class GenerateIsPausedHex {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        MessageBridgeClient messageBridge = getMessageBridgeClientFromEnv(neow3j);

        System.out.println("=== Generate isPaused() Executable Hex Script ===");
        System.out.println("Target Contract: " + messageBridge.getScriptHash());
        System.out.println("Method: isPaused()");

        // Create the script that calls isPaused() on the message bridge
        byte[] scriptBytes = messageBridge.serializeCall(messageBridge.getScriptHash(), "isPaused", CallFlags.ALL,
                asList());

        System.out.println("\n--- Generated Executable Hex Script ---");
        System.out.println("Hex Script:     " + toHexString(scriptBytes));
        System.out.println("Length (bytes): " + scriptBytes.length);
    }
}
