package network.bane.support.crypto;

import io.neow3j.crypto.ECKeyPair;
import io.neow3j.crypto.Sign;
import io.neow3j.wallet.Account;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static network.bane.support.hash.TokenBridgeHashChainHelper.createWithdrawalMessageToSign;
import static network.bane.support.TestConstants.DEFAULT_LINKED_CHAIN_ID;
import static network.bane.support.TestEnvironment.neow3j;

public class SignHelper {

    public static Map<ECKeyPair.ECPublicKey, Sign.SignatureData> signMsg(List<Account> validators, String root)
            throws IOException {
        return signMsg(DEFAULT_LINKED_CHAIN_ID, validators, root);
    }

    public static Map<ECKeyPair.ECPublicKey, Sign.SignatureData> signMsg(BigInteger linkedChainId, List<Account> validators,
            String root) throws IOException {

        BigInteger network = BigInteger.valueOf(neow3j.getVersion().send().getVersion().getProtocol().getNetwork());
        String msg = createWithdrawalMessageToSign(network, linkedChainId, root);
        Map<ECKeyPair.ECPublicKey, Sign.SignatureData> signatures = new HashMap<>();
        for (int i = 0; i < validators.size(); i++) {
            ECKeyPair validator = validators.get(i).getECKeyPair();
            signatures.put(validator.getPublicKey(), Sign.signHexMessage(msg, validator));
        }
        return signatures;
    }

}
