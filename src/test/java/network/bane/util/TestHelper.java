package network.bane.util;

import io.neow3j.crypto.ECKeyPair;
import io.neow3j.crypto.ECKeyPair.ECPublicKey;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.wallet.Account;

import java.util.List;

import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.types.ContractParameter.publicKey;

public class TestHelper {

    // Account names available in the neo-express config file.
    public static final String ALICE = "NM7Aky765FG8NhhwtxjXRx7jEL1cnw7PBP";
    public static final String BOB = "NZpsgXn9VQQoLexpuXJsrX8BsoyAhKUyiX";
    public static final String CHARLIE = "NdbtgSku2qLuwsBBzLx3FLtmmMdm32Ktor";
    public static final String DENISE = "NerDv9t8exrQRrP11jjvZKXzSXvTnmfDTo";
    public static final String EVE = "NZ539Rd57v5NEtAdkHyFGaWj1uGt2DecUL";
    public static final String FLORIAN = "NRy5bp81kScYFZHLfMBXuubFfRyboVyu7G";

    public static final Account owner = Account.create();
    public static final ECPublicKey ownerPubKey = owner.getECKeyPair().getPublicKey();
    public static final Hash160 ownerScriptHash = owner.getScriptHash();

    public static final Account relayer = Account.fromWIF("L5iiAW1NicU3znJfcBAbDgFyVy9dudd1HjBLeNtacaG73JcjMymU");
    public static final ECPublicKey relayerPubKey = relayer.getECKeyPair().getPublicKey();
    public static final Hash160 relayerScriptHash = relayer.getScriptHash();

    public static final Account validator1 = Account.fromWIF("L46dW4Z8KEvURXaE1YrqsgNSaQ4G2B4uN97uNeyhJp1VL7UjLUpb");
    public static final ECPublicKey validator1PubKey = validator1.getECKeyPair().getPublicKey();
    public static final Hash160 validator1ScriptHash = validator1.getScriptHash();
    public static final Account validator2 = Account.fromWIF("Kz9FY9FcmP1HpKPBNACYeZGY2dRv5DKDMgSrM3DA3ZhxmQXduqCj");
    public static final ECPublicKey validator2PubKey = validator2.getECKeyPair().getPublicKey();
    public static final Hash160 validator2ScriptHash = validator2.getScriptHash();
    public static final Account validator3 = Account.fromWIF("KzrpcAzUr8QFkE83rhGb55XNq5bP6Nze63J54GG61zbu5Un62HjZ");
    public static final ECPublicKey validator3PubKey = validator3.getECKeyPair().getPublicKey();
    public static final Hash160 validator3ScriptHash = validator3.getScriptHash();
    public static final Account validator4 = Account.fromWIF("Kyc8rihRGy3pT8z24zMdUV7zPgNaCtrLhmpFTJEEk8QkASnzp2o6");
    public static final ECPublicKey validator4PubKey = validator4.getECKeyPair().getPublicKey();
    public static final Hash160 validator4ScriptHash = validator4.getScriptHash();
    public static final Account validator5 = Account.fromWIF("KxaXJG4Vt6hbZrLdjCdHG1LJYUJaE8knPkefxw8uyDVuNXh4RYXM");
    public static final ECPublicKey validator5PubKey = validator5.getECKeyPair().getPublicKey();
    public static final Hash160 validator5ScriptHash = validator5.getScriptHash();
    public static final Account validator6 = Account.fromWIF("KyvW6BeMagHm1jUnBytHEsZNvZU7mmJejoybK9RWzzyjxGeRJbdj");
    public static final ECPublicKey validator6PubKey = validator6.getECKeyPair().getPublicKey();
    public static final Hash160 validator6ScriptHash = validator6.getScriptHash();
    public static final Account validator7 = Account.fromWIF("L5kcMEa2zFagyEF9TnVGi3Y5uP4NeCaEnQyZGo93wiv8aAAPTVWk");
    public static final ECPublicKey validator7PubKey = validator7.getECKeyPair().getPublicKey();
    public static final Hash160 validator7ScriptHash = validator7.getScriptHash();

    public static final Account account1 = Account.fromWIF("L49FvTGZTdSJbck687Kg9wrjBzQBJ1asnvKWqcvJ2sJcyP9U9rbJ");
    public static final Account account2 = Account.fromWIF("KwPnYQg2VFMq2Jn62DTVKYCg7TGdzFWvsfKkzKYgPi2LSJcxEW7D");
    public static final Account account3 = Account.fromWIF("KzLcmDDahZdkZPgbwgicjMEASo6qLP5B6TtpvdDT9hd4pgjmSMms");
    public static final Account account4 = Account.fromWIF("KxpH1KLjefnEt7Xr5wkDonQimg4DvjXGh5FWVbgDgmtp9FJSkui9");
    public static final Account account5 = Account.fromWIF("KwuK42Lwmkv3hhTSWoiUfrbUMzkQ4DZqbcQPf9C3X8DxZSXvKmia");
    public static final Account account6 = Account.fromWIF("KxvThFfpAQjo74b2onr9DuNRSp7J4PDiihdkKGRutn51foSA6yHs");
    public static final Account account7 = Account.fromWIF("L2Kmh1imZCNiDBgLztASH8NR4ydBG1eu5jSvqzJSybAvxh7v2GBs");
    public static final Account account8 = Account.fromWIF("KzTvj76TPsaxww9zW5xWncnPrSnBNkbuKvYS7j7TxnG1JMS141oB");
    public static final Account account9 = Account.fromWIF("L5W8RKdjDWXeBuyTpLNv5yquGphkS6m3YHSKGhBmXB1itz2Kxo3Q");

    // Hardhat signers 10-19
    public static final Hash160 recipient0 = new Hash160("0xBcd4042DE499D14e55001CcbB24a551F3b954096");
    public static final Hash160 recipient1 = new Hash160("0x71bE63f3384f5fb98995898A86B02Fb2426c5788");
    public static final Hash160 recipient2 = new Hash160("0xFABB0ac9d68B0B445fB7357272Ff202C5651694a");
    public static final Hash160 recipient3 = new Hash160("0x1CBd3b2770909D4e10f157cABC84C7264073C9Ec");
    public static final Hash160 recipient4 = new Hash160("0xdF3e18d64BC6A983f673Ab319CCaE4f1a57C7097");
    public static final Hash160 recipient5 = new Hash160("0xcd3B766CCDd6AE721141F452C550Ca635964ce71");
    public static final Hash160 recipient6 = new Hash160("0x2546BcD3c84621e976D8185a91A922aE77ECEc30");
    public static final Hash160 recipient7 = new Hash160("0xbDA5747bFD65F08deb54cb465eB87D40e51B197E");
    public static final Hash160 recipient8 = new Hash160("0xdD2FD4581271e230360230F9337D5c0430Bf44C0");
    public static final Hash160 recipient9 = new Hash160("0x8626f6940E2eb28930eFb4CeF49B2d1F2C9C1199");

    public static ContractParameter prepareManagementDeployParameter(
            ECKeyPair.ECPublicKey owner,
            ECKeyPair.ECPublicKey relayer,
            List<ECPublicKey> validators,
            Integer threshold
    ) {
        return array(
                publicKey(owner),
                publicKey(relayer),
                array(
                        publicKey(validators.get(0)),
                        publicKey(validators.get(1)),
                        publicKey(validators.get(2)),
                        publicKey(validators.get(3)),
                        publicKey(validators.get(4)),
                        publicKey(validators.get(5)),
                        publicKey(validators.get(6))
                ),
                integer(threshold)
        );
    }

}
