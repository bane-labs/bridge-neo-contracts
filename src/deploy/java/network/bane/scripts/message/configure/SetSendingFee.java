package network.bane.scripts.message.configure;

import io.neow3j.contract.GasToken;
import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;

import java.math.BigInteger;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.PrintHelper.printSender;
import static network.bane.utils.env.EnvVariables.MESSAGE_BRIDGE_HASH;
import static network.bane.utils.env.EnvVariables.MESSAGE_SENDING_FEE;
import static network.bane.utils.env.EnvVariables.getBigIntegerFromEnvVar;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.env.EnvWallets.getGovernorAccountFromEnv;

public class SetSendingFee {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        Hash160 msgBridgeHash = getHash160FromEnvVar(MESSAGE_BRIDGE_HASH);
        BigInteger sendingFee = getBigIntegerFromEnvVar(MESSAGE_SENDING_FEE);
        GasToken gasToken = new GasToken(neow3j);

        Account governorAcc = getGovernorAccountFromEnv();

        System.out.println("Setting sending fee in message bridge...");
        printNetwork(neow3j);
        printSender(governorAcc.getScriptHash());
        System.out.printf("New fee: %s (%s %s)", sendingFee, gasToken.toDecimals(sendingFee), gasToken.getSymbol());

        SmartContract msgBridge = new SmartContract(msgBridgeHash, neow3j);

        BigInteger currentSendingFee = msgBridge.callFunctionReturningInt("sendingFee");
        if (currentSendingFee.equals(sendingFee)) {
            System.out.println("\nSending Fee is already set to the desired value - no action needed");
            return;
        }
        System.out.printf("\nCurrent sending fee:    %s (%s $GAS)", currentSendingFee,
                gasToken.toDecimals(currentSendingFee));
        System.out.printf("\nSetting sending fee to: %s (%s $GAS)\n", sendingFee, gasToken.toDecimals(sendingFee));

        Transaction tx = msgBridge.invokeFunction("setSendingFee", integer(sendingFee))
                .signers(calledByEntry(governorAcc))
                .sign();

        NeoSendRawTransaction rawTxResponse = tx.send();
        if (rawTxResponse.hasError()) {
            throw new Exception("Error setting sendingFee MessageBridge: " + rawTxResponse.getError().getMessage());
        }
        Hash256 txHash = rawTxResponse.getSendRawTransaction().getHash();
        System.out.println("Transaction sent: " + txHash);
        waitUntilTransactionIsExecuted(txHash, neow3j);
        System.out.println("Transaction confirmed in block: " + neow3j.getTransactionHeight(txHash).send().getHeight());

        if (!msgBridge.callFunctionReturningInt("sendingFee").equals(sendingFee)) {
            throw new Exception("Sending fee is not set to the desired value");
        }
        System.out.printf("\nSending fee successfully set to: %s (%s $GAS)", sendingFee,
                gasToken.toDecimals(sendingFee));
        System.out.println();
    }

}
