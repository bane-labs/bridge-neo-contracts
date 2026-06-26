package network.bane.utils.wallet;

import io.neow3j.crypto.ECKeyPair;
import io.neow3j.types.Hash160;
import io.neow3j.wallet.Account;
import io.neow3j.wallet.Wallet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.neow3j.utils.Strings.capitaliseFirstLetter;
import static java.lang.String.format;

public class CreateWallet {

    public static void main(String[] args) throws Exception {

        // Set password and role here
        String password = "";
        // For example, use "owner" here and then "wallets/owner.json" for the NEON3_DEPLOYER_WALLET env variable.
        String role = "";

        Wallet wallet = Wallet.create(password).name("Bridge" + capitaliseFirstLetter(role));
        String filename = role + ".json";
        Path path = Paths.get("wallets", role + ".json");

        // Check if directory exists. If not, create it.
        if (!Files.exists(path.getParent())) {
            Files.createDirectory(path.getParent());
        }
        // Check if file exists
        if (Files.exists(path)) {
            throw new Exception(format("Wallet file %s already exists", filename));
        }
        Path filePath = Files.createFile(path);
        wallet.saveNEP6Wallet(filePath.toFile());

        Account defaultAccount = wallet.getDefaultAccount();
        String address = defaultAccount.getAddress();
        Hash160 scriptHash = defaultAccount.getScriptHash();
        defaultAccount.decryptPrivateKey(password);
        String wif = defaultAccount.getECKeyPair().exportAsWIF();
        ECKeyPair.ECPublicKey publicKey = defaultAccount.getECKeyPair().getPublicKey();

        System.out.println();
        System.out.println("################################# Wallet Generation #################################");
        System.out.println("Public Key:        02c5ed27dc59bb6e1b3e7b058691de25285435c698a904c01494e0816bb7c968cb");
        System.out.println("Address:           " + address);
        System.out.println("ScriptHash:        " + scriptHash);
        System.out.println("Public Key:        " + publicKey.getEncodedCompressedHex());
        System.out.println("Private Key (WIF): " + wif);
        System.out.println();
        System.out.println("Wallet file:       " + filePath);
        System.out.println("Wallet file path:  " + filePath.toAbsolutePath());
        System.out.println("############################## End of Wallet Generation #############################");
    }

}
