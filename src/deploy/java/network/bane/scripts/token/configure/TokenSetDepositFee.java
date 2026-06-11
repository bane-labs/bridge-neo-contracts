package network.bane.scripts.token.configure;

import io.neow3j.contract.FungibleToken;
import io.neow3j.contract.GasToken;
import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;

import java.math.BigInteger;
import java.util.HashMap;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.map;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.PrintHelper.printSender;
import static network.bane.utils.env.EnvVariables.BRIDGE_HASH;
import static network.bane.utils.env.EnvVariables.SETTING_TOKEN_HASH;
import static network.bane.utils.env.EnvVariables.TOKEN_DEPOSIT_FEE;
import static network.bane.utils.env.EnvVariables.getBigIntegerFromEnvVar;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.env.EnvWallets.getGovernorAccountFromEnv;

public class TokenSetDepositFee {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        SmartContract bridge = new SmartContract(getHash160FromEnvVar(BRIDGE_HASH), neow3j);
        Hash160 tokenHash = getHash160FromEnvVar(SETTING_TOKEN_HASH);
        BigInteger depositFee = getBigIntegerFromEnvVar(TOKEN_DEPOSIT_FEE);
        FungibleToken gasToken = new GasToken(neow3j);

        Account governorAcc = getGovernorAccountFromEnv();

        System.out.println("Setting token deposit fee...");
        printNetwork(neow3j);
        printSender(governorAcc.getScriptHash());
        System.out.println("Token:   " + tokenHash);
        System.out.printf("New fee: %s (%s $%s)%n", depositFee, gasToken.toDecimals(depositFee), gasToken.getSymbol());

        HashMap<Hash160, BigInteger> feeMap = new HashMap<>();
        feeMap.put(tokenHash, depositFee);

        Transaction tx = bridge.invokeFunction("setTokenDepositFee", map(feeMap))
                .signers(calledByEntry(governorAcc))
                .sign();

        NeoSendRawTransaction rawTxResponse = tx.send();
        if (rawTxResponse.hasError()) {
            throw new Exception("Error setting token deposit fee: " + rawTxResponse.getError().getMessage());
        }

        Hash256 txHash = rawTxResponse.getSendRawTransaction().getHash();
        waitUntilTransactionIsExecuted(txHash, neow3j);

        BigInteger actualDepositFee = bridge.callFunctionReturningInt("tokenDepositFee", hash160(tokenHash));
        System.out.println("Token deposit fee set successfully");
        System.out.printf("New token deposit fee of token %s: %s $GAS%n", tokenHash,
                new GasToken(neow3j).toDecimals(actualDepositFee));
        System.out.println("Transaction hash: " + txHash);
    }

}
