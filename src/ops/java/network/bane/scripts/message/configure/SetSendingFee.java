package network.bane.scripts.message.configure;

import io.neow3j.contract.GasToken;
import io.neow3j.protocol.Neow3j;
import io.neow3j.wallet.Account;
import network.bane.client.MessageBridgeClient;

import java.math.BigInteger;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.PrintHelper.printSender;
import static network.bane.utils.env.EnvVariables.MESSAGE_SENDING_FEE;
import static network.bane.utils.env.EnvVariables.getBigIntegerFromEnvVar;
import static network.bane.utils.env.EnvVariables.getMessageBridgeClientFromEnv;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.env.EnvWallets.getGovernorAccountFromEnv;

public class SetSendingFee {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        MessageBridgeClient messageBridge = getMessageBridgeClientFromEnv(neow3j);
        BigInteger sendingFee = getBigIntegerFromEnvVar(MESSAGE_SENDING_FEE);
        GasToken gasToken = new GasToken(neow3j);

        Account governorAcc = getGovernorAccountFromEnv();

        System.out.println("Setting sending fee in message bridge...");
        printNetwork(neow3j);
        printSender(governorAcc.getScriptHash());
        System.out.printf("New fee: %s (%s %s)", sendingFee, gasToken.toDecimals(sendingFee), gasToken.getSymbol());

        BigInteger currentSendingFee = messageBridge.sendingFee();
        if (currentSendingFee.equals(sendingFee)) {
            System.out.println("\nSending Fee is already set to the desired value - no action needed");
            return;
        }
        System.out.printf("\nCurrent sending fee:    %s (%s $GAS)", currentSendingFee,
                gasToken.toDecimals(currentSendingFee));
        System.out.printf("\nSetting sending fee to: %s (%s $GAS)\n", sendingFee, gasToken.toDecimals(sendingFee));

        messageBridge.setSendingFee(sendingFee).withSigners(calledByEntry(governorAcc)).signSendAndAwait(System.out);

        if (!messageBridge.sendingFee().equals(sendingFee)) {
            throw new Exception("Sending fee is not set to the desired value");
        }
        System.out.printf("\nSending fee successfully set to: %s (%s $GAS)", sendingFee,
                gasToken.toDecimals(sendingFee));
    }

}
