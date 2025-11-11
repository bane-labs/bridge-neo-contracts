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
import static network.bane.utils.env.EnvVariables.BRIDGE_HASH;
import static network.bane.utils.env.EnvVariables.WALLET_FILEPATH_GOVERNOR;
import static network.bane.utils.env.EnvVariables.WALLET_PASSWORD_GOVERNOR;
import static network.bane.utils.env.EnvVariables.getEnvVariable;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.wallet.LoadWallet.getAccountFromWallet;

public class UnpauseAll {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        Hash160 bridgeHash = getHash160FromEnvVar(BRIDGE_HASH);

        String governorWalletFilepath = getEnvVariable(WALLET_FILEPATH_GOVERNOR);
        String governorWalletPassword = getEnvVariable(WALLET_PASSWORD_GOVERNOR);

        Account governor = getAccountFromWallet(governorWalletFilepath, governorWalletPassword);

        unpause(neow3j, bridgeHash, governor);
        unpauseNativeBridge(neow3j, bridgeHash, governor);
        unpauseDeposits(neow3j, bridgeHash, governor);

        for (Hash160 token : getRegisteredTokenBridges(neow3j, bridgeHash)) {
            unpauseTokenBridge(neow3j, bridgeHash, token, governor);
        }
    }

    private static List<Hash160> getRegisteredTokenBridges(Neow3j neow3j, Hash160 bridgeHash) throws IOException {
        return new SmartContract(bridgeHash, neow3j).callInvokeFunction("getRegisteredTokens")
                .getInvocationResult().getFirstStackItem().getList().stream()
                .map(i -> Hash160.fromAddress(i.getAddress()))
                .collect(Collectors.toList());
    }

}
