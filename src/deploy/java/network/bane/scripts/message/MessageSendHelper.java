package network.bane.scripts.message;

import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoApplicationLog;
import io.neow3j.protocol.core.response.NeoBlock;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.protocol.core.stackitem.ArrayStackItem;
import io.neow3j.protocol.core.stackitem.ByteStringStackItem;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.types.NeoVMStateType;
import io.neow3j.utils.Numeric;
import io.neow3j.wallet.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static io.neow3j.utils.Numeric.toHexString;
import static java.util.Collections.singletonList;

class MessageSendHelper {

    private static final Logger log = LoggerFactory.getLogger(MessageSendHelper.class);

    static NeoApplicationLog sendTransaction(Neow3j neow3j, Transaction tx) throws Exception {
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

        return tx.getApplicationLog();
    }

    static void getMessageSendEvents(NeoApplicationLog log, Hash160 messageBridgeHash) {
        NeoApplicationLog.Execution exec = log.getFirstExecution();
        if (exec.getState().equals(NeoVMStateType.HALT)) {
            System.out.println("Transaction successful!");

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

    static void printSendingMessageInfo(Account senderAcc, byte[] messageData, String typeString) {
        System.out.println("Sending message (hex): " + toHexString(messageData));
        System.out.println("Sender: " + senderAcc.getScriptHash());
        System.out.println("Message Data (hex): " + toHexString(messageData));
        System.out.println("Message Data Size: " + messageData.length + " bytes");
        System.out.println("Message Type: " + typeString);
    }

    static void printStateRoot(SmartContract messageBridge, String getRootMethodName) throws IOException {
        List<StackItem> evmToNeoRootResult = messageBridge.callInvokeFunction(getRootMethodName).getInvocationResult().getStack();
        byte[] evmToNeoRootBytes = ((ByteStringStackItem) evmToNeoRootResult.get(0)).getValue();
        System.out.println(getRootMethodName + ": " + Numeric.toHexString(evmToNeoRootBytes));
    }

    static void checkExecutionResult(SmartContract messageBridge, BigInteger nonce) throws IOException {
        List<StackItem> objectResult = messageBridge.callInvokeFunction(
                "getNeoExecutionResult",
                singletonList(integer(nonce))
        ).getInvocationResult().getStack();
        System.out.println("Execution result for nonce " + nonce + ":" + objectResult);

        List<StackItem> serializedResult = messageBridge.callInvokeFunction(
                "getSerializedNeoExecutionResult",
                singletonList(integer(nonce))
        ).getInvocationResult().getStack();
        if (!serializedResult.isEmpty() && serializedResult.get(0).getValue() != null) {
            byte[] resultBytes = ((ByteStringStackItem) serializedResult.get(0)).getValue();
            if (resultBytes.length > 0) {
                System.out.println("\n--- Execution Result for nonce " + nonce + " ---");
                System.out.println("Result bytes length: " + resultBytes.length);
                System.out.println("Result as hex: " + Numeric.toHexString(resultBytes));
                System.out.println("Result as UTF-8: " + new String(resultBytes, StandardCharsets.UTF_8));
            } else {
                System.out.println("No execution result stored.");
            }
        }
    }

    static boolean checkThatMessageExists(SmartContract messageBridge, BigInteger nonce) throws IOException {
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
