package network.bane.utils;

import io.neow3j.crypto.exceptions.CipherException;
import io.neow3j.crypto.exceptions.NEP2InvalidFormat;
import io.neow3j.crypto.exceptions.NEP2InvalidPassphrase;
import io.neow3j.wallet.Account;
import io.neow3j.wallet.Wallet;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static network.bane.utils.GetEnv.getEnvVariable;

public class LoadWallet {

    public static Account getOwnerAccountFromWallet() throws NEP2InvalidPassphrase, NEP2InvalidFormat,
            CipherException, IOException {
        String filePath = getEnvVariable("NEON3_OWNER_WALLET");
        String password = getEnvVariable("NEON3_OWNER_PASSWORD");
        return getAccountFromWallet(filePath, password);
    }

    public static Account getDeployerAccountFromWallet() throws NEP2InvalidPassphrase, NEP2InvalidFormat,
            CipherException, IOException {
        String filePath = getEnvVariable("NEON3_DEPLOYER_WALLET");
        String password = getEnvVariable("NEON3_DEPLOYER_PASSWORD");
        Account acc = getAccountFromWallet(filePath, password);
        return acc;
    }

    private static Account getAccountFromWallet(String filePath, String password) throws NEP2InvalidPassphrase,
            NEP2InvalidFormat, CipherException, IOException {
        Path path = Paths.get(filePath);
        Wallet wallet = Wallet.fromNEP6Wallet(path.toFile());
        Account acc = wallet.getDefaultAccount();
        acc.decryptPrivateKey(password);
        return acc;
    }

}
