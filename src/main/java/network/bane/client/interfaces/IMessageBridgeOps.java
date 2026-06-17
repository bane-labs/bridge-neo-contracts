package network.bane.client.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.neow3j.contract.NefFile;
import io.neow3j.crypto.ECKeyPair;
import io.neow3j.crypto.Sign;
import io.neow3j.protocol.core.response.ContractManifest;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.types.CallFlags;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import network.bane.dto.message.ExecutableState;
import network.bane.dto.message.MessageBridge;
import network.bane.dto.message.MessageEnvelope;
import network.bane.dto.message.N3Message;
import network.bane.dto.message.N3MessageMetadataExec;
import network.bane.dto.message.N3MessageMetadataResult;
import network.bane.dto.message.N3MessageMetadataStoreOnly;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * Message bridge operations exposed by the on-chain message bridge contract.
 * <p>
 * These methods mirror entry points in {@code MessageBridgeContract}. Write methods return a prepared
 * {@link IWriteCaller}; callers configure signers and execute the transaction on the returned object.
 */
public interface IMessageBridgeOps {

    /**
     * Updates the message bridge contract.
     * <p>
     * Mirrors {@code MessageBridgeContract.update(nef, manifest, data)}. The contract must be paused and the owner of
     * the management contract must witness the update.
     *
     * @param nefFile  new contract NEF file.
     * @param manifest new contract manifest.
     * @param data     optional update payload passed to the on-chain update method.
     * @return prepared write call.
     * @throws JsonProcessingException if the manifest cannot be serialized to JSON.
     */
    IWriteCaller update(NefFile nefFile, ContractManifest manifest, Object data) throws JsonProcessingException;

    /**
     * @return message bridge contract version.
     * @throws IOException if the RPC call fails.
     */
    String version() throws IOException;

    /**
     * Pauses all message bridge sending and execution.
     *
     * @return prepared write call.
     */
    IWriteCaller pause();

    /**
     * Unpauses all message bridge sending and execution.
     *
     * @return prepared write call.
     */
    IWriteCaller unpause();

    /**
     * @return true if the message bridge is fully paused.
     * @throws IOException if the RPC call fails.
     */
    boolean isPaused() throws IOException;

    /**
     * Pauses N3-to-linked-chain message sending.
     *
     * @return prepared write call.
     */
    IWriteCaller pauseSending();

    /**
     * Unpauses N3-to-linked-chain message sending.
     *
     * @return prepared write call.
     */
    IWriteCaller unpauseSending();

    /**
     * Pauses linked-chain-to-N3 message execution.
     *
     * @return prepared write call.
     */
    IWriteCaller pauseExecuting();

    /**
     * Unpauses linked-chain-to-N3 message execution.
     *
     * @return prepared write call.
     */
    IWriteCaller unpauseExecuting();

    /**
     * @return true if message sending is paused.
     * @throws IOException if the RPC call fails.
     */
    boolean sendingIsPaused() throws IOException;

    /**
     * @return true if message execution is paused.
     * @throws IOException if the RPC call fails.
     */
    boolean executingIsPaused() throws IOException;

    /**
     * @return linked chain id configured for this message bridge.
     * @throws IOException if the RPC call fails.
     */
    BigInteger linkedChainId() throws IOException;

    /**
     * @return management contract script hash.
     * @throws IOException if the RPC call fails.
     */
    Hash160 management() throws IOException;

    /**
     * @return accumulated unclaimed message sending fees.
     * @throws IOException if the RPC call fails.
     */
    BigInteger unclaimedFees() throws IOException;

    /**
     * Sends an executable message from N3 to the linked chain.
     *
     * @param rawMessage  raw linked-chain message bytes.
     * @param storeResult true if the execution result should be stored.
     * @param feeSponsor  account script hash paying the sending fee.
     * @param maxFee      maximum fee accepted by the sender.
     * @return prepared write call.
     */
    IWriteCaller sendExecutableMessage(byte[] rawMessage, boolean storeResult, Hash160 feeSponsor, BigInteger maxFee);

    /**
     * Sends a store-only message from N3 to the linked chain.
     *
     * @param rawMessage raw linked-chain message bytes.
     * @param feeSponsor account script hash paying the sending fee.
     * @param maxFee     maximum fee accepted by the sender.
     * @return prepared write call.
     */
    IWriteCaller sendStoreOnlyMessage(byte[] rawMessage, Hash160 feeSponsor, BigInteger maxFee);

    /**
     * Sends an execution result message back to the linked chain.
     *
     * @param relatedMessageNonce nonce of the related linked-chain-to-N3 message.
     * @param feeSponsor          account script hash paying the sending fee.
     * @param maxFee              maximum fee accepted by the sender.
     * @return prepared write call.
     */
    IWriteCaller sendResultMessage(BigInteger relatedMessageNonce, Hash160 feeSponsor, BigInteger maxFee);

    /**
     * Serializes an execution-manager call.
     *
     * @param target    target contract script hash.
     * @param method    target method.
     * @param callFlags call flags used for the target invocation.
     * @param args      target method arguments.
     * @return serialized call bytes.
     * @throws IOException if the RPC call fails.
     */
    byte[] serializeCall(Hash160 target, String method, CallFlags callFlags, List<ContractParameter> args)
            throws IOException;

    /**
     * @return true if the serialized execution-manager call has a valid shape.
     * @throws IOException if the RPC call fails.
     */
    boolean isValidCall(byte[] serializedCall) throws IOException;

    /**
     * @return true if the serialized execution-manager call is allowed by the execution manager.
     * @throws IOException if the RPC call fails.
     */
    boolean isAllowedCall(byte[] serializedCall) throws IOException;

    /**
     * Concatenates a message operation exactly as the contract does for root computation.
     *
     * @param messageEnvelope serialized message envelope contract parameter.
     * @return operation bytes encoded as a hex string with {@code 0x} prefix.
     * @throws IOException if the RPC call fails.
     */
    String concatenateOperation(MessageEnvelope messageEnvelope) throws IOException;

    /**
     * Stores linked-chain-to-N3 messages proven by validator signatures.
     *
     * @param evmToNeoRoot new linked-chain-to-N3 root.
     * @param signatures   validator signatures over the root.
     * @param messages     serialized list parameter of message envelopes.
     * @return prepared write call.
     */
    IWriteCaller storeMessages(String evmToNeoRoot, Map<ECKeyPair.ECPublicKey, Sign.SignatureData> signatures,
            List<MessageEnvelope> messages) throws IOException;

    /**
     * Gets a stored linked-chain-to-N3 message by nonce.
     *
     * @param nonce message nonce.
     * @return message DTO.
     * @throws IOException if the RPC call fails.
     */
    N3Message getMessage(BigInteger nonce) throws IOException;

    /**
     * Deserializes bytes through the contract's StdLib helper.
     *
     * @param serializedMessage serialized bytes.
     * @return deserialized stack item.
     * @throws IOException if the RPC call fails.
     */
    StackItem deserialize(byte[] serializedMessage) throws IOException;

    /**
     * Gets stored metadata for a message.
     *
     * @param nonce message nonce.
     * @return metadata stack item.
     * @throws IOException if the RPC call fails.
     */
    StackItem getMetadata(BigInteger nonce) throws IOException;

    /**
     * Serializes executable-message metadata.
     *
     * @param metadata the metadata to serialize.
     * @return serialized metadata bytes.
     * @throws IOException if the RPC call fails.
     */
    byte[] serializeMetadataExecutable(N3MessageMetadataExec metadata) throws IOException;

    /**
     * Serializes store-only-message metadata.
     *
     * @param metadata the metadata to serialize.
     * @return serialized metadata bytes.
     * @throws IOException if the RPC call fails.
     */
    byte[] serializeMetadataStoreOnly(N3MessageMetadataStoreOnly metadata) throws IOException;

    /**
     * Serializes result-message metadata.
     *
     * @param metadata the metadata to serialize.
     * @return serialized metadata bytes.
     * @throws IOException if the RPC call fails.
     */
    byte[] serializeMetadataResult(N3MessageMetadataResult metadata) throws IOException;

    /**
     * Gets executable state for a stored executable message.
     *
     * @param nonce message nonce.
     * @return executable state DTO.
     * @throws IOException if the RPC call fails.
     */
    ExecutableState getExecutableState(BigInteger nonce) throws IOException;

    /**
     * Executes a stored linked-chain-to-N3 message.
     *
     * @param nonce message nonce.
     * @return prepared write call.
     */
    IWriteCaller executeMessage(BigInteger nonce);

    /**
     * Gets the deserialized N3 execution result for an executable linked-chain-to-N3 message.
     *
     * @param relatedEvmToNeoMessageNonce related message nonce.
     * @return execution result stack item.
     * @throws IOException if the RPC call fails.
     */
    StackItem getNeoExecutionResult(BigInteger relatedEvmToNeoMessageNonce) throws IOException;

    /**
     * Gets the serialized N3 execution result for an executable linked-chain-to-N3 message.
     *
     * @param relatedEvmToNeoMessageNonce related message nonce.
     * @return serialized execution result bytes, or an empty array if no result exists.
     * @throws IOException if the RPC call fails.
     */
    byte[] getSerializedNeoExecutionResult(BigInteger relatedEvmToNeoMessageNonce) throws IOException;

    /**
     * Gets the linked-chain result message nonce corresponding to a N3-to-linked-chain executable message.
     *
     * @param relatedNeoToEvmMessageNonce related N3-to-linked-chain message nonce.
     * @return linked-chain result message nonce, or zero if none exists.
     * @throws IOException if the RPC call fails.
     */
    BigInteger getEvmExecutionResultNonce(BigInteger relatedNeoToEvmMessageNonce) throws IOException;

    /**
     * Gets the serialized EVM execution result bytes.
     *
     * @param relatedNeoToEvmMessageNonce related N3-to-linked-chain message nonce.
     * @return serialized execution result bytes, or an empty array if no result exists.
     * @throws IOException if the RPC call fails.
     */
    byte[] getEvmExecutionResult(BigInteger relatedNeoToEvmMessageNonce) throws IOException;

    /**
     * @return full message bridge state DTO.
     * @throws IOException if the RPC call fails.
     */
    MessageBridge getMessageBridge() throws IOException;

    /**
     * @return current sending fee.
     * @throws IOException if the RPC call fails.
     */
    BigInteger sendingFee() throws IOException;

    /**
     * Updates the sending fee.
     *
     * @param newFee new sending fee.
     * @return prepared write call.
     */
    IWriteCaller setSendingFee(BigInteger newFee);

    /**
     * @return maximum accepted message size in bytes.
     * @throws IOException if the RPC call fails.
     */
    BigInteger maxMessageSize() throws IOException;

    /**
     * Updates the maximum accepted message size.
     *
     * @param newMaxBytes new maximum message size in bytes.
     * @return prepared write call.
     */
    IWriteCaller setMaxMessageSize(BigInteger newMaxBytes);

    /**
     * @return maximum number of messages accepted in one store operation.
     * @throws IOException if the RPC call fails.
     */
    BigInteger maxNrMessages() throws IOException;

    /**
     * Updates the maximum number of messages accepted in one store operation.
     *
     * @param newMaxNrMessages new maximum number of messages.
     * @return prepared write call.
     */
    IWriteCaller setMaxNrMessages(BigInteger newMaxNrMessages);

    /**
     * @return execution manager script hash.
     * @throws IOException if the RPC call fails.
     */
    Hash160 executionManager() throws IOException;

    /**
     * Updates the execution manager script hash.
     *
     * @param newExecutionManager new execution manager script hash.
     * @return prepared write call.
     */
    IWriteCaller setExecutionManager(Hash160 newExecutionManager);

    /**
     * @return execution window in milliseconds.
     * @throws IOException if the RPC call fails.
     */
    BigInteger executionWindowMilliseconds() throws IOException;

    /**
     * Updates the execution window in milliseconds.
     *
     * @param newExecutionWindowMilliseconds new execution window in milliseconds.
     * @return prepared write call.
     */
    IWriteCaller setExecutionWindowMilliseconds(BigInteger newExecutionWindowMilliseconds);

    /**
     * @return current N3-to-linked-chain message nonce.
     * @throws IOException if the RPC call fails.
     */
    BigInteger neoToEvmNonce() throws IOException;

    /**
     * @return current N3-to-linked-chain root as a hex string with {@code 0x} prefix.
     * @throws IOException if the RPC call fails.
     */
    String neoToEvmRoot() throws IOException;

    /**
     * @return current linked-chain-to-N3 message nonce.
     * @throws IOException if the RPC call fails.
     */
    BigInteger evmToNeoNonce() throws IOException;

    /**
     * @return current linked-chain-to-N3 root as a hex string with {@code 0x} prefix.
     * @throws IOException if the RPC call fails.
     */
    String evmToNeoRoot() throws IOException;
}
