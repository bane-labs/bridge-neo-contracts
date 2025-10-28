package network.bane.scripts.message;

import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoApplicationLog;
import io.neow3j.protocol.core.response.NeoBlock;
import io.neow3j.protocol.core.response.NeoGetTransaction;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.types.NeoVMStateType;
import io.neow3j.wallet.Account;
import network.bane.utils.env.GetEnv;

import java.math.BigInteger;

import static io.neow3j.transaction.AccountSigner.global;
import static io.neow3j.types.ContractParameter.bool;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static io.neow3j.utils.Numeric.hexStringToByteArray;
import static network.bane.utils.env.EnvVariables.NODE;
import static network.bane.utils.env.GetEnv.getEnvVariable;
import static network.bane.utils.wallet.LoadWallet.getOwnerAccountFromWallet;

/**
 * Helper tool for sending messages through the Message Bridge.
 *
 * Usage:
 * 1. Set environment variables:
 *    - MESSAGE_BRIDGE_HASH: Hash of the deployed message bridge contract
 *    - MESSAGE_DATA: Hex string of the message data to send
 *    - MESSAGE_TYPE: Either "executable" or "store-only"
 *    - FEE_SPONSOR: Hash160 of fee sponsor (optional, uses sender if not set)
 *
 * 2. Run the script: gradle run -PmainClass=network.bane.scripts.message.SendMessage
 */
public class SendMessage {

    public static void main(String[] args) throws Throwable {
        System.out.println("=== Message Bridge - Send Message Tool ===");

        // Initialize Neo connection
        Neow3j neow3j = Neow3j.build(new HttpService(NODE));

        // Get contract hash and sender account
        Hash160 messageBridgeHash = new Hash160(getEnvVariable("MESSAGE_BRIDGE_HASH"));
        Account senderAccount = getOwnerAccountFromWallet();

        // Get message parameters from environment (all required)
        String messageHex = getEnvVariable("MESSAGE_DATA");
        String messageType = getEnvVariable("MESSAGE_TYPE");
        String feeSponsorEnv = null;
        try {
            feeSponsorEnv = GetEnv.getEnvVariable("FEE_SPONSOR");
        } catch (Exception e) {
            // Ignore if not set
        }

        // Get storeResult from env (default: false)
        boolean storeResult = false;
        String storeResultEnv;
        try {
            storeResultEnv = GetEnv.getEnvVariable("MESSAGE_STORE_RESULT");
        } catch (Exception e) {
            storeResultEnv = "false";
        }
        storeResult = Boolean.parseBoolean(storeResultEnv);

        // Validate message type
        boolean isExecutable;
        if ("executable".equalsIgnoreCase(messageType)) {
            isExecutable = true;
        } else if ("store-only".equalsIgnoreCase(messageType)) {
            isExecutable = false;
        } else {
            throw new IllegalArgumentException("MESSAGE_TYPE must be either 'executable' or 'store-only', got: " + messageType);
        }

        byte[] messageData = hexStringToByteArray(messageHex);
        Hash160 feeSponsor = (feeSponsorEnv != null && !feeSponsorEnv.isEmpty()) ? new Hash160(feeSponsorEnv) : senderAccount.getScriptHash();

        System.out.println("Message Bridge Contract: " + messageBridgeHash);
        System.out.println("Sender: " + senderAccount.getScriptHash());
        System.out.println("Message Data (hex): " + messageHex);
        System.out.println("Message Data (bytes): " + messageData.length + " bytes");
        System.out.println("Message Type: " + messageType);
        System.out.println("Fee Sponsor: " + feeSponsor);
        System.out.println("Store Result : " + storeResult);

        // Get the message bridge contract
        SmartContract messageBridge = new SmartContract(messageBridgeHash, neow3j);

        // Get the current sending fee
        BigInteger sendingFee = messageBridge.callInvokeFunction("sendingFee").getInvocationResult()
                .getStack().get(0).getInteger();
        System.out.println("Sending Fee: " + sendingFee + " GAS fractions");

        // Build the transaction based on message type
        Transaction tx;
        if (isExecutable) {
            System.out.println("\n--- Sending Executable Message ---");
            // Send executable message: sendExecutableMessage(rawMessage, storeResult, feeSponsor, sendingFee)
            tx = messageBridge.invokeFunction("sendExecutableMessage",
                    byteArray(messageData),
                    bool(storeResult),
                    hash160(feeSponsor),
                    integer(sendingFee)
            ).signers(global(senderAccount)).sign();
        } else {
            System.out.println("\n--- Sending Store-Only Message ---");
            // Send store-only message: sendMessage(rawMessage, feeSponsor, sendingFee)
            tx = messageBridge.invokeFunction("sendMessage",
                    byteArray(messageData),
                    hash160(feeSponsor),
                    integer(sendingFee)
            ).signers(global(senderAccount)).sign();
        }

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
        NeoApplicationLog appLog = neow3j.getApplicationLog(txHash).send().getApplicationLog();
        NeoGetTransaction transaction = neow3j.getTransaction(txHash).send();
        Hash256 blockHash = transaction.getResult().getBlockHash();
        NeoBlock block = neow3j.getBlock(blockHash, false).send().getBlock();
        System.out.println("Included in Block: " + block.getIndex());

        if (appLog.getExecutions().get(0).getState().equals(NeoVMStateType.HALT)) {
            System.out.println("Message sent successfully!");

            // Look for MessageSend event in the logs
            appLog.getExecutions().get(0).getNotifications().stream()
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
            System.out.println("Exception: " + appLog.getExecutions().get(0).getException());
        }
    }
}
