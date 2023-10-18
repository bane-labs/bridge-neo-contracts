package network.bane.util;

import io.neow3j.contract.SmartContract;
import io.neow3j.crypto.Base64;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.ContractStorageEntry;
import io.neow3j.types.Hash160;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static io.neow3j.utils.Numeric.cleanHexPrefix;
import static io.neow3j.utils.Numeric.toHexString;

public class SmartContractHelper extends SmartContract {

    public SmartContractHelper(Hash160 scriptHash, Neow3j neow3j) {
        super(scriptHash, neow3j);
    }

    public List<ContractStorageEntry> findStorage(String prefixHex) throws IOException {
        return neow3j.findStorage(scriptHash, prefixHex, BigInteger.ZERO).send().getFoundStorage().getStorageEntries();
    }

    public String getStorage(String keyHex) throws IOException {
        byte[] storageBytes = Base64.decode(neow3j.getStorage(scriptHash, keyHex).send().getStorage());
        return toHexString(storageBytes);
    }

    public String getStorageNoPrefix(String keyHex) throws IOException {
        return cleanHexPrefix(getStorage(keyHex));
    }

}
