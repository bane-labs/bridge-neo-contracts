package network.bane.scripts.message;

import io.neow3j.contract.GasToken;
import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoApplicationLog;
import io.neow3j.transaction.Transaction;
import io.neow3j.wallet.Account;

import java.math.BigInteger;

import static io.neow3j.transaction.AccountSigner.none;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.utils.Numeric.hexStringToByteArray;
import static io.neow3j.utils.Numeric.isValidHexString;
import static network.bane.scripts.message.MessageSendHelper.getMessageSendEvents;
import static network.bane.scripts.message.MessageSendHelper.printSendingMessageInfo;
import static network.bane.scripts.message.MessageSendHelper.sendTransaction;
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
 * - MESSAGE_BRIDGE_HASH: Hash of the deployed message bridge contract
 * - MESSAGE_SEND_STORE_ONLY_MESSAGE: The message to send as a hex string or UTF-8 string. If the string is a valid
 *    hex string, it will be interpreted as hex, otherwise as UTF-8.
 * <p>
 * Run with: gradle run -PmainClass=network.bane.scripts.message.SendStoreOnlyMessage
 */
public class SendStoreOnlyMessage {

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

        // Invoking: sendStoreOnlyMessage(rawMessage, feeSponsor, sendingFee)
        Transaction tx = messageBridge.invokeFunction("sendStoreOnlyMessage",
                        byteArray(messageData),
                        hash160(senderAcc),
                        integer(maxFee)
                )
                .signers(none(senderAcc).setAllowedContracts(GasToken.SCRIPT_HASH))
                .sign();

        System.out.println("\n--- Sending Store-Only Message ---");
        NeoApplicationLog log = sendTransaction(neow3j, tx);
        getMessageSendEvents(log, messageBridge.getScriptHash());
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
