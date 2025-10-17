package network.bane.util;

import io.neow3j.contract.NefFile;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.ObjectMapperFactory;
import io.neow3j.protocol.core.response.ContractManifest;
import io.neow3j.protocol.core.response.InvocationResult;
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
import network.bane.util.structs.ExecutableStateDto;
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
import static io.neow3j.transaction.AccountSigner.global;
import static io.neow3j.types.ContractParameter.any;
import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.bool;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.types.ContractParameter.map;
import static io.neow3j.types.ContractParameter.string;
import static io.neow3j.utils.Numeric.hexStringToByteArray;
import static java.util.Arrays.asList;
import static network.bane.util.MessageHelper.createN3MessageHash;
import static network.bane.util.TestHelper.concatAndKeccak256;
import static network.bane.util.TestHelper.governor;
import static network.bane.util.TestHelper.owner;
import static network.bane.util.TestHelper.relayer;
import static network.bane.util.TestHelper.securityGuard;
import static network.bane.util.TestHelper.signMsg;
import static network.bane.util.TestHelper.validator1;
import static network.bane.util.TestHelper.validator2;
import static network.bane.util.TestHelper.validator3;
import static network.bane.util.TestHelper.validator4;
import static network.bane.util.TestHelper.validator5;
import static network.bane.util.helper.PrintHelper.printTransactionFee;
import static network.bane.util.helper.TestHelper.alice;
import static network.bane.util.helper.TestHelper.getNextN3Nonce;
import static network.bane.util.helper.TestHelper.messageBridge;

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

    public Hash256 pauseSending() throws Throwable {
        return pauseSending(governor);
    }

    public Hash256 pauseSending(Account sender) throws Throwable {
        AccountSigner signer = calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("pauseSending").signers(signer));
    }

    public Hash256 unpauseSending() throws Throwable {
        return unpauseSending(governor);
    }

    public Hash256 unpauseSending(Account sender) throws Throwable {
        AccountSigner signer = calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("unpauseSending").signers(signer));
    }

    public boolean sendingIsPaused() throws IOException {
        return callFunctionReturningBool("sendingIsPaused");
    }

    public Hash256 pauseExecuting() throws Throwable {
        return pauseExecuting(governor);
    }

    public Hash256 pauseExecuting(Account sender) throws Throwable {
        AccountSigner signer = calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("pauseExecuting").signers(signer));
    }

    public Hash256 unpauseExecuting() throws Throwable {
        return unpauseExecuting(governor);
    }

    public Hash256 unpauseExecuting(Account sender) throws Throwable {
        AccountSigner signer = calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("unpauseExecuting").signers(signer));
    }

    public boolean executingIsPaused() throws IOException {
        return callFunctionReturningBool("executingIsPaused");
    }

    // endregion
    // region message bridge
    // region sending messages

    public Hash256 sendMessage(AccountSigner signer, byte[] rawMessage, Hash160 feeSponsor, BigInteger maxFee)
            throws Throwable {
        return sendAndAwaitExecution(invokeFunction("sendMessage", byteArray(rawMessage), hash160(feeSponsor),
                integer(maxFee)).signers(signer));
    }

    public Hash256 sendMessage(AccountSigner signer, byte[] rawMessage) throws Throwable {
        return sendMessage(signer, rawMessage, signer.getAccount(), sendingFee());
    }

    public Hash256 sendMessage(AccountSigner signer, byte[] rawMessage, Account feeSponsor, BigInteger maxFee)
            throws Throwable {
        TransactionBuilder b = invokeFunction("sendMessage", byteArray(rawMessage), hash160(feeSponsor),
                integer(maxFee));
        if (signer.getScriptHash().equals(feeSponsor.getScriptHash())) {
            return sendAndAwaitExecution(b.signers(signer));
        }
        return sendAndAwaitExecution(b.signers(signer, global(feeSponsor)));
    }

    public Hash256 sendMessage(AccountSigner signer, String rawMessageHex, Hash160 feeSponsor, BigInteger maxFee)
            throws Throwable {
        return sendMessage(signer, hexStringToByteArray(rawMessageHex), feeSponsor, maxFee);
    }

    public Hash256 sendExecutableMessage(AccountSigner signer, byte[] rawMessage, boolean storeResult,
            Hash160 feeSponsor, BigInteger maxFee) throws Throwable {
        return sendAndAwaitExecution(invokeFunction("sendExecutableMessage", byteArray(rawMessage),
                bool(storeResult), hash160(feeSponsor), integer(maxFee)).signers(signer));
    }

    public Hash256 sendExecutableMessage(AccountSigner signer, byte[] rawMessage, boolean storeResult,
            Account feeSponsor, BigInteger maxFee) throws Throwable {
        TransactionBuilder b = invokeFunction("sendExecutableMessage", byteArray(rawMessage),
                bool(storeResult), hash160(feeSponsor), integer(maxFee));
        if (signer.getScriptHash().equals(feeSponsor.getScriptHash())) {
            return sendAndAwaitExecution(b.signers(signer));
        }
        return sendAndAwaitExecution(b.signers(signer, global(feeSponsor)));
    }

    public Hash256 sendExecutableMessage(byte[] rawMessage, boolean storeResult) throws Throwable {
        return sendExecutableMessage(global(alice), rawMessage, storeResult, alice, sendingFee());
    }

    public Hash256 sendResultMessage(AccountSigner signer, BigInteger relatedMessageNonce, Hash160 feeSponsor,
            BigInteger maxFee) throws Throwable {
        return sendAndAwaitExecution(invokeFunction("sendResultMessage", integer(relatedMessageNonce),
                hash160(feeSponsor), integer(maxFee)).signers(signer));
    }

    // endregion
    // region call serialization/validation

    public byte[] serializeCall(Hash160 target, String method, CallFlags callFlags,
            List<ContractParameter> args) throws IOException {
        InvocationResult result = callInvokeFunction("serializeCall", asList(
                hash160(target),
                string(method),
                integer(callFlags.getValue()),
                array(args)
        )).getInvocationResult();
        return result.getFirstStackItem().getByteArray();
    }

    public boolean isValidCall(byte[] serializedCall) throws IOException {
        return callFunctionReturningBool("isValidCall", byteArray(serializedCall));
    }

    public boolean isAllowedCall(byte[] serializedCall) throws IOException {
        return callFunctionReturningBool("isAllowedCall", byteArray(serializedCall));
    }

    public String concatenateOperation(ContractParameter messageEnvelopeParam) throws IOException {
        return callInvokeFunction("concatenateOperation", asList(messageEnvelopeParam))
                .getInvocationResult().getFirstStackItem().getHexString();
    }

    // endregion
    // region storing/executing messages

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

    public BigInteger storeMessage(byte[] n3FuncCall) throws Throwable {
        Hash256 bestBlockHash = neow3j.getBestBlockHash().send().getBlockHash();
        long bestBlockTime = neow3j.getBlockHeader(bestBlockHash).send().getBlock().getTime();
        BigInteger timestamp = BigInteger.valueOf(bestBlockTime);
        Hash160 sender = alice.getScriptHash();
        return storeMessage(n3FuncCall, timestamp, sender, true);
    }

    public BigInteger storeMessage(byte[] msgBytes, BigInteger timestamp, Hash160 sender, boolean storeResult)
            throws Throwable {

        BigInteger nonce = getNextN3Nonce();

        N3MessageMetadataExecDto metadata = new N3MessageMetadataExecDto(timestamp, sender, storeResult);
        String msgHash1 = createN3MessageHash(nonce, metadata, msgBytes);
        N3MessageDto n3MessageDto = new N3MessageDto(metadata, msgBytes);

        Hash256 currentRoot = getMessageBridge().evmToN3MessageState.root;
        String root = concatAndKeccak256(currentRoot.toString(), msgHash1);
        List<Account> validators = asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter messageEnvelope = array(integer(nonce), n3MessageDto.toContractParameter(messageBridge));
        Hash256 txHash = storeMessages(root, signMsg(validators, root), array(messageEnvelope));
        printTransactionFee(neow3j, "tx with 1 message", txHash);
        return nonce;
    }

    public N3MessageDto getMessage(int nonce) throws IOException {
        return getMessage(BigInteger.valueOf(nonce));
    }

    public N3MessageDto getMessage(BigInteger nonce) throws IOException {
        StackItem item = callInvokeFunction("getMessage", asList(integer(nonce))).getInvocationResult()
                .getFirstStackItem();
        return N3MessageDto.fromStackItem(item);
    }

    // endregion
    // region execution

    public ExecutableStateDto getExecutableState(BigInteger nonce) throws IOException {
        InvocationResult result = callInvokeFunction("getExecutableState",
                asList(integer(nonce))).getInvocationResult();
        if (result.hasStateFault()) {
            throw new IllegalStateException("Failed to get execution state: " + result.getException());
        }
        List<StackItem> items = result.getFirstStackItem().getList();
        boolean executed = items.get(0).getBoolean();
        BigInteger expirationTime = items.get(1).getInteger();
        return new ExecutableStateDto(executed, expirationTime);
    }

    public Hash256 executeMessage(AccountSigner signer, BigInteger nonce) throws Throwable {
        return sendAndAwaitExecution(invokeFunction("executeMessage", integer(nonce)).signers(signer));
    }

    public byte[] getResult(BigInteger nonce) throws IOException {
        StackItem item = callInvokeFunction("getResult", asList(integer(nonce))).getInvocationResult()
                .getFirstStackItem();
        if (item.getValue() == null) {
            return new byte[0];
        }
        return item.getByteArray();
    }

    public BigInteger getEvmExecutionResultNonce(BigInteger relatedNonce) throws IOException {
        return callFunctionReturningInt("getEvmExecutionResultNonce", integer(relatedNonce));
    }

    public byte[] getEvmExecutionResult(BigInteger relatedNonce) throws IOException {
        StackItem item = callInvokeFunction("getEvmExecutionResult", asList(integer(relatedNonce))).getInvocationResult()
                .getFirstStackItem();
        if (item.getValue() == null) {
            return new byte[0];
        }
        return item.getByteArray();
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

    public BigInteger unclaimedFees() throws IOException {
        return callFunctionReturningInt("unclaimedFees");
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

    public BigInteger maxMessageSize() throws IOException {
        return callFunctionReturningInt("maxMessageSize");
    }

    public Hash256 setMaxMessageSize(BigInteger newMaxSize) throws Throwable {
        return setMaxMessageSize(governor, newMaxSize);
    }

    public Hash256 setMaxMessageSize(Account sender, BigInteger newMaxSize) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(
                invokeFunction("setMaxMessageSize", integer(newMaxSize)).signers(signer));
    }

    public BigInteger maxNrMessages() throws IOException {
        return callFunctionReturningInt("maxNrMessages");
    }

    public Hash256 setMaxNrMessages(BigInteger newMaxNrMessages) throws Throwable {
        return setMaxNrMessages(governor, newMaxNrMessages);
    }

    public Hash256 setMaxNrMessages(Account sender, BigInteger newMaxNrMessages) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(
                invokeFunction("setMaxNrMessages", integer(newMaxNrMessages)).signers(signer));
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

    public BigInteger executionWindowMilliseconds() throws IOException {
        return callFunctionReturningInt("executionWindowMilliseconds");
    }

    public Hash256 setExecutionWindowMilliseconds(BigInteger newExecutionWindowMillis) throws Throwable {
        return setExecutionWindowMilliseconds(governor, newExecutionWindowMillis);
    }

    public Hash256 setExecutionWindowMilliseconds(Account sender, BigInteger newExecutionWindowMillis)
            throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(
                invokeFunction("setExecutionWindowMilliseconds", integer(newExecutionWindowMillis)).signers(signer));
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
                asList(integer(meta.timestamp), hash160(meta.sender), integer(meta.relatedMessageNonce))
        ).getInvocationResult().getFirstStackItem().getByteArray();
    }

    // endregion
    // endregion
    // endregion

}
