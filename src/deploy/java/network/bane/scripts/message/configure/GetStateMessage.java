package network.bane.scripts.message.configure;

import io.neow3j.contract.GasToken;
import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.stackitem.StackItem;
import network.bane.utils.structs.MessageBridge;

import java.io.IOException;
import java.math.BigDecimal;

import static network.bane.utils.env.EnvVariables.MESSAGE_BRIDGE_HASH;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;

public class GetStateMessage {

    public static void main(String[] args) throws IOException {
        Neow3j neow3j = getNeow3jFromEnv();
        SmartContract bridge = new SmartContract(getHash160FromEnvVar(MESSAGE_BRIDGE_HASH), neow3j);

        StackItem msgBridgeStateItem = bridge.callInvokeFunction("getMessageBridge").getInvocationResult()
                .getFirstStackItem();
        MessageBridge messageBridgeState = MessageBridge.fromStackItem(msgBridgeStateItem);
        boolean paused = bridge.callFunctionReturningBool("isPaused");
        boolean sendingPaused = bridge.callFunctionReturningBool("sendingIsPaused");
        boolean executingPaused = bridge.callFunctionReturningBool("executingIsPaused");

        GasToken gasToken = new GasToken(neow3j);
        BigDecimal feeDecimal = gasToken.toDecimals(messageBridgeState.config.fee);

        System.out.println();
        System.out.printf("Message Bridge:\n");
        System.out.printf(" paused:           %s\n", paused);
        System.out.printf(" sending paused:   %s\n", sendingPaused);
        System.out.printf(" executing paused: %s\n", executingPaused);
        System.out.printf(" evmToNeo state:\n");
        System.out.printf("  nonce: %s\n", messageBridgeState.evmToNeoState.nonce);
        System.out.printf("  root:  %s\n", messageBridgeState.evmToNeoState.root);
        System.out.printf(" neoToEvm state:\n");
        System.out.printf("  nonce: %s\n", messageBridgeState.neoToEvmState.nonce);
        System.out.printf("  root:  %s\n", messageBridgeState.neoToEvmState.root);
        System.out.printf(" config:\n");
        System.out.printf("  sending fee:               %s (%s $GAS)\n", messageBridgeState.config.fee, feeDecimal);
        System.out.printf("  max message size:          %s\n", messageBridgeState.config.maxMessageSize);
        System.out.printf("  max nr messages per relay: %s\n", messageBridgeState.config.maxNrMessages);
        System.out.printf("  execution manager:         %s\n", messageBridgeState.config.executionManager);
        System.out.printf("  execution window (ms):     %s (%s days)\n",
                messageBridgeState.config.executionWindowMilliseconds,
                messageBridgeState.config.executionWindowMilliseconds / 3600000 / 24);
    }

}
