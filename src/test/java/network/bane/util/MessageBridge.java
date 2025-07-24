package network.bane.util;

import io.neow3j.contract.NefFile;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.ObjectMapperFactory;
import io.neow3j.protocol.core.response.ContractManifest;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.Signer;
import io.neow3j.transaction.TransactionBuilder;
import io.neow3j.types.CallFlags;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.utils.Numeric;
import io.neow3j.wallet.Account;
import network.bane.util.helper.SmartContractHelper;
import network.bane.util.structs.MessageBridgeDto;
import network.bane.util.structs.N3MessageDto;
import network.bane.util.structs.N3MessageMetadataExecDto;
import network.bane.util.structs.N3MessageMetadataResultDto;
import network.bane.util.structs.N3MessageMetadataStoreOnlyDto;
import network.bane.util.structs.State;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.types.ContractParameter.any;
import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.bool;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.types.ContractParameter.map;
import static io.neow3j.types.ContractParameter.string;
import static java.util.Arrays.asList;
import static network.bane.util.TestHelper.governor;
import static network.bane.util.TestHelper.owner;
import static network.bane.util.TestHelper.relayer;
import static network.bane.util.TestHelper.securityGuard;

public class MessageBridge extends SmartContractHelper {

    public MessageBridge(Hash160 scriptHash, Neow3j neow3j) {
        super(scriptHash, neow3j);
    }

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
    // region pause/unpause

    public Hash256 pause() throws Throwable {
        return pause(securityGuard);
    }

    public Hash256 pause(Account sender) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("pause").signers(signer));
    }

    public Hash256 unpause() throws Throwable {
        return unpause(governor);
    }

    public Hash256 unpause(Account sender) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("unpause").signers(signer));
    }

    public boolean isPaused() throws IOException {
        return callFunctionReturningBool("isPaused");
    }

    public Hash256 pauseSendAndExecute() throws Throwable {
        return pauseSendAndExecute(governor);
    }

    public Hash256 pauseSendAndExecute(Account sender) throws Throwable {
        AccountSigner signer = calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("pauseSendAndExecute").signers(signer));
    }

    public Hash256 unpauseSendAndExecute() throws Throwable {
        return unpauseSendAndExecute(governor);
    }

    public Hash256 unpauseSendAndExecute(Account sender) throws Throwable {
        AccountSigner signer = calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("unpauseSendAndExecute").signers(signer));
    }

    public boolean sendAndExecuteIsPaused() throws IOException {
        return callFunctionReturningBool("sendAndExecuteIsPaused");
    }

    // endregion
    // region message bridge
    // region storing/executing messages

    public byte[] getSerializedN3MethodCall(Hash160 target, String method, CallFlags callFlags,
            List<ContractParameter> args) throws IOException {
        return callInvokeFunction("getSerializedN3MethodCall", asList(
                hash160(target),
                string(method),
                integer(callFlags.getValue()),
                array(args)
        )).getInvocationResult().getFirstStackItem().getByteArray();
    }

    public Hash256 storeMessages(String n3MessageRoot, Map<ContractParameter, ContractParameter> signatures,
            ContractParameter messages) throws Throwable {
        return storeMessages(relayer, n3MessageRoot, signatures, messages);
    }

    public Hash256 storeMessages(Account sender, String n3MessageRoot,
            Map<ContractParameter, ContractParameter> signatures, ContractParameter messages) throws Throwable {
        return sendAndAwaitExecution(
                invokeFunction("storeMessages", byteArray(n3MessageRoot), map(signatures), messages)
                        .signers(calledByEntry(sender)));
    }

    public N3MessageDto getMessage(int nonce) throws IOException {
        return getMessage(BigInteger.valueOf(nonce));
    }

    public N3MessageDto getMessage(BigInteger nonce) throws IOException {
        StackItem item = callInvokeFunction("getMessage", asList(integer(nonce))).getInvocationResult()
                .getFirstStackItem();
        return N3MessageDto.fromStackItem(item);
    }
    // 40
    // 03
    // 21 04 40a87c68
    // 28 140d165c9899c38bbf5991c5e47b04937258caec692001

    // endregion
    // region execution

    public boolean isPending(BigInteger nonce) throws IOException {
        return callFunctionReturningBool("isPending", integer(nonce));
    }

    public Hash256 executeMessage(AccountSigner signer, BigInteger nonce) throws Throwable {
        return sendAndAwaitExecution(invokeFunction("executeMessage", integer(nonce)).signers(signer));
    }

    // endregion
    // region message bridge configuration/state
    // region message bridge configuration

    public MessageBridgeDto getMessageBridge() throws IOException {
        List<StackItem> messageBridgeList = callInvokeFunction("getMessageBridge")
                .getInvocationResult().getFirstStackItem().getList();
        List<StackItem> evmToN3StateList = messageBridgeList.get(0).getList();
        State evmToN3State = new State(
                evmToN3StateList.get(0).getInteger(),
                new Hash256(evmToN3StateList.get(1).getByteArray())
        );
        List<StackItem> n3ToEvmStateList = messageBridgeList.get(1).getList();
        State n3ToEvmState = new State(
                n3ToEvmStateList.get(0).getInteger(),
                new Hash256(n3ToEvmStateList.get(1).getByteArray())
        );
        List<StackItem> messageConfigList = messageBridgeList.get(2).getList();
        MessageBridgeDto.MessageConfigDto messageConfigDto = new MessageBridgeDto.MessageConfigDto(
                messageConfigList.get(0).getInteger(),
                messageConfigList.get(1).getInteger().intValue(),
                messageConfigList.get(2).getInteger().intValue(),
                Hash160.fromAddress(messageConfigList.get(3).getAddress()),
                messageConfigList.get(4).getInteger().intValue()
        );
        return new MessageBridgeDto(evmToN3State, n3ToEvmState, messageConfigDto);
    }

    public BigInteger sendingFee() throws IOException {
        return callFunctionReturningInt("sendingFee");
    }

    public Hash256 setSendingFee(BigInteger newFee) throws Throwable {
        return setSendingFee(governor, newFee);
    }

    public Hash256 setSendingFee(Account sender, BigInteger newFee) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("setSendingFee", integer(newFee)).signers(signer));
    }

    public BigInteger maxBytesForSending() throws IOException {
        return callFunctionReturningInt("maxBytesForSending");
    }

    public Hash256 setMaxBytesForSending(BigInteger newMaxSize) throws Throwable {
        return setMaxBytesForSending(governor, newMaxSize);
    }

    public Hash256 setMaxBytesForSending(Account sender, BigInteger newMaxSize) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(
                invokeFunction("setMaxBytesForSending", integer(newMaxSize)).signers(signer));
    }

    public BigInteger maxNrMessagesForStoring() throws IOException {
        return callFunctionReturningInt("maxNrMessagesForStoring");
    }

    public Hash256 setMaxNrMessagesForStoring(BigInteger newMaxNrMessages) throws Throwable {
        return setMaxNrMessagesForStoring(governor, newMaxNrMessages);
    }

    public Hash256 setMaxNrMessagesForStoring(Account sender, BigInteger newMaxNrMessages) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(
                invokeFunction("setMaxNrMessagesForStoring", integer(newMaxNrMessages)).signers(signer));
    }

    public Hash160 executionManager() throws IOException {
        return callFunctionReturningScriptHash("executionManager");
    }

    public Hash256 setExecutionManager(Hash160 newExecutionManager) throws Throwable {
        return setExecutionManager(governor, newExecutionManager);
    }

    public Hash256 setExecutionManager(Account sender, Hash160 newExecutionManager) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("setExecutionManager", hash160(newExecutionManager))
                .signers(signer));
    }

    public BigInteger executionWindowSeconds() throws IOException {
        return callFunctionReturningInt("executionWindowSeconds");
    }

    public Hash256 setExecutionWindowSeconds(BigInteger newExecutionWindowSeconds) throws Throwable {
        return setExecutionWindowSeconds(governor, newExecutionWindowSeconds);
    }

    public Hash256 setExecutionWindowSeconds(Account sender, BigInteger newExecutionWindowSeconds) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(
                invokeFunction("setExecutionWindowSeconds", integer(newExecutionWindowSeconds)).signers(signer));
    }

    // endregion
    // region message bridge state

    public BigInteger messageEvmToN3Nonce() throws IOException {
        return getMessageBridge().evmToN3MessageState.nonce;
    }

    public String messageEvmToN3Root() throws IOException {
        return Numeric.toHexString(getMessageBridge().evmToN3MessageState.root.toArray());
    }

    public BigInteger messageN3ToEvmNonce() throws IOException {
        return getMessageBridge().n3ToEvmMessageState.nonce;
    }

    public String messageN3ToEvmRoot() throws IOException {
        return Numeric.toHexString(getMessageBridge().n3ToEvmMessageState.root.toArray());
    }

    public byte[] serializeMetadataExec(N3MessageMetadataExecDto meta) throws IOException {
        return callInvokeFunction("serializeMetadataExecutable",
                asList(integer(meta.timestamp), hash160(meta.sender), bool(meta.storeResult))
        ).getInvocationResult().getFirstStackItem().getByteArray();
    }

    public byte[] serializeMetadataStoreOnly(N3MessageMetadataStoreOnlyDto meta) throws IOException {
        return callInvokeFunction("serializeMetadataStoreOnly",
                asList(integer(meta.timestamp), hash160(meta.sender))
        ).getInvocationResult().getFirstStackItem().getByteArray();
    }

    public byte[] serializeMetadataResult(N3MessageMetadataResultDto meta) throws IOException {
        return callInvokeFunction("serializeMetadataResult",
                asList(integer(meta.timestamp), hash160(meta.sender), integer(meta.initialMessageNonce))
        ).getInvocationResult().getFirstStackItem().getByteArray();
    }

    // endregion
    // endregion
    // endregion

}
