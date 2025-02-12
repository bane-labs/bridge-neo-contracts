package network.bane.util.helper;

import io.neow3j.contract.ContractManagement;
import io.neow3j.contract.FungibleToken;
import io.neow3j.contract.NefFile;
import io.neow3j.protocol.ObjectMapperFactory;
import io.neow3j.protocol.core.response.ContractManifest;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.test.ContractTestExtension;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.types.Hash160;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Paths;

import static io.neow3j.contract.SmartContract.calcContractHash;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static network.bane.util.TestHelper.owner;

public class TokenDeployment {

    public static FungibleToken deployTestToken(ContractTestExtension ext) throws Throwable {
        File contractNefFile = Paths.get("src", "test", "resources", "TestToken.nef").toFile();
        NefFile nefFile = NefFile.readFromFile(contractNefFile);

        File manifestFile = Paths.get("src", "test", "resources", "TestToken.manifest.json").toFile();
        ContractManifest manifest;
        try (FileInputStream s = new FileInputStream(manifestFile)) {
            manifest = ObjectMapperFactory.getObjectMapper().readValue(s, ContractManifest.class);
        }
        NeoSendRawTransaction response =
                new ContractManagement(ext.getNeow3j()).deploy(nefFile, manifest, hash160(owner))
                        .signers(AccountSigner.calledByEntry(owner))
                        .sign()
                        .send();
        waitUntilTransactionIsExecuted(response.getSendRawTransaction().getHash(), ext.getNeow3j());
        Hash160 tokenHash = calcContractHash(owner.getScriptHash(), nefFile.getCheckSumAsInteger(), manifest.getName());
        return new FungibleToken(tokenHash, ext.getNeow3j());
    }
}
