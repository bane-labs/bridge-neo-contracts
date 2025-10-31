package network.bane.scripts.message;

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

import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static io.neow3j.utils.Numeric.toHexString;

class MessageSendHelper {

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

}
