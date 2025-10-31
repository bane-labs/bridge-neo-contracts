package network.bane.scripts.message;

import io.neow3j.contract.GasToken;
import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoApplicationLog;
import io.neow3j.protocol.core.response.NeoBlock;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.types.NeoVMStateType;
import io.neow3j.wallet.Account;

import java.math.BigInteger;

import static io.neow3j.transaction.AccountSigner.none;
import static io.neow3j.types.ContractParameter.any;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static io.neow3j.utils.Numeric.hexStringToByteArray;
import static io.neow3j.utils.Numeric.isValidHexString;
import static io.neow3j.utils.Numeric.toHexString;
import static network.bane.utils.env.EnvVariables.MESSAGE_BRIDGE_HASH;
import static network.bane.utils.env.EnvVariables.MESSAGE_SEND_STORE_ONLY_MESSAGE;
import static network.bane.utils.env.EnvVariables.WALLET_PASSWORD_PERSONAL;
import static network.bane.utils.env.EnvVariables.WALLET_FILEPATH_PERSONAL;
import static network.bane.utils.env.EnvVariables.getEnvVariable;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.wallet.LoadWallet.getAccountFromWallet;

/**
 * Sends a store-only message
 * <p>
 * Requires the following environment variables to be set:
 * - N3_JSON_RPC: The RPC endpoint of the N3 node
 * - WALLET_FILEPATH_PERSONAL: the filepath to the personal wallet. This wallet is used to send the message.
 * - WALLET_PASSWORD_PERSONAL: the password for the personal wallet
 * - MESSAGE_SEND_STORE_ONLY_MESSAGE: The message to send as a hex string or UTF-8 string. If the string is a valid
 *    hex string, it will be interpreted as hex, otherwise as UTF-8.
 * - MESSAGE_BRIDGE_HASH: Hash of the deployed message bridge contract
 * - MESSAGE_DATA: Hex string of the message data to send
 * - MESSAGE_TYPE: Either "executable" or "store-only"
 * - FEE_SPONSOR: Hash160 of fee sponsor (optional, uses sender if not set)
 * <p>
 * Run with: gradle run -PmainClass=network.bane.scripts.message.SendMessage
 */
public class SendMessage {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        SmartContract messageBridge = new SmartContract(getHash160FromEnvVar(MESSAGE_BRIDGE_HASH), neow3j);
        String personalWalletPath = getEnvVariable(WALLET_FILEPATH_PERSONAL);
        String personalWalletPassword = getEnvVariable(WALLET_PASSWORD_PERSONAL);
        String messageToSend = getEnvVariable(MESSAGE_SEND_STORE_ONLY_MESSAGE);

        System.out.println("=== Message Bridge - Send Store-Only Message ===");
        System.out.println("Using Message Bridge Contract: " + messageBridge.getScriptHash());

        Account senderAcc = getAccountFromWallet(personalWalletPath, personalWalletPassword);

        byte[] messageData = getMessageDataBytes(messageToSend);
        printSendingMessageInfo(senderAcc, messageData, "Store-Only");

        // Get the current sending fee
        BigInteger sendingFee = messageBridge.callFunctionReturningInt("sendingFee");
        System.out.printf("Sending Fee: %s GAS%n", GasToken.toDecimals(sendingFee, 8));
        BigInteger maxFee = sendingFee;

        // Invoking: sendMessage(rawMessage, feeSponsor, sendingFee)
        Transaction tx = messageBridge.invokeFunction("sendMessage",
                        byteArray(messageData),
                        any(null),
                        integer(maxFee)
                )
                .signers(none(senderAcc).setAllowedContracts(GasToken.SCRIPT_HASH))
                .sign();

        System.out.println("\n--- Sending Store-Only Message ---");
        sendMessageSendTransaction(neow3j, tx, messageBridge.getScriptHash());
    }

    static void sendMessageSendTransaction(Neow3j neow3j, Transaction tx, Hash160 messageBridgeHash) throws Exception {
        // Send the transaction
        System.out.println("Sending transaction...");
        NeoSendRawTransaction response = tx.send();

        if (response.hasError()) {
            throw new Exception("Error sending message: " + response.getError().getMessage());
        }

        Hash256 txHash = response.getSendRawTransaction().getHash();
        System.out.println("Transaction sent successfully!");
        System.out.println("Transaction Hash: " + txHash);

        // Wait for transaction execution and get the result
        System.out.println("Waiting for transaction execution...");
        waitUntilTransactionIsExecuted(txHash, neow3j);
        Hash256 blockHash = neow3j.getTransaction(txHash).send().getTransaction().getBlockHash();
        NeoBlock block = neow3j.getBlockHeader(blockHash).send().getBlock();
        System.out.println("Included in Block: " + block.getIndex());

        NeoApplicationLog.Execution exec = tx.getApplicationLog().getFirstExecution();
        if (exec.getState().equals(NeoVMStateType.HALT)) {
            System.out.println("Message execution successful!");

            // Look for MessageSend event in the logs
            exec.getNotifications().stream()
                    .filter(notification -> notification.getContract().equals(messageBridgeHash))
                    .filter(notification -> "MessageSend".equals(notification.getEventName()))
                    .findFirst()
                    .ifPresent(notification -> {
                        System.out.println("\n--- Message Send Event ---");
                        StackItem state = notification.getState();
                        System.out.println(state);
                    });
        } else {
            System.out.println("Transaction failed!");
            System.out.println("Exception: " + exec.getException());
        }
    }

    static byte[] getMessageDataBytes(String messageToSend) {
        if (isValidHexString(messageToSend)) {
            return hexStringToByteArray(messageToSend);
        } else {
            System.out.println("Provided message is not in hexadecimal format - using UTF-8 bytes");
            return messageToSend.getBytes();
        }
    }

    static void printSendingMessageInfo(Account senderAcc, byte[] messageData, String typeString) {
        System.out.println("Sending message (hex): " + toHexString(messageData));
        System.out.println("Sender: " + senderAcc.getScriptHash());
        System.out.println("Message Data (hex): " + toHexString(messageData));
        System.out.println("Message Data Size: " + messageData.length + " bytes");
        System.out.println("Message Type: " + typeString);
    }

}
