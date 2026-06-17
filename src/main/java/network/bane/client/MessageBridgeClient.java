package network.bane.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.neow3j.contract.NefFile;
import io.neow3j.contract.SmartContract;
import io.neow3j.crypto.ECKeyPair;
import io.neow3j.crypto.Sign;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.ContractManifest;
import io.neow3j.protocol.core.response.InvocationResult;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.types.CallFlags;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import network.bane.client.interfaces.IMessageBridgeOps;
import network.bane.client.interfaces.IWriteCaller;
import network.bane.dto.message.ExecutableState;
import network.bane.dto.message.MessageBridge;
import network.bane.dto.message.MessageEnvelope;
import network.bane.dto.message.N3Message;
import network.bane.dto.message.N3MessageMetadataExec;
import network.bane.dto.message.N3MessageMetadataResult;
import network.bane.dto.message.N3MessageMetadataStoreOnly;
import network.bane.dto.message.interfaces.IMetadataSerializer;
import network.bane.dto.message.interfaces.INeoDeserializer;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.neow3j.protocol.ObjectMapperFactory.getObjectMapper;
import static io.neow3j.types.ContractParameter.any;
import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.bool;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.types.ContractParameter.map;
import static io.neow3j.types.ContractParameter.string;
import static io.neow3j.utils.Numeric.prependHexPrefix;
import static java.util.Arrays.asList;

/**
 * Client facade for message bridge contract interactions.
 */
public class MessageBridgeClient extends SmartContract implements IMessageBridgeOps, IMetadataSerializer,
        INeoDeserializer {

    private final BridgeClientBase base;

    public MessageBridgeClient(Hash160 scriptHash, Neow3j neow3j) {
        super(scriptHash, neow3j);
        this.base = new BridgeClientBase(scriptHash, neow3j);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller update(NefFile nefFile, ContractManifest manifest, Object data) throws JsonProcessingException {
        byte[] nefBytes = nefFile.toArray();
        byte[] manifestBytes = getObjectMapper().writeValueAsBytes(manifest);
        return base.invokeWrite("update", byteArray(nefBytes), byteArray(manifestBytes), any(data));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String version() throws IOException {
        return base.callFunctionReturningString("version");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller pause() {
        return base.invokeWrite("pause");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller unpause() {
        return base.invokeWrite("unpause");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPaused() throws IOException {
        return base.callFunctionReturningBool("isPaused");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller pauseSending() {
        return base.invokeWrite("pauseSending");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller unpauseSending() {
        return base.invokeWrite("unpauseSending");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller pauseExecuting() {
        return base.invokeWrite("pauseExecuting");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller unpauseExecuting() {
        return base.invokeWrite("unpauseExecuting");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean sendingIsPaused() throws IOException {
        return base.callFunctionReturningBool("sendingIsPaused");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean executingIsPaused() throws IOException {
        return base.callFunctionReturningBool("executingIsPaused");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger linkedChainId() throws IOException {
        return base.callFunctionReturningInt("linkedChainId");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Hash160 management() throws IOException {
        return base.callFunctionReturningScriptHash("management");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger unclaimedFees() throws IOException {
        return base.callFunctionReturningInt("unclaimedFees");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller sendExecutableMessage(byte[] rawMessage, boolean storeResult, Hash160 feeSponsor,
            BigInteger maxFee) {
        return base.invokeWrite("sendExecutableMessage", byteArray(rawMessage), bool(storeResult), hash160(feeSponsor),
                integer(maxFee));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller sendStoreOnlyMessage(byte[] rawMessage, Hash160 feeSponsor, BigInteger maxFee) {
        return base.invokeWrite("sendStoreOnlyMessage", byteArray(rawMessage), hash160(feeSponsor), integer(maxFee));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller sendResultMessage(BigInteger relatedMessageNonce, Hash160 feeSponsor, BigInteger maxFee) {
        return base.invokeWrite("sendResultMessage", integer(relatedMessageNonce), hash160(feeSponsor),
                integer(maxFee));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] serializeCall(Hash160 target, String method, CallFlags callFlags, List<ContractParameter> args)
            throws IOException {
        return base.invokeReadFirstStackItem("serializeCall", hash160(target), string(method),
                integer(callFlags.getValue()), array(args)).getByteArray();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValidCall(byte[] serializedCall) throws IOException {
        return base.callFunctionReturningBool("isValidCall", byteArray(serializedCall));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAllowedCall(byte[] serializedCall) throws IOException {
        return base.callFunctionReturningBool("isAllowedCall", byteArray(serializedCall));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String concatenateOperation(MessageEnvelope messageEnvelope) throws IOException {
        return prependHexPrefix(base.invokeReadFirstStackItem("concatenateOperation",
                array(
                        integer(messageEnvelope.getNonce()),
                        array(
                                messageEnvelope.getMessage().metadata.serializeToContractParameter(this),
                                byteArray(messageEnvelope.getMessage().messageBytes)
                        )
                )
        ).getHexString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller storeMessages(String evmToNeoRoot, Map<ECKeyPair.ECPublicKey, Sign.SignatureData> signatures,
            List<MessageEnvelope> messages) throws IOException {
        ArrayList<ContractParameter> msgEnvelopeParams = new ArrayList<>();
        for (MessageEnvelope message : messages) {
            msgEnvelopeParams.add(
                    array(
                            integer(message.getNonce()),
                            array(
                                    message.getMessage().metadata.serializeToContractParameter(this),
                                    byteArray(message.getMessage().messageBytes)
                            )
                    )
            );
        }
        return base.invokeWrite("storeMessages", byteArray(evmToNeoRoot), map(signatures), array(msgEnvelopeParams));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public N3Message getMessage(BigInteger nonce) throws IOException {
        return N3Message.fromStackItem(this, base.invokeReadFirstStackItem("getMessage", integer(nonce)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StackItem deserialize(byte[] serializedMessage) throws IOException {
        return base.invokeReadFirstStackItem("deserialize", byteArray(serializedMessage));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StackItem getMetadata(BigInteger nonce) throws IOException {
        return base.invokeReadFirstStackItem("getMetadata", integer(nonce));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] serializeMetadataExecutable(N3MessageMetadataExec metadata) throws IOException {
        return base.invokeReadFirstStackItem("serializeMetadataExecutable", integer(metadata.timestamp),
                hash160(metadata.sender), bool(metadata.storeResult)).getByteArray();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] serializeMetadataStoreOnly(N3MessageMetadataStoreOnly metadata) throws IOException {
        return base.invokeReadFirstStackItem("serializeMetadataStoreOnly", integer(metadata.timestamp),
                hash160(metadata.sender)).getByteArray();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] serializeMetadataResult(N3MessageMetadataResult metadata) throws IOException {
        return base.invokeReadFirstStackItem("serializeMetadataResult", integer(metadata.timestamp),
                hash160(metadata.sender), integer(metadata.relatedMessageNonce)).getByteArray();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExecutableState getExecutableState(BigInteger nonce) throws IOException {
        InvocationResult result = base.callInvokeFunction("getExecutableState", asList(integer(nonce)))
                .getInvocationResult();
        if (result.hasStateFault()) {
            throw new IllegalStateException("Failed to get executable state: " + result.getException());
        }
        List<StackItem> items = result.getFirstStackItem().getList();
        return new ExecutableState(items.get(0).getBoolean(), items.get(1).getInteger());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller executeMessage(BigInteger nonce) {
        return base.invokeWrite("executeMessage", integer(nonce));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StackItem getNeoExecutionResult(BigInteger relatedEvmToNeoMessageNonce) throws IOException {
        return base.invokeReadFirstStackItem("getNeoExecutionResult", integer(relatedEvmToNeoMessageNonce));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getSerializedNeoExecutionResult(BigInteger relatedEvmToNeoMessageNonce) throws IOException {
        StackItem item = base.invokeReadFirstStackItem("getSerializedNeoExecutionResult",
                integer(relatedEvmToNeoMessageNonce));
        return item.getValue() == null ? new byte[0] : item.getByteArray();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger getEvmExecutionResultNonce(BigInteger relatedNeoToEvmMessageNonce) throws IOException {
        return base.callFunctionReturningInt("getEvmExecutionResultNonce", integer(relatedNeoToEvmMessageNonce));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getEvmExecutionResult(BigInteger relatedNeoToEvmMessageNonce) throws IOException {
        StackItem item = base.invokeReadFirstStackItem("getEvmExecutionResult", integer(relatedNeoToEvmMessageNonce));
        return item.getValue() == null ? new byte[0] : item.getByteArray();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageBridge getMessageBridge() throws IOException {
        return MessageBridge.fromStackItem(base.invokeReadFirstStackItem("getMessageBridge"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger sendingFee() throws IOException {
        return base.callFunctionReturningInt("sendingFee");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller setSendingFee(BigInteger newFee) {
        return base.invokeWrite("setSendingFee", integer(newFee));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger maxMessageSize() throws IOException {
        return base.callFunctionReturningInt("maxMessageSize");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller setMaxMessageSize(BigInteger newMaxBytes) {
        return base.invokeWrite("setMaxMessageSize", integer(newMaxBytes));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger maxNrMessages() throws IOException {
        return base.callFunctionReturningInt("maxNrMessages");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller setMaxNrMessages(BigInteger newMaxNrMessages) {
        return base.invokeWrite("setMaxNrMessages", integer(newMaxNrMessages));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Hash160 executionManager() throws IOException {
        return base.callFunctionReturningScriptHash("executionManager");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller setExecutionManager(Hash160 newExecutionManager) {
        return base.invokeWrite("setExecutionManager", hash160(newExecutionManager));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger executionWindowMilliseconds() throws IOException {
        return base.callFunctionReturningInt("executionWindowMilliseconds");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller setExecutionWindowMilliseconds(BigInteger newExecutionWindowMilliseconds) {
        return base.invokeWrite("setExecutionWindowMilliseconds", integer(newExecutionWindowMilliseconds));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger neoToEvmNonce() throws IOException {
        return base.callFunctionReturningInt("neoToEvmNonce");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String neoToEvmRoot() throws IOException {
        return prependHexPrefix(base.invokeReadFirstStackItem("neoToEvmRoot").getHexString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger evmToNeoNonce() throws IOException {
        return base.callFunctionReturningInt("evmToNeoNonce");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String evmToNeoRoot() throws IOException {
        return prependHexPrefix(base.invokeReadFirstStackItem("evmToNeoRoot").getHexString());
    }
}
