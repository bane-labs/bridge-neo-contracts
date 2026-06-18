package network.bane.scripts.message;

import io.neow3j.protocol.core.response.NeoApplicationLog;
import io.neow3j.protocol.core.stackitem.ArrayStackItem;
import io.neow3j.protocol.core.stackitem.ByteStringStackItem;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.types.Hash160;
import io.neow3j.types.NeoVMStateType;
import io.neow3j.wallet.Account;
import network.bane.client.MessageBridgeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.utils.Numeric.toHexString;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;

class MessageSendHelper {

    private static final Logger log = LoggerFactory.getLogger(MessageSendHelper.class);

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

    static void checkExecutionResult(MessageBridgeClient messageBridge, BigInteger nonce) throws IOException {
        StackItem neoExecutionResult = messageBridge.getNeoExecutionResult(nonce);
        System.out.println("Execution result for nonce " + nonce + ":" + neoExecutionResult);
        try {
            byte[] resultBytes = messageBridge.getSerializedNeoExecutionResult(nonce);
            System.out.println("\n--- Execution Result for nonce " + nonce + " ---");
            System.out.println("Result bytes length: " + resultBytes.length);
            System.out.println("Result as hex: " + toHexString(resultBytes));
            System.out.println("Result as UTF-8: " + new String(resultBytes, UTF_8));
        } catch (IndexOutOfBoundsException e) {
            System.out.println("No execution result stored.");
        }
    }

    static boolean checkThatMessageExists(MessageBridgeClient messageBridge, BigInteger nonce) throws IOException {
        // Check if message exists and print its details
        List<StackItem> messageResult = messageBridge.callInvokeFunction("getMessage", asList(integer(nonce)))
                .getInvocationResult().getStack();
        if (!messageResult.isEmpty() && messageResult.get(0).getValue() != null) {
            System.out.println("Message found for nonce: " + nonce);
            // Print message details
            System.out.println("Message Details:");
            StackItem item = messageResult.get(0);
            System.out.println("  Message: " +
                    toHexString(((ByteStringStackItem) ((ArrayStackItem) item).getValue().get(1)).getValue())
            );
        } else {
            System.err.println("ERROR: No message found for nonce: " + nonce);
            return true;
        }
        return false;
    }
}
