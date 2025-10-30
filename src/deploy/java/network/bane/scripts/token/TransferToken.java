package network.bane.scripts.token;

import io.neow3j.contract.FungibleToken;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;

import java.math.BigInteger;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static network.bane.utils.env.EnvVariables.N3_JSON_RPC;
import static network.bane.utils.env.EnvVariables.WALLET_PASSWORD_PERSONAL;
import static network.bane.utils.env.EnvVariables.WALLET_FILEPATH_PERSONAL;
import static network.bane.utils.env.EnvVariables.TOKEN_DEPOSIT_AMOUNT;
import static network.bane.utils.env.EnvVariables.TOKEN_DEPOSIT_RECIPIENT_ON_EVM;
import static network.bane.utils.env.EnvVariables.TOKEN_TRANSFER_TOKEN_HASH;
import static network.bane.utils.wallet.LoadWallet.getAccountFromWallet;

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

    // The following are the required env variables for running this script
    private static final Neow3j neow3j = Neow3j.build(new HttpService(N3_JSON_RPC));
    private static final String personalWalletPath = WALLET_FILEPATH_PERSONAL;
    private static final String personalWalletPassword = WALLET_PASSWORD_PERSONAL;
    private static final Hash160 tokenHash = TOKEN_TRANSFER_TOKEN_HASH;
    private static final Hash160 to = TOKEN_DEPOSIT_RECIPIENT_ON_EVM;
    private static final BigInteger amount = TOKEN_DEPOSIT_AMOUNT;

    public static void main(String[] args) throws Throwable {
        Account signerAcc = getAccountFromWallet(personalWalletPath, personalWalletPassword);
        Hash160 from = signerAcc.getScriptHash();

        Transaction tx = new FungibleToken(tokenHash, neow3j).transfer(from, to, amount)
                .signers(calledByEntry(signerAcc))
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
