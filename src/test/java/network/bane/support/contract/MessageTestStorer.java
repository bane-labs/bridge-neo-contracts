package network.bane.util;

import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import network.bane.util.helper.SmartContractHelper;
import network.bane.dto.message.N3Message;
import network.bane.dto.message.N3MessageMetadata;
import network.bane.dto.message.N3MessageMetadataExec;
import network.bane.dto.message.N3MessageMetadataResult;
import network.bane.dto.message.N3MessageMetadataStoreOnly;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static io.neow3j.transaction.AccountSigner.none;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.types.ContractParameter.string;
import static java.util.Arrays.asList;
import static network.bane.support.TestEnvironment.alice;
import static network.bane.support.TestEnvironment.messageBridge;

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

    public static N3MessageMetadata getMetadataFromStackItem(StackItem stackItem) {
        List<StackItem> items = stackItem.getList();
        int type = items.get(0).getInteger().intValue();
        BigInteger timestamp = items.get(1).getInteger();
        Hash160 sender = Hash160.fromAddress(items.get(2).getAddress());
        if (type == N3Message.MESSAGE_TYPE_STORE_ONLY) {
            return new N3MessageMetadataStoreOnly(timestamp, sender);
        }
        if (type == N3Message.MESSAGE_TYPE_EXECUTABLE) {
            boolean storeResult = items.get(3).getBoolean();
            return new N3MessageMetadataExec(timestamp, sender, storeResult);
        }
        if (type == N3Message.MESSAGE_TYPE_RESULT) {
            BigInteger initialMsgNonce = items.get(3).getInteger();
            return new N3MessageMetadataResult(timestamp, sender, initialMsgNonce);
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

    public N3MessageMetadata getStoredMetadata(BigInteger nonce) throws IOException {
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

}
