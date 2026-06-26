package network.bane.client.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.neow3j.contract.NefFile;
import io.neow3j.crypto.ECKeyPair;
import io.neow3j.protocol.core.response.ContractManifest;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Management operations exposed by the on-chain bridge management contract.
 * <p>
 * These methods mirror entry points in {@code BridgeManagementContract}. Write methods return a prepared
 * {@link IWriteCaller}; callers configure signers and execute the transaction on the returned object.
 */
public interface IManagementOps {

    /**
     * Sets a new owner for the management contract.
     * <p>
     * Mirrors {@code BridgeManagementContract.setOwner(newOwner)}. The current owner must authorize the call, and the
     * new owner must also witness the owner change on-chain.
     *
     * @param newOwner new owner script hash.
     * @return prepared write call.
     */
    IWriteCaller setOwner(Hash160 newOwner);

    /**
     * Sets a new relayer.
     * <p>
     * Mirrors {@code BridgeManagementContract.setRelayer(newRelayer)}. Only the current owner can execute this call.
     *
     * @param newRelayer new relayer script hash.
     * @return prepared write call.
     */
    IWriteCaller setRelayer(Hash160 newRelayer);

    /**
     * Adds a validator public key.
     * <p>
     * Mirrors {@code BridgeManagementContract.addValidator(validator, incrementThreshold)}. Only the owner can execute
     * this call. If {@code incrementThreshold} is true, the validator threshold is increased after adding the validator.
     *
     * @param validator          validator public key to add.
     * @param incrementThreshold true to increment the validator threshold in the same transaction.
     * @return prepared write call.
     */
    IWriteCaller addValidator(ECKeyPair.ECPublicKey validator, boolean incrementThreshold);

    /**
     * Removes a validator public key.
     * <p>
     * Mirrors {@code BridgeManagementContract.removeValidator(validator, decrementThreshold)}. Only the owner can
     * execute this call. If {@code decrementThreshold} is true, the validator threshold is decreased before removing the
     * validator.
     *
     * @param validator          validator public key to remove.
     * @param decrementThreshold true to decrement the validator threshold in the same transaction.
     * @return prepared write call.
     */
    IWriteCaller removeValidator(ECKeyPair.ECPublicKey validator, boolean decrementThreshold);

    /**
     * Replaces an existing validator public key with a new validator public key.
     * <p>
     * Mirrors {@code BridgeManagementContract.replaceValidator(oldValidator, newValidator)}. Only the owner can execute
     * this call.
     *
     * @param oldValidator validator public key to replace.
     * @param newValidator replacement validator public key.
     * @return prepared write call.
     */
    IWriteCaller replaceValidator(ECKeyPair.ECPublicKey oldValidator, ECKeyPair.ECPublicKey newValidator);

    /**
     * Sets the validator signature threshold.
     * <p>
     * Mirrors {@code BridgeManagementContract.setValidatorThreshold(newThreshold)}. Only the owner can execute this call.
     *
     * @param newThreshold new number of validator signatures required.
     * @return prepared write call.
     */
    IWriteCaller setValidatorThreshold(int newThreshold);

    /**
     * Checks whether a public key is registered as a validator.
     *
     * @param validator validator public key to check.
     * @return true if the public key is registered as a validator.
     * @throws IOException if the RPC call fails.
     */
    boolean isValidator(ECKeyPair.ECPublicKey validator) throws IOException;

    /**
     * Sets a new governor.
     * <p>
     * Mirrors {@code BridgeManagementContract.setGovernor(newGovernor)}. Only the owner can execute this call.
     *
     * @param newGovernor new governor script hash.
     * @return prepared write call.
     */
    IWriteCaller setGovernor(Hash160 newGovernor);

    /**
     * Sets a new security guard.
     * <p>
     * Mirrors {@code BridgeManagementContract.setSecurityGuard(newSecurityGuard)}. Only the owner can execute this call.
     *
     * @param newSecurityGuard new security guard script hash.
     * @return prepared write call.
     */
    IWriteCaller setSecurityGuard(Hash160 newSecurityGuard);

    /**
     * @return current owner script hash.
     * @throws IOException if the RPC call fails.
     */
    Hash160 owner() throws IOException;

    /**
     * @return current relayer script hash.
     * @throws IOException if the RPC call fails.
     */
    Hash160 relayer() throws IOException;

    /**
     * @return registered validator public keys.
     * @throws IOException if the RPC call fails.
     */
    List<ECKeyPair.ECPublicKey> validators() throws IOException;

    /**
     * @return number of validator signatures required for validator-governed operations.
     * @throws IOException if the RPC call fails.
     */
    int validatorThreshold() throws IOException;

    /**
     * Verifies validator signatures for a root and linked-chain id.
     * <p>
     * Mirrors {@code BridgeManagementContract.verifyValidatorSignatures(linkedChainId, root, signatures)}. The contract
     * prepends the N3 network id and linked-chain id to the root before verifying signatures with the registered
     * validators and the current validator threshold.
     *
     * @param linkedChainId linked-chain id used in the signed message.
     * @param root          root hash bytes encoded as a hex string.
     * @param signatures    map of validator public-key parameters to signature parameters.
     * @return true if enough registered validators signed the derived message.
     * @throws IOException if the RPC call fails.
     */
    boolean verifyValidatorSignatures(int linkedChainId, String root,
            Map<ContractParameter, ContractParameter> signatures) throws IOException;

    /**
     * @return current governor script hash.
     * @throws IOException if the RPC call fails.
     */
    Hash160 governor() throws IOException;

    /**
     * @return current security guard script hash.
     * @throws IOException if the RPC call fails.
     */
    Hash160 securityGuard() throws IOException;

    /**
     * Updates the management contract.
     * <p>
     * Mirrors {@code BridgeManagementContract.update(nef, manifest, data)}. Only the owner can execute this call.
     *
     * @param nefFile  new contract NEF file.
     * @param manifest new contract manifest.
     * @param data     optional update payload passed to the on-chain update method.
     * @return prepared write call.
     * @throws JsonProcessingException if the manifest cannot be serialized to JSON.
     */
    IWriteCaller update(NefFile nefFile, ContractManifest manifest, Object data) throws JsonProcessingException;
}
