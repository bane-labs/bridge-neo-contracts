package network.bane.scripts.token;

import io.neow3j.contract.FungibleToken;
import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;

import java.math.BigDecimal;
import java.math.BigInteger;

import static io.neow3j.transaction.AccountSigner.global;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static network.bane.utils.env.EnvVariables.NODE;
import static network.bane.utils.env.GetEnv.getEnvVariable;
import static network.bane.utils.wallet.LoadWallet.getOwnerAccountFromWallet;

public class DepositNative {

    public static final BigInteger DEFAULT_NATIVE_FEE = FungibleToken.toFractions(new BigDecimal("0.1"), 8);

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = Neow3j.build(new HttpService(NODE));

        Hash160 bridgeAddress = new Hash160(getEnvVariable("BRIDGE_HASH"));
        Account from = getOwnerAccountFromWallet();
        Hash160 to = new Hash160(getEnvVariable("DEFAULT_RECIPIENT_ON_NEOX"));
        BigInteger amount = FungibleToken.toFractions(new BigDecimal("6"), 8);
        BigInteger maxFee = DEFAULT_NATIVE_FEE;

        SmartContract bridge = new SmartContract(bridgeAddress, neow3j);
        Transaction tx = bridge.invokeFunction("depositNative",
                        hash160(from),
                        hash160(to),
                        integer(amount),
                        integer(maxFee)
                ).signers(global(from))
                .sign();

        NeoSendRawTransaction rawTxResponse = tx.send();
        if (rawTxResponse.hasError()) {
            throw new Exception("Error registering token: " + rawTxResponse.getError().getMessage());
        }

        Hash256 txHash = rawTxResponse.getSendRawTransaction().getHash();
        waitUntilTransactionIsExecuted(txHash, neow3j);
        System.out.println("Token registered successfully.");
        System.out.println("Transaction hash: " + txHash);
    }
}
