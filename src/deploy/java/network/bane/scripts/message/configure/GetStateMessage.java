package network.bane.scripts.message.configure;

import io.neow3j.contract.GasToken;
import io.neow3j.protocol.Neow3j;
import network.bane.client.MessageBridgeClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.env.EnvVariables.getMessageBridgeClientFromEnv;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;

public class GetStateMessage {

    public static void main(String[] args) throws IOException {
        Neow3j neow3j = getNeow3jFromEnv();
        MessageBridgeClient messageBridge = getMessageBridgeClientFromEnv(neow3j);

        System.out.println("Get message bridge state...");
        printNetwork(neow3j);

        GasToken gasToken = new GasToken(neow3j);
        BigDecimal feeDecimal = gasToken.toDecimals(messageBridge.sendingFee());

        System.out.printf("Message Bridge:\n");
        System.out.printf(" paused:           %s\n", messageBridge.isPaused());
        System.out.printf(" sending paused:   %s\n", messageBridge.sendingIsPaused());
        System.out.printf(" executing paused: %s\n", messageBridge.executingIsPaused());
        System.out.printf(" evmToNeo state:\n");
        System.out.printf("  nonce: %s\n", messageBridge.evmToNeoNonce());
        System.out.printf("  root:  %s\n", messageBridge.evmToNeoRoot());
        System.out.printf(" neoToEvm state:\n");
        System.out.printf("  nonce: %s\n", messageBridge.neoToEvmNonce());
        System.out.printf("  root:  %s\n", messageBridge.neoToEvmRoot());
        System.out.printf(" config:\n");
        System.out.printf("  sending fee:               %s (%s $GAS)\n", messageBridge.sendingFee(), feeDecimal);
        System.out.printf("  max message size:          %s\n", messageBridge.maxMessageSize());
        System.out.printf("  max nr messages per relay: %s\n", messageBridge.maxNrMessages());
        System.out.printf("  execution manager:         %s\n", messageBridge.executionManager());
        BigInteger execMillis = messageBridge.executionWindowMilliseconds();
        System.out.printf("  execution window (ms):     %s (%s days)\n", execMillis,
                execMillis.divide(BigInteger.valueOf(1000 * 60 * 60 * 24)));
    }

}
