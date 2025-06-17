package network.bane.util;

import io.neow3j.contract.NefFile;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.ObjectMapperFactory;
import io.neow3j.protocol.core.response.ContractManifest;
import io.neow3j.protocol.core.response.InvocationResult;
import io.neow3j.transaction.TransactionBuilder;
import io.neow3j.types.CallFlags;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;
import network.bane.util.helper.SmartContractHelper;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.transaction.AccountSigner.none;
import static io.neow3j.types.ContractParameter.any;
import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.types.ContractParameter.string;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static network.bane.util.TestHelper.owner;

public class MessageExecutor extends SmartContractHelper {


    public MessageExecutor(Hash160 scriptHash, Neow3j neow3j) {
        super(scriptHash, neow3j);
    }

    // region update

    public Hash256 update(NefFile nefFile, ContractManifest manifest, ContractParameter data) throws Throwable {
        return update(owner, nefFile, manifest, data);
    }

    public Hash256 update(Account sender, NefFile nefFile, ContractManifest manifest, ContractParameter data)
            throws Throwable {
        if (nefFile == null) {
            throw new IllegalArgumentException("NefFile cannot be null");
        } else if (manifest == null) {
            throw new IllegalArgumentException("ContractManifest cannot be null");
        } else {
            byte[] manifestBytes = ObjectMapperFactory.getObjectMapper().writeValueAsBytes(manifest);
            if (manifestBytes.length > 65535) {
                throw new IllegalArgumentException(format("The given contract manifest is too long. Manifest was %d " +
                        "bytes big, but a max of %d bytes is allowed.", manifestBytes.length, 65535));
            } else {
                TransactionBuilder b = data == null ?
                        invokeFunction("update", byteArray(nefFile.toArray()), byteArray(manifestBytes), any(null)) :
                        invokeFunction("update", byteArray(nefFile.toArray()), byteArray(manifestBytes), any(data));
                return sendAndAwaitExecution(b.signers(calledByEntry(sender)));
            }
        }
    }

    // endregion
    // region read

    public InvocationResult serializeMessage(Hash160 contract, String method, CallFlags callFlags,
            List<ContractParameter> params) throws IOException {
        return callInvokeFunction("serializeMessage", asList(hash160(contract), string(method),
                integer(callFlags.getValue()), array(params))).getInvocationResult();
    }

    public InvocationResult serializeMessage(int timestamp, Hash160 msgSender, Hash160 contract, String method,
            CallFlags callFlags, List<ContractParameter> params) throws IOException {
        return callInvokeFunction("serializeMessage", asList(integer(timestamp), hash160(msgSender),
                hash160(contract), string(method), integer(callFlags.getValue()), array(params))).getInvocationResult();
    }

    public InvocationResult serializeInvocation(Hash160 contract, String method, CallFlags callFlags,
            List<ContractParameter> params) throws IOException {
        return callInvokeFunction("serializeInvocation", asList(hash160(contract), string(method),
                integer(callFlags.getValue()), array(params))).getInvocationResult();
    }

    public InvocationResult deserializeMessage(byte[] serializedMessage) throws IOException {
        return callInvokeFunction("deserializeMessage", asList(byteArray(serializedMessage))).getInvocationResult();
    }

    public InvocationResult deserializeInvocation(byte[] serializedInvocation) throws IOException {
        return callInvokeFunction("deserializeInvocation", asList(byteArray(serializedInvocation))).getInvocationResult();
    }

    public InvocationResult getInvocation(byte[] serializedInvocation) throws IOException {
        return callInvokeFunction("getInvocation", asList(byteArray(serializedInvocation))).getInvocationResult();
    }

    public InvocationResult getMessage(int id) throws IOException {
        return callInvokeFunction("getMessage", asList(integer(id))).getInvocationResult();
    }

    public InvocationResult getMessageDeserialized(int id) throws IOException {
        return callInvokeFunction("getMessageDeserialized", asList(integer(id))).getInvocationResult();
    }

    // endregion
    // region store/execute

    public Hash256 storeMessage(Account sender, BigInteger timestamp, Hash160 msgSender, byte[] invocation) throws Throwable {
        return sendAndAwaitExecution(
                invokeFunction("storeMessage", integer(timestamp), hash160(msgSender), byteArray(invocation))
                        .signers(none(sender)));
    }

    public Hash256 executeMessage(Account sender, BigInteger id) throws Throwable {
        return sendAndAwaitExecution(invokeFunction("executeMessage", integer(id)).signers(none(sender)));
    }

    // endregion

}
