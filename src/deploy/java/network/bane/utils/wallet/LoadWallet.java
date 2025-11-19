package network.bane.utils.wallet;

import io.neow3j.crypto.exceptions.CipherException;
import io.neow3j.crypto.exceptions.NEP2InvalidFormat;
import io.neow3j.crypto.exceptions.NEP2InvalidPassphrase;
import io.neow3j.wallet.Account;
import io.neow3j.wallet.Wallet;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LoadWallet {

    public static Account getAccountFromWallet(String filePath, String password)
            throws NEP2InvalidPassphrase, NEP2InvalidFormat, CipherException, IOException {
        Path path = Paths.get(filePath);
        Wallet wallet = Wallet.fromNEP6Wallet(path.toFile());
        Account acc = wallet.getDefaultAccount();
        acc.decryptPrivateKey(password);
        return acc;
    }

}
