package network.bane.scripts.token;

import io.neow3j.contract.FungibleToken;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;

import java.math.BigInteger;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.PrintHelper.printSender;
import static network.bane.utils.env.EnvVariables.TOKEN_DEPOSIT_AMOUNT;
import static network.bane.utils.env.EnvVariables.TOKEN_DEPOSIT_RECIPIENT_ON_EVM;
import static network.bane.utils.env.EnvVariables.TOKEN_TRANSFER_TOKEN_HASH;
import static network.bane.utils.env.EnvVariables.getBigIntegerFromEnvVar;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.env.EnvWallets.getPersonalAccountFromEnv;

/**
 * This script transfers a specified amount of a fungible token from a personal wallet to a recipient address on EVM.
 * <p>
 * Requires the following environment variables to be set:
 * - N3_JSON_RPC: The RPC endpoint of the N3 node
 * - WALLET_FILEPATH_PERSONAL: the filepath to the personal wallet. This wallet is used to send the tokens.
 * - WALLET_PASSWORD_PERSONAL: the password for the personal wallet
 * - TOKEN_TRANSFER_TOKEN_HASH: The hash of the fungible token to transfer
 * - TOKEN_DEPOSIT_AMOUNT: The amount of tokens to transfer
 * - TOKEN_DEPOSIT_RECIPIENT_ON_EVM: The recipient address on EVM to receive the tokens
 * <p>
 * Run with: gradle run -PmainClass=network.bane.scripts.token.TransferToken
 */
public class TransferToken {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        Hash160 tokenHash = getHash160FromEnvVar(TOKEN_TRANSFER_TOKEN_HASH);
        FungibleToken token = new FungibleToken(tokenHash, neow3j);
        Hash160 to = getHash160FromEnvVar(TOKEN_DEPOSIT_RECIPIENT_ON_EVM);
        BigInteger amount = getBigIntegerFromEnvVar(TOKEN_DEPOSIT_AMOUNT);

        Account fromAcc = getPersonalAccountFromEnv();
        Hash160 from = fromAcc.getScriptHash();

        System.out.println("Transfer tokens...");
        printNetwork(neow3j);
        printSender(from);
        System.out.println("From:   " + from.toAddress());
        System.out.println("To:     " + to);
        System.out.printf("Amount: %s (%s %s)%n", amount, token.toDecimals(amount), token.getSymbol());

        Transaction tx = token.transfer(from, to, amount)
                .signers(calledByEntry(fromAcc))
                .sign();

        NeoSendRawTransaction rawTx = tx.send();
        if (rawTx.hasError()) {
            throw new Exception("Error transferring token: " + rawTx.getError().getMessage());
        }

        Hash256 txHash = rawTx.getSendRawTransaction().getHash();
        waitUntilTransactionIsExecuted(txHash, neow3j);
        System.out.println("Token transferred successfully");
        System.out.println("Transaction hash: " + txHash);
    }

}
