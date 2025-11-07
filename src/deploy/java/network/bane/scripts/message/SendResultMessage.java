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
import static network.bane.scripts.message.MessageSendHelper.getMessageSendEvents;
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
 * Run with: gradle run -PmainClass=network.bane.scripts.message.SendResultMessage
 */
public class SendResultMessage {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        SmartContract messageBridge = new SmartContract(getHash160FromEnvVar(MESSAGE_BRIDGE_HASH), neow3j);
        String personalWalletPath = getEnvVariable(WALLET_FILEPATH_PERSONAL);
        String personalWalletPassword = getEnvVariable(WALLET_PASSWORD_PERSONAL);

        String nonceStr = getEnvVariable(MESSAGE_NONCE);

        Account senderAcc = getAccountFromWallet(personalWalletPath, personalWalletPassword);

        System.out.println("=== Message Bridge - Send Result Message ===");
        System.out.println("Using Message Bridge Contract: " + messageBridge.getScriptHash());
        System.out.println("Related nonce: " + nonceStr);

        // Get the current sending fee
        BigInteger sendingFee = messageBridge.callFunctionReturningInt("sendingFee");
        System.out.printf("Sending Fee: %s GAS%n", GasToken.toDecimals(sendingFee, 8));

        BigInteger nonce = new BigInteger(nonceStr);

        // Invoking: sendResultMessage(rawMessage, storeResult, feeSponsor, sendingFee)
        Transaction tx = messageBridge.invokeFunction("sendResultMessage",
                        integer(nonce),
                        hash160(senderAcc.getScriptHash()),
                        integer(sendingFee)
                )
                .signers(none(senderAcc).setAllowedContracts(GasToken.SCRIPT_HASH))
                .sign();

        System.out.println("\n--- Sending Executable Message ---");
        NeoApplicationLog log = sendTransaction(neow3j, tx);
        getMessageSendEvents(log, messageBridge.getScriptHash());
    }
}
