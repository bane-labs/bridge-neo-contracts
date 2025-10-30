package network.bane.scripts.message;

import io.neow3j.contract.GasToken;
import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.transaction.Transaction;
import io.neow3j.wallet.Account;

import java.math.BigInteger;

import static io.neow3j.transaction.AccountSigner.none;
import static io.neow3j.types.ContractParameter.any;
import static io.neow3j.types.ContractParameter.bool;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.utils.Numeric.hexStringToByteArray;
import static network.bane.scripts.message.SendMessage.printSendingMessageInfo;
import static network.bane.scripts.message.SendMessage.sendMessageSendTransaction;
import static network.bane.utils.env.EnvVariables.MESSAGE_BRIDGE_HASH;
import static network.bane.utils.env.EnvVariables.MESSAGE_SEND_EXECUTABLE_MESSAGE;
import static network.bane.utils.env.EnvVariables.MESSAGE_SEND_EXECUTABLE_STORE_BOOL;
import static network.bane.utils.env.EnvVariables.N3_JSON_RPC;
import static network.bane.utils.env.EnvVariables.WALLET_PASSWORD_PERSONAL;
import static network.bane.utils.env.EnvVariables.WALLET_FILEPATH_PERSONAL;
import static network.bane.utils.wallet.LoadWallet.getAccountFromWallet;

/**
 * Sends an executable message
 * <p>
 * Requires the following environment variables to be set:
 * - N3_JSON_RPC: The RPC endpoint of the N3 node
 * - MESSAGE_BRIDGE_HASH: Hash of the deployed message bridge contract
 * - WALLET_FILEPATH_PERSONAL: the filepath to the personal wallet. This wallet is used to send the message.
 * - WALLET_PASSWORD_PERSONAL: the password for the personal wallet
 * - MESSAGE_SEND_EXECUTABLE_MESSAGE: The message to send as a hex string.
 * - MESSAGE_SEND_EXECUTABLE_STORE_BOOL: Boolean indicating whether to store the result of the execution.
 * <p>
 * Run with: gradle run -PmainClass=network.bane.scripts.message.SendExecutableMessage
 */
public class SendExecutableMessage {

    // The following are the required env variables for running this script
    private static final Neow3j neow3j = Neow3j.build(new HttpService(N3_JSON_RPC));
    private static final SmartContract messageBridge = new SmartContract(MESSAGE_BRIDGE_HASH, neow3j);
    private static final String personalWalletPath = WALLET_FILEPATH_PERSONAL;
    private static final String personalWalletPassword = WALLET_PASSWORD_PERSONAL;
    private static final String messageToSend = MESSAGE_SEND_EXECUTABLE_MESSAGE;

    public static void main(String[] args) throws Throwable {
        boolean storeResult = true;
        try {
            storeResult = MESSAGE_SEND_EXECUTABLE_STORE_BOOL;
        } catch (Exception ignore) {
        }
        Account senderAcc = getAccountFromWallet(personalWalletPath, personalWalletPassword);

        System.out.println("=== Message Bridge - Send Executable Message ===");
        System.out.println("Using Message Bridge Contract: " + messageBridge.getScriptHash());

        byte[] messageData = hexStringToByteArray(messageToSend);
        printSendingMessageInfo(senderAcc, messageData, "Executable");
        System.out.println("Store Result: " + storeResult);

        // Get the current sending fee
        BigInteger sendingFee = messageBridge.callFunctionReturningInt("sendingFee");
        System.out.printf("Sending Fee: %s GAS%n", GasToken.toDecimals(sendingFee, 8));
        BigInteger maxFee = sendingFee;

        // Invoking: sendExecutableMessage(rawMessage, storeResult, feeSponsor, sendingFee)
        Transaction tx = messageBridge.invokeFunction("sendExecutableMessage",
                        byteArray(messageData),
                        bool(storeResult),
                        any(null),
                        integer(maxFee)
                )
                .signers(none(senderAcc).setAllowedContracts(GasToken.SCRIPT_HASH))
                .sign();

        System.out.println("\n--- Sending Executable Message ---");
        sendMessageSendTransaction(neow3j, tx, messageBridge.getScriptHash());
    }

}
