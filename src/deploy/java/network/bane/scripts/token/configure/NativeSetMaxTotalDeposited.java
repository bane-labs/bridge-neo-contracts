package network.bane.scripts.token.configure;

import io.neow3j.contract.FungibleToken;
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
import static network.bane.utils.env.EnvVariables.NATIVE_SET_MAX_TOTAL_DEPOSITED;
import static network.bane.utils.env.EnvVariables.getBigIntegerFromEnvVar;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.env.EnvWallets.getGovernorAccountFromEnv;

public class NativeSetMaxTotalDeposited {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        SmartContract bridge = new SmartContract(getHash160FromEnvVar(BRIDGE_HASH), neow3j);
        BigInteger maxTotalDeposited = getBigIntegerFromEnvVar(NATIVE_SET_MAX_TOTAL_DEPOSITED);

        FungibleToken token = new FungibleToken(bridge.callFunctionReturningScriptHash("nativeToken"), neow3j);

        Account governorAcc = getGovernorAccountFromEnv();

        System.out.println("Setting native max total deposited...");
        System.out.printf("New value: %s (%s $%s) %n" + maxTotalDeposited, token.toDecimals(maxTotalDeposited),
                token.getSymbol());
        printNetwork(neow3j);
        printSender(governorAcc.getScriptHash());


        Transaction tx = bridge.invokeFunction("setMaxTotalDepositedNative", integer(maxTotalDeposited))
                .signers(calledByEntry(governorAcc))
                .sign();

        NeoSendRawTransaction rawTxResponse = tx.send();
        if (rawTxResponse.hasError()) {
            throw new Exception("Error setting native max total deposited: " + rawTxResponse.getError().getMessage());
        }

        Hash256 txHash = rawTxResponse.getSendRawTransaction().getHash();
        waitUntilTransactionIsExecuted(txHash, neow3j);

        BigInteger actualMaxTotalDeposited = bridge.callFunctionReturningInt("maxTotalDepositedNative");
        System.out.println("Native max total deposited set successfully");
        System.out.printf("New native max total deposited: %s $%s%n", token.toDecimals(actualMaxTotalDeposited),
                token.getSymbol());
        System.out.println("Transaction hash: " + txHash);
    }

}
