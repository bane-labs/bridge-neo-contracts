package network.bane.scripts.token.configure;

import io.neow3j.contract.FungibleToken;
import io.neow3j.contract.GasToken;
import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;

import java.math.BigInteger;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.PrintHelper.printSender;
import static network.bane.utils.env.EnvVariables.BRIDGE_HASH;
import static network.bane.utils.env.EnvVariables.NATIVE_DEPOSIT_FEE;
import static network.bane.utils.env.EnvVariables.getBigIntegerFromEnvVar;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.env.EnvWallets.getGovernorAccountFromEnv;

public class NativeSetDepositFee {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        SmartContract bridge = new SmartContract(getHash160FromEnvVar(BRIDGE_HASH), neow3j);
        BigInteger depositFee = getBigIntegerFromEnvVar(NATIVE_DEPOSIT_FEE);

        GasToken gasToken = new GasToken(neow3j);
        FungibleToken token = new FungibleToken(bridge.callFunctionReturningScriptHash("nativeToken"), neow3j);

        Account governorAcc = getGovernorAccountFromEnv();

        System.out.println("Setting native deposit fee...");
        printNetwork(neow3j);
        printSender(governorAcc.getScriptHash());
        System.out.printf("New fee: %s (%s $%s)%n", depositFee, gasToken.toDecimals(depositFee), gasToken.getSymbol());

        Transaction tx = bridge.invokeFunction("setNativeDepositFee", integer(depositFee))
                .signers(calledByEntry(governorAcc))
                .sign();

        NeoSendRawTransaction rawTxResponse = tx.send();
        if (rawTxResponse.hasError()) {
            throw new Exception("Error setting native deposit fee: " + rawTxResponse.getError().getMessage());
        }

        Hash256 txHash = rawTxResponse.getSendRawTransaction().getHash();
        waitUntilTransactionIsExecuted(txHash, neow3j);

        BigInteger actualDepositFee = bridge.callFunctionReturningInt("nativeDepositFee");
        System.out.println("Native deposit fee set successfully");
        System.out.printf("New native deposit fee: %s $%s%n", token.toDecimals(actualDepositFee), token.getSymbol());
        System.out.println("Transaction hash: " + txHash);
    }

}
