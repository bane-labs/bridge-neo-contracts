package network.bane.client;

import io.neow3j.contract.NefFile;
import io.neow3j.crypto.Base64;
import io.neow3j.crypto.ECKeyPair;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.ContractManifest;
import io.neow3j.protocol.core.response.ContractStorageEntry;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.utils.Numeric.toHexString;

public class ManagementTestClient extends ManagementClient {

    public ManagementTestClient(Hash160 scriptHash, Neow3j neow3j) {
        super(scriptHash, neow3j);
    }

    public Hash256 setRelayer(Account sender, Hash160 newRelayer) throws Throwable {
        return setRelayer(newRelayer).withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 addValidator(Account sender, ECKeyPair.ECPublicKey validator, boolean incrementThreshold)
            throws Throwable {
        return addValidator(validator, incrementThreshold).withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 removeValidator(Account sender, ECKeyPair.ECPublicKey validator, boolean decrementThreshold)
            throws Throwable {
        return removeValidator(validator, decrementThreshold).withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 replaceValidator(Account sender, ECKeyPair.ECPublicKey oldValidator,
            ECKeyPair.ECPublicKey newValidator) throws Throwable {
        return replaceValidator(oldValidator, newValidator).withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 setValidatorThreshold(Account sender, int newThreshold) throws Throwable {
        return setValidatorThreshold(newThreshold).withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 setGovernor(Account sender, Hash160 newGovernor) throws Throwable {
        return setGovernor(newGovernor).withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 setSecurityGuard(Account sender, Hash160 newSecurityGuard) throws Throwable {
        return setSecurityGuard(newSecurityGuard).withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 update(Account sender, NefFile nefFile, ContractManifest manifest, Object data) throws Throwable {
        return update(nefFile, manifest, data).withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    // Generic

    public List<ContractStorageEntry> findStorage(String prefixHex) throws IOException {
        return neow3j.findStorage(scriptHash, prefixHex, BigInteger.ZERO).send().getFoundStorage().getStorageEntries();
    }

    public String getStorage(String keyHex) throws IOException {
        byte[] storageBytes = Base64.decode(neow3j.getStorage(scriptHash, keyHex).send().getStorage());
        return toHexString(storageBytes);
    }
}
