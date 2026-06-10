package network.bane.scripts.message;

import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;

import java.math.BigInteger;

import static network.bane.scripts.message.MessageSendHelper.checkExecutionResult;
import static network.bane.scripts.message.MessageSendHelper.checkThatMessageExists;
import static network.bane.scripts.message.MessageSendHelper.printStateRoot;
import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.env.EnvVariables.MESSAGE_BRIDGE_HASH;
import static network.bane.utils.env.EnvVariables.MESSAGE_NONCE;
import static network.bane.utils.env.EnvVariables.getEnvVariable;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;

/**
 * Retrieves message information and state roots from the message bridge
 * <p>
 * Requires the following environment variables to be set:
 * - N3_JSON_RPC: The RPC endpoint of the N3 node
 * - WALLET_FILEPATH_PERSONAL: the filepath to the personal wallet
 * - WALLET_PASSWORD_PERSONAL: the password for the personal wallet
 * - MESSAGE_BRIDGE_HASH: Hash of the deployed message bridge contract
 * - MESSAGE_NONCE: The nonce of the message to retrieve
 * <p>
 * Run with: gradle run -PmainClass=network.bane.scripts.message.GetMessageAndStates
 */

public class GetMessageAndStates {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        SmartContract messageBridge = new SmartContract(getHash160FromEnvVar(MESSAGE_BRIDGE_HASH), neow3j);
        String nonceStr = getEnvVariable(MESSAGE_NONCE);

        System.out.println("Get message and state roots from message bridge...");
        printNetwork(neow3j);

        BigInteger nonce = new BigInteger(nonceStr);

        checkThatMessageExists(messageBridge, nonce);

        checkExecutionResult(messageBridge, nonce);

        // Get and print the EVM to NeoN3 root
        printStateRoot(messageBridge, "neoToEvmRoot");
        printStateRoot(messageBridge, "evmToNeoRoot");
    }
}
