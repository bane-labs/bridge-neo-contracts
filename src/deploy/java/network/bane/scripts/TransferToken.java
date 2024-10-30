package network.bane.scripts;

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
import static network.bane.utils.env.EnvVariables.NODE;
import static network.bane.utils.env.GetEnv.getEnvVariable;
import static network.bane.utils.wallet.LoadWallet.getOwnerAccountFromWallet;

public class TransferToken {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = Neow3j.build(new HttpService(NODE));
        Hash160 tokenHash = new Hash160(getEnvVariable("TOKEN_HASH"));
        Account ownerAcc = getOwnerAccountFromWallet();
        Hash160 from = ownerAcc.getScriptHash();
        Hash160 to = new Hash160(getEnvVariable("BRIDGE_HASH"));
        BigInteger amount = new BigInteger("100000000000000000000");
        Transaction tx = new FungibleToken(tokenHash, neow3j).transfer(from, to, amount)
                .signers(calledByEntry(ownerAcc))
                .sign();

        NeoSendRawTransaction rawTx = tx.send();
        if (rawTx.hasError()) {
            throw new Exception("Error transferring token: " + rawTx.getError().getMessage());
        }
        Hash256 txHash = rawTx.getSendRawTransaction().getHash();
        waitUntilTransactionIsExecuted(txHash, neow3j);
        System.out.println("Token transferred successfully.");
        System.out.println("Transaction hash: " + txHash);
    }

}
