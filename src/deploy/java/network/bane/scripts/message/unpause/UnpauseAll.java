package network.bane.scripts.message.unpause;

import io.neow3j.protocol.Neow3j;
import io.neow3j.types.Hash160;
import io.neow3j.wallet.Account;

import static network.bane.scripts.message.unpause.Unpause.unpause;
import static network.bane.scripts.message.unpause.UnpauseExecuting.unpauseExecuting;
import static network.bane.scripts.message.unpause.UnpauseSending.unpauseSending;
import static network.bane.utils.env.EnvVariables.MESSAGE_BRIDGE_HASH;
import static network.bane.utils.env.EnvVariables.WALLET_FILEPATH_GOVERNOR;
import static network.bane.utils.env.EnvVariables.WALLET_PASSWORD_GOVERNOR;
import static network.bane.utils.env.EnvVariables.getEnvVariable;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.wallet.LoadWallet.getAccountFromWallet;

public class UnpauseAll {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        Hash160 bridgeHash = getHash160FromEnvVar(MESSAGE_BRIDGE_HASH);

        String governorWalletFilepath = getEnvVariable(WALLET_FILEPATH_GOVERNOR);
        String governorWalletPassword = getEnvVariable(WALLET_PASSWORD_GOVERNOR);

        Account governor = getAccountFromWallet(governorWalletFilepath, governorWalletPassword);

        unpause(neow3j, bridgeHash, governor);
        unpauseSending(neow3j, bridgeHash, governor);
        unpauseExecuting(neow3j, bridgeHash, governor);
    }

}
