package network.bane.scripts.token.unpause;

import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.types.Hash160;
import io.neow3j.wallet.Account;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static network.bane.scripts.token.unpause.Unpause.unpause;
import static network.bane.scripts.token.unpause.UnpauseDeposits.unpauseDeposits;
import static network.bane.scripts.token.unpause.UnpauseNativeBridge.unpauseNativeBridge;
import static network.bane.scripts.token.unpause.UnpauseTokenBridge.unpauseTokenBridge;
import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.PrintHelper.printSender;
import static network.bane.utils.env.EnvVariables.BRIDGE_HASH;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.env.EnvWallets.getGovernorAccountFromEnv;

public class UnpauseAll {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        Hash160 bridgeHash = getHash160FromEnvVar(BRIDGE_HASH);

        Account governorAcc = getGovernorAccountFromEnv();

        System.out.println("Unpausing all components of the token bridge...");
        printNetwork(neow3j);
        printSender(governorAcc.getScriptHash());

        unpause(neow3j, bridgeHash, governorAcc);
        unpauseNativeBridge(neow3j, bridgeHash, governorAcc);
        unpauseDeposits(neow3j, bridgeHash, governorAcc);

        for (Hash160 token : getRegisteredTokenBridges(neow3j, bridgeHash)) {
            unpauseTokenBridge(neow3j, bridgeHash, token, governorAcc);
        }
    }

    private static List<Hash160> getRegisteredTokenBridges(Neow3j neow3j, Hash160 bridgeHash) throws IOException {
        return new SmartContract(bridgeHash, neow3j).callInvokeFunction("getRegisteredTokens")
                .getInvocationResult().getFirstStackItem().getList().stream()
                .map(i -> Hash160.fromAddress(i.getAddress()))
                .collect(Collectors.toList());
    }

}
