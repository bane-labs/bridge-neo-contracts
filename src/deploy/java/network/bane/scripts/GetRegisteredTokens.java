package network.bane.scripts;

import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;

import java.io.IOException;
import java.util.List;

import static io.neow3j.types.ContractParameter.hash160;
import static network.bane.scripts.TokenBridge.TokenConfig;
import static network.bane.scripts.TokenBridge.State;
import static java.util.Arrays.asList;
import static network.bane.utils.env.EnvVariables.NODE;
import static network.bane.utils.env.GetEnv.getEnvVariable;

public class GetRegisteredTokens {

    public static void main(String[] args) throws IOException {
        Neow3j neow3j = Neow3j.build(new HttpService(NODE));
        Hash160 bridgeAddress = new Hash160(getEnvVariable("BRIDGE_HASH"));
        SmartContract bridge = new SmartContract(bridgeAddress, neow3j);
        List<StackItem> registeredTokensStack = bridge.callInvokeFunction("getRegisteredTokens")
                .getInvocationResult().getFirstStackItem().getList();

        for (StackItem stackItem : registeredTokensStack) {
            Hash160 token = Hash160.fromAddress(stackItem.getAddress());
            List<StackItem> tokenBridgeStack = bridge.callInvokeFunction("getTokenBridge", asList(hash160(token)))
                    .getInvocationResult().getFirstStackItem().getList();
            boolean paused = tokenBridgeStack.get(0).getBoolean();
            List<StackItem> depositStateStack = tokenBridgeStack.get(1).getList();
            State depositState = new State(depositStateStack.get(0).getInteger(),
                    new Hash256(depositStateStack.get(1).getHexString()));
            List<StackItem> withdrawalStateStack = tokenBridgeStack.get(2).getList();
            State withdrawalState = new State(withdrawalStateStack.get(0).getInteger(),
                    new Hash256(withdrawalStateStack.get(1).getHexString()));
            List<StackItem> configStack = tokenBridgeStack.get(3).getList();
            TokenConfig config = new TokenConfig(
                    Hash160.fromAddress(configStack.get(0).getAddress()),
                    configStack.get(1).getInteger(),
                    configStack.get(2).getInteger(),
                    configStack.get(3).getInteger(),
                    configStack.get(4).getInteger().intValue(),
                    configStack.get(5).getInteger().intValue()
            );

            TokenBridge tokenBridge = new TokenBridge(paused, depositState, withdrawalState, config);
            System.out.println(token);
            System.out.println(tokenBridge);
            System.out.println();
        }
    }
}
