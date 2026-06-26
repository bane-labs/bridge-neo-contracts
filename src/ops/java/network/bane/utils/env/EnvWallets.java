package network.bane.utils.env;

import io.neow3j.crypto.exceptions.CipherException;
import io.neow3j.crypto.exceptions.NEP2InvalidFormat;
import io.neow3j.crypto.exceptions.NEP2InvalidPassphrase;
import io.neow3j.wallet.Account;

import java.io.IOException;

import static java.lang.Boolean.parseBoolean;
import static network.bane.utils.env.EnvVariables.WALLET_FILEPATH_DEPLOYER;
import static network.bane.utils.env.EnvVariables.WALLET_FILEPATH_GOVERNOR;
import static network.bane.utils.env.EnvVariables.WALLET_FILEPATH_OWNER;
import static network.bane.utils.env.EnvVariables.WALLET_FILEPATH_PERSONAL;
import static network.bane.utils.env.EnvVariables.WALLET_PASSWORD_DEPLOYER;
import static network.bane.utils.env.EnvVariables.WALLET_PASSWORD_GOVERNOR;
import static network.bane.utils.env.EnvVariables.WALLET_PASSWORD_OWNER;
import static network.bane.utils.env.EnvVariables.WALLET_PASSWORD_PERSONAL;
import static network.bane.utils.env.EnvVariables.WALLET_USE_WIF;
import static network.bane.utils.env.EnvVariables.WALLET_WIF_DEPLOYER;
import static network.bane.utils.env.EnvVariables.WALLET_WIF_GOVERNOR;
import static network.bane.utils.env.EnvVariables.WALLET_WIF_OWNER;
import static network.bane.utils.env.EnvVariables.WALLET_WIF_PERSONAL;
import static network.bane.utils.env.EnvVariables.getEnvVariable;
import static network.bane.utils.env.EnvVariables.getEnvVariableOrDefault;
import static network.bane.utils.wallet.LoadWallet.getAccountFromWallet;

public class EnvWallets {

    public static Account getPersonalAccountFromEnv()
            throws NEP2InvalidPassphrase, NEP2InvalidFormat, CipherException, IOException {
        return getAccountFromEnv(WALLET_FILEPATH_PERSONAL, WALLET_PASSWORD_PERSONAL, WALLET_WIF_PERSONAL);
    }

    public static Account getDeployerAccountFromEnv()
            throws NEP2InvalidPassphrase, NEP2InvalidFormat, CipherException, IOException {
        return getAccountFromEnv(WALLET_FILEPATH_DEPLOYER, WALLET_PASSWORD_DEPLOYER, WALLET_WIF_DEPLOYER);
    }

    public static Account getOwnerAccountFromEnv()
            throws NEP2InvalidPassphrase, NEP2InvalidFormat, CipherException, IOException {
        return getAccountFromEnv(WALLET_FILEPATH_OWNER, WALLET_PASSWORD_OWNER, WALLET_WIF_OWNER);
    }

    public static Account getGovernorAccountFromEnv()
            throws NEP2InvalidPassphrase, NEP2InvalidFormat, CipherException, IOException {
        return getAccountFromEnv(WALLET_FILEPATH_GOVERNOR, WALLET_PASSWORD_GOVERNOR, WALLET_WIF_GOVERNOR);
    }


    private static Account getAccountFromEnv(String walletFilepathEnv, String walletPasswordEnv, String walletWifEnv)
            throws NEP2InvalidPassphrase, NEP2InvalidFormat, CipherException, IOException {
        // Use wallet file with password by default
        boolean useWif = parseBoolean(getEnvVariableOrDefault(WALLET_USE_WIF, "false"));
        if (useWif) {
            return Account.fromWIF(getEnvVariable(walletWifEnv));
        } else {
            String governorWalletPath = getEnvVariable(walletFilepathEnv);
            String governorWalletPassword = getEnvVariable(walletPasswordEnv);
            return getAccountFromWallet(governorWalletPath, governorWalletPassword);
        }
    }

}
