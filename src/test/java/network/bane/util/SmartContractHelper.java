package network.bane.util;

import io.neow3j.contract.SmartContract;
import io.neow3j.crypto.Base64;
import io.neow3j.protocol.Neow3j;
import io.neow3j.types.Hash160;

import java.io.IOException;

import static io.neow3j.utils.Numeric.toHexString;

public class SmartContractHelper extends SmartContract {

    public SmartContractHelper(Hash160 scriptHash, Neow3j neow3j) {
        super(scriptHash, neow3j);
    }

    public String getStorage(String keyHex) throws IOException {
        return toHexString(Base64.decode(neow3j.getStorage(scriptHash, keyHex).send().getStorage()));
    }

}
