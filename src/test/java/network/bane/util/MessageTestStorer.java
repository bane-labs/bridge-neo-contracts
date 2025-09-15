package network.bane.util;

import io.neow3j.contract.NefFile;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.ObjectMapperFactory;
import io.neow3j.protocol.core.response.ContractManifest;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.TransactionBuilder;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;
import network.bane.util.helper.SmartContractHelper;
import network.bane.util.structs.N3MessageDto;
import network.bane.util.structs.N3MessageMetadataDto;
import network.bane.util.structs.N3MessageMetadataExecDto;
import network.bane.util.structs.N3MessageMetadataResultDto;
import network.bane.util.structs.N3MessageMetadataStoreOnlyDto;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.transaction.AccountSigner.none;
import static io.neow3j.types.ContractParameter.any;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.types.ContractParameter.string;
import static java.util.Arrays.asList;
import static network.bane.util.TestHelper.owner;
import static network.bane.util.helper.TestHelper.alice;
import static network.bane.util.helper.TestHelper.messageBridge;

public class MessageTestStorer extends SmartContractHelper {

    public MessageTestStorer(Hash160 scriptHash, Neow3j neow3j) {
        super(scriptHash, neow3j);
    }

    // region bridge

    public Hash256 setMessageBridge() throws Throwable {
        return sendAndAwaitExecution(
                invokeFunction("setMessageBridge", hash160(messageBridge.getScriptHash())).signers(none(alice))
        );
    }

    // endregion
    // region storing

    public Hash256 storeMetadataOfExecutingMessage() throws Throwable {
        return sendAndAwaitExecution(invokeFunction("storeMetadataOfExecutingMessage").signers(none(alice)));
    }

    public static N3MessageMetadataDto getMetadataFromStackItem(StackItem stackItem) {
        List<StackItem> items = stackItem.getList();
        int type = items.get(0).getInteger().intValue();
        BigInteger timestamp = items.get(1).getInteger();
        Hash160 sender = Hash160.fromAddress(items.get(2).getAddress());
        if (type == N3MessageDto.MESSAGE_TYPE_STORE_ONLY) {
            return new N3MessageMetadataStoreOnlyDto(timestamp, sender);
        }
        if (type == N3MessageDto.MESSAGE_TYPE_EXECUTABLE) {
            boolean storeResult = items.get(3).getBoolean();
            return new N3MessageMetadataExecDto(timestamp, sender, storeResult);
        }
        if (type == N3MessageDto.MESSAGE_TYPE_RESULT) {
            BigInteger initialMsgNonce = items.get(3).getInteger();
            return new N3MessageMetadataResultDto(timestamp, sender, initialMsgNonce);
        }
        throw new IllegalStateException("Unexpected metadata format");
    }

    public boolean hasMetadataStored(BigInteger nonce) throws IOException {
        try {
            getStoredMetadata(nonce);
        } catch (IllegalStateException e) {
            return false;
        }
        return true;
    }

    public N3MessageMetadataDto getStoredMetadata(BigInteger nonce) throws IOException {
        StackItem metadataItem = callInvokeFunction("getStoredMetadata",
                asList(integer(nonce))).getInvocationResult().getFirstStackItem();
        if (metadataItem.getValue() == null) {
            throw new IllegalStateException("No metadata found for nonce: " + nonce);
        }
        return getMetadataFromStackItem(metadataItem);
    }

    // endregion
    // region metadata

    public Hash256 storeValueAndReturnIt(String key, ContractParameter value) throws Throwable {
        return storeValueAndReturnIt(none(alice), key, value);
    }

    public Hash256 storeValueAndReturnIt(AccountSigner signer, String key, ContractParameter value) throws Throwable {
        return sendAndAwaitExecution(invokeFunction("storeValueAndReturnIt", string(key), value).signers(signer));
    }

    public StackItem getStoredValue(String key) throws IOException {
        return callInvokeFunction("getStoredValue", asList(string(key))).getInvocationResult().getFirstStackItem();
    }

    // endregion
    // region update

    public Hash256 update(NefFile newNefFile, ContractManifest newManifest, ContractParameter data) throws Throwable {
        return update(owner, newNefFile, newManifest, data);
    }

    public Hash256 update(Account sender, NefFile newNefFile, ContractManifest newManifest, ContractParameter data)
            throws Throwable {
        if (newNefFile == null) {
            throw new IllegalArgumentException("The NEF file cannot be null.");
        } else if (newManifest == null) {
            throw new IllegalArgumentException("The manifest cannot be null.");
        } else {
            byte[] manifestBytes = ObjectMapperFactory.getObjectMapper().writeValueAsBytes(newManifest);
            if (manifestBytes.length > 65535) {
                throw new IllegalArgumentException(String.format("The given contract manifest is too long. Manifest " +
                        "was %d bytes big, but a max of %d bytes is allowed.", manifestBytes.length, 65535));
            } else {
                TransactionBuilder b = data == null ?
                        invokeFunction("update", byteArray(newNefFile.toArray()), byteArray(manifestBytes), any(null)) :
                        invokeFunction("update", byteArray(newNefFile.toArray()), byteArray(manifestBytes), data);
                return sendAndAwaitExecution(b.signers(calledByEntry(sender)));
            }
        }
    }

    // endregion

}
