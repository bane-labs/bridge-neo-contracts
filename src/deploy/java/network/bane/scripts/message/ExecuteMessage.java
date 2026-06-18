package network.bane.scripts.message;

import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.wallet.Account;
import network.bane.client.MessageBridgeClient;

import java.math.BigInteger;
import java.util.List;

import static io.neow3j.transaction.AccountSigner.none;
import static io.neow3j.types.ContractParameter.integer;
import static java.util.Arrays.asList;
import static network.bane.scripts.message.MessageSendHelper.checkExecutionResult;
import static network.bane.scripts.message.MessageSendHelper.checkThatMessageExists;
import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.PrintHelper.printSender;
import static network.bane.utils.env.EnvVariables.MESSAGE_NONCE;
import static network.bane.utils.env.EnvVariables.getEnvVariable;
import static network.bane.utils.env.EnvVariables.getMessageBridgeClientFromEnv;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.env.EnvWallets.getPersonalAccountFromEnv;

/**
 * Executes a message by its nonce
 * <p>
 * Requires the following environment variables to be set:
 * - N3_JSON_RPC: The RPC endpoint of the N3 node
 * - WALLET_FILEPATH_PERSONAL: the filepath to the personal wallet. This wallet is used to execute the message.
 * - WALLET_PASSWORD_PERSONAL: the password for the personal wallet
 * - MESSAGE_BRIDGE_HASH: Hash of the deployed message bridge contract
 * - MESSAGE_EXECUTE_NONCE: The nonce of the message to execute (as integer)
 * <p>
 * Run with: gradle run -PmainClass=network.bane.scripts.message.ExecuteMessage
 */
public class ExecuteMessage {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        MessageBridgeClient messageBridge = getMessageBridgeClientFromEnv(neow3j);
        BigInteger nonce = new BigInteger(getEnvVariable(MESSAGE_NONCE));
        Account personalAccount = getPersonalAccountFromEnv();

        System.out.println("=== Message Bridge - Execute Message ===");
        printNetwork(neow3j);
        printSender(personalAccount.getScriptHash());
        System.out.println("Using Message Bridge Contract: " + messageBridge.getScriptHash());
        System.out.println("Executor Account: " + personalAccount.getAddress());
        System.out.println("Message Nonce to Execute: " + nonce);

        // Get and print the EVM to NeoN3 root before execution
        System.out.println("EvmToNeoRoot: " + messageBridge.evmToNeoRoot());

        try {
            if (checkThatMessageExists(messageBridge, nonce)) return;
        } catch (Exception e) {
            System.err.println("ERROR: Failed to get message for nonce " + nonce + ": " + e.getMessage());
            return;
        }

        // Check executable state before execution
        try {
            List<StackItem> execStateResult = messageBridge.callInvokeFunction("getExecutableState",
                    asList(integer(nonce))).getInvocationResult().getStack();
            if (!execStateResult.isEmpty()) {
                System.out.println("Executable state retrieved for nonce: " + nonce);
            }
        } catch (Exception e) {
            System.err.println("WARNING: Could not get executable state for nonce " + nonce + ": " + e.getMessage());
        }

        // Execute the message
        System.out.println("\n--- Executing Message ---");
        messageBridge.executeMessage(nonce).withSigners(none(personalAccount)).signSendAndAwait();

        // Check for execution result after execution
        try {
            checkExecutionResult(messageBridge, nonce);
        } catch (Exception e) {
            System.err.println("Could not retrieve execution result: " + e.getMessage());
        }
        System.out.println("\n=== Message Execution Complete ===");
    }
}
