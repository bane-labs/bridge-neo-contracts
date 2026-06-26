package network.bane.scripts.message;

import io.neow3j.contract.GasToken;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoApplicationLog;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;
import network.bane.client.MessageBridgeClient;

import java.math.BigInteger;

import static io.neow3j.transaction.AccountSigner.none;
import static io.neow3j.utils.Numeric.hexStringToByteArray;
import static io.neow3j.utils.Numeric.isValidHexString;
import static network.bane.scripts.message.MessageSendHelper.getMessageSendEvents;
import static network.bane.scripts.message.MessageSendHelper.printSendingMessageInfo;
import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.PrintHelper.printSender;
import static network.bane.utils.env.EnvVariables.MESSAGE_SEND_STORE_ONLY_MESSAGE;
import static network.bane.utils.env.EnvVariables.getEnvVariable;
import static network.bane.utils.env.EnvVariables.getMessageBridgeClientFromEnv;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.env.EnvWallets.getPersonalAccountFromEnv;

/**
 * Sends a store-only message
 * <p>
 * Requires the following environment variables to be set:
 * - N3_JSON_RPC: The RPC endpoint of the N3 node
 * - WALLET_FILEPATH_PERSONAL: the filepath to the personal wallet. This wallet is used to send the message.
 * - WALLET_PASSWORD_PERSONAL: the password for the personal wallet
 * - MESSAGE_BRIDGE_HASH: Hash of the deployed message bridge contract
 * - MESSAGE_SEND_STORE_ONLY_MESSAGE: The message to send as a hex string or UTF-8 string. If the string is a valid
 * hex string, it will be interpreted as hex, otherwise as UTF-8.
 * <p>
 * Run with: ./gradlew runOps -PmainClass=network.bane.scripts.message.SendStoreOnlyMessage
 */
public class SendStoreOnlyMessage {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        MessageBridgeClient messageBridge = getMessageBridgeClientFromEnv(neow3j);
        String messageToSend = getEnvVariable(MESSAGE_SEND_STORE_ONLY_MESSAGE);
        Account personalAccount = getPersonalAccountFromEnv();
        Hash160 personalScriptHash = personalAccount.getScriptHash();

        System.out.println("=== Message Bridge - Send Store-Only Message ===");
        printNetwork(neow3j);
        printSender(personalScriptHash);
        System.out.println("Using Message Bridge Contract: " + messageBridge.getScriptHash());

        byte[] messageData = getMessageDataBytes(messageToSend);
        printSendingMessageInfo(personalAccount, messageData, "Store-Only");

        // Get the current sending fee
        BigInteger sendingFee = messageBridge.sendingFee();
        System.out.printf("Sending Fee: %s GAS%n", GasToken.toDecimals(sendingFee, 8));

        Hash256 txHash = messageBridge.sendStoreOnlyMessage(messageData, personalScriptHash, sendingFee)
                .withSigners(none(personalAccount).setAllowedContracts(GasToken.SCRIPT_HASH))
                .signSendAndAwait(System.out);

        NeoApplicationLog appLog = neow3j.getApplicationLog(txHash).send().getApplicationLog();
        getMessageSendEvents(appLog, messageBridge.getScriptHash());
    }

    private static byte[] getMessageDataBytes(String messageToSend) {
        if (isValidHexString(messageToSend)) {
            return hexStringToByteArray(messageToSend);
        } else {
            System.out.println("Provided message is not in hexadecimal format - using UTF-8 bytes");
            return messageToSend.getBytes();
        }
    }

}
