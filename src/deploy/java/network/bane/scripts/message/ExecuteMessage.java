package network.bane.scripts.message;

import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.stackitem.ArrayStackItem;
import io.neow3j.protocol.core.stackitem.ByteStringStackItem;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.transaction.Transaction;
import io.neow3j.utils.Numeric;
import io.neow3j.wallet.Account;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static io.neow3j.transaction.AccountSigner.none;
import static io.neow3j.types.ContractParameter.integer;
import static java.util.Collections.singletonList;
import static network.bane.scripts.message.MessageSendHelper.checkExecutionResult;
import static network.bane.scripts.message.MessageSendHelper.printStateRoot;
import static network.bane.scripts.message.MessageSendHelper.sendTransaction;
import static network.bane.utils.env.EnvVariables.MESSAGE_BRIDGE_HASH;
import static network.bane.utils.env.EnvVariables.MESSAGE_NONCE;
import static network.bane.utils.env.EnvVariables.WALLET_FILEPATH_PERSONAL;
import static network.bane.utils.env.EnvVariables.WALLET_PASSWORD_PERSONAL;
import static network.bane.utils.env.EnvVariables.getEnvVariable;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.wallet.LoadWallet.getAccountFromWallet;

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
        SmartContract messageBridge = new SmartContract(getHash160FromEnvVar(MESSAGE_BRIDGE_HASH), neow3j);
        String personalWalletPath = getEnvVariable(WALLET_FILEPATH_PERSONAL);
        String personalWalletPassword = getEnvVariable(WALLET_PASSWORD_PERSONAL);
        String nonceStr = getEnvVariable(MESSAGE_NONCE);

        System.out.println("=== Message Bridge - Execute Message ===");
        System.out.println("Using Message Bridge Contract: " + messageBridge.getScriptHash());

        Account executorAcc = getAccountFromWallet(personalWalletPath, personalWalletPassword);
        BigInteger nonce = new BigInteger(nonceStr);

        System.out.println("Executor Account: " + executorAcc.getAddress());
        System.out.println("Message Nonce to Execute: " + nonce);

        // Get and print the EVM to NeoN3 root before execution
        printStateRoot(messageBridge, "evmToNeoRoot");

        try {
            if (checkThatMessageExists(messageBridge, nonce)) return;
        } catch (Exception e) {
            System.err.println("ERROR: Failed to get message for nonce " + nonce + ": " + e.getMessage());
            return;
        }

        // Check executable state before execution
        try {
            List<StackItem> execStateResult = messageBridge.callInvokeFunction(
                    "getExecutableState",
                    singletonList(integer(nonce))
            ).getInvocationResult().getStack();
            if (!execStateResult.isEmpty()) {
                System.out.println("Executable state retrieved for nonce: " + nonce);
            }
        } catch (Exception e) {
            System.err.println("WARNING: Could not get executable state for nonce " + nonce + ": " + e.getMessage());
        }

        // Execute the message
        System.out.println("\n--- Executing Message ---");
        Transaction tx = messageBridge.invokeFunction("executeMessage", integer(nonce))
                .signers(none(executorAcc))
                .sign();

       sendTransaction(neow3j, tx);

        // Check for execution result after execution
        try {
            checkExecutionResult(messageBridge, nonce);
        } catch (Exception e) {
            System.err.println("Could not retrieve execution result: " + e.getMessage());
        }

        System.out.println("\n=== Message Execution Complete ===");
    }

    private static boolean checkThatMessageExists(SmartContract messageBridge, BigInteger nonce) throws IOException {
        // Check if message exists and print its details
        List<StackItem> messageResult = messageBridge.callInvokeFunction(
                "getMessage",
                singletonList(integer(nonce))
        ).getInvocationResult().getStack();
        if (!messageResult.isEmpty() && messageResult.get(0).getValue() != null) {
            System.out.println("Message found for nonce: " + nonce);
            // Print message details
            System.out.println("Message Details:");
            StackItem item = messageResult.get(0);
            System.out.println(
                    "  Message: " + Numeric.toHexString(
                            ((ByteStringStackItem) ((ArrayStackItem) item).getValue().get(1)).getValue()
                    )
            );
        } else {
            System.err.println("ERROR: No message found for nonce: " + nonce);
            return true;
        }
        return false;
    }
}
