package network.bane.scripts;

import io.neow3j.contract.FungibleToken;
import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;

import java.math.BigDecimal;
import java.math.BigInteger;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static network.bane.utils.env.EnvVariables.NODE;
import static network.bane.utils.env.GetEnv.getEnvVariable;
import static network.bane.utils.wallet.LoadWallet.getOwnerAccountFromWallet;

public class RegisterToken {

    public static final BigInteger DEFAULT_GAS_FEE = FungibleToken.toFractions(new BigDecimal("0.1"), 8);
    public static final BigInteger DEFAULT_MIN_AMOUNT = FungibleToken.toFractions(new BigDecimal("1"), 18);
    public static final BigInteger DEFAULT_MAX_AMOUNT = FungibleToken.toFractions(new BigDecimal("100000"), 18);
    public static final int DEFAULT_MAX_WITHDRAWALS = 100;
    public static final int DECIMAL_SCALING_FACTOR = 0;

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = Neow3j.build(new HttpService(NODE));

        Hash160 tokenHash = new Hash160(getEnvVariable("TOKEN_HASH"));
        Hash160 bridgeAddress = new Hash160(getEnvVariable("BRIDGE_HASH"));

        SmartContract bridge = new SmartContract(bridgeAddress, neow3j);
        Transaction tx = bridge.invokeFunction("registerToken",
                        hash160(tokenHash),
                        array(
                                hash160(new Hash160(getEnvVariable("NEOX_TOKEN_HASH"))),
                                integer(DEFAULT_GAS_FEE),
                                integer(DEFAULT_MIN_AMOUNT),
                                integer(DEFAULT_MAX_AMOUNT),
                                integer(DEFAULT_MAX_WITHDRAWALS),
                                integer(DECIMAL_SCALING_FACTOR)

                        )
                ).signers(calledByEntry(getOwnerAccountFromWallet()))
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
