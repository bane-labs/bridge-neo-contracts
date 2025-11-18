package network.bane.scripts.message;

import io.neow3j.contract.SmartContract;
import io.neow3j.script.ScriptBuilder;
import io.neow3j.types.CallFlags;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;

import java.util.Arrays;

import static network.bane.utils.env.EnvVariables.MESSAGE_BRIDGE_HASH;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;

/**
 * Generates an executable hex script for calling isPaused() on the message bridge contract
 *
 * This script generates the hex code that can be used as MESSAGE_SEND_EXECUTABLE_MESSAGE
 * to call the isPaused() method on the message bridge contract.
 *
 * Run with: gradle run -PmainClass=network.bane.scripts.message.GenerateIsPausedHex
 */
public class GenerateIsPausedHex {

    public static void main(String[] args) throws Throwable {
        Hash160 messageBridgeHash = getHash160FromEnvVar(MESSAGE_BRIDGE_HASH);
        SmartContract messageBridge = new SmartContract(messageBridgeHash, getNeow3jFromEnv());

        System.out.println("=== Generate isPaused() Executable Hex Script ===");
        System.out.println("Target Contract: " + messageBridgeHash);
        System.out.println("Method: isPaused()");

        // Create the script that calls isPaused() on the message bridge
        ScriptBuilder script = new ScriptBuilder();
        String hexScript = messageBridge.callInvokeFunction("serializeCall", Arrays.asList(
                        ContractParameter.hash160(messageBridgeHash),
                        ContractParameter.string("isPaused"),
                        ContractParameter.integer(CallFlags.ALL.getValue()),
                        ContractParameter.array()
                )
        ).getInvocationResult().getFirstStackItem().getHexString();

        byte[] scriptBytes = script.toArray();

        System.out.println("\n--- Generated Executable Hex Script ---");
        System.out.println("Hex Script: " + hexScript);
        System.out.println("Length: " + scriptBytes.length + " bytes");
    }
}
