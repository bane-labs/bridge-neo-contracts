package network.bane.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.neow3j.contract.NefFile;
import io.neow3j.contract.SmartContract;
import io.neow3j.crypto.ECKeyPair;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.ContractManifest;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import network.bane.client.interfaces.IManagementOps;
import network.bane.client.interfaces.IWriteCaller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.neow3j.protocol.ObjectMapperFactory.getObjectMapper;
import static io.neow3j.types.ContractParameter.any;
import static io.neow3j.types.ContractParameter.bool;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.types.ContractParameter.map;
import static io.neow3j.types.ContractParameter.publicKey;

/**
 * Client facade for bridge management contract interactions.
 */
public class ManagementClient extends SmartContract implements IManagementOps {

    private final BridgeClientBase base;

    public ManagementClient(Hash160 scriptHash, Neow3j neow3j) {
        super(scriptHash, neow3j);
        this.base = new BridgeClientBase(scriptHash, neow3j);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller setOwner(Hash160 newOwner) {
        return base.invokeWrite("setOwner", hash160(newOwner));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller setRelayer(Hash160 newRelayer) {
        return base.invokeWrite("setRelayer", hash160(newRelayer));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller addValidator(ECKeyPair.ECPublicKey validator, boolean incrementThreshold) {
        return base.invokeWrite("addValidator", publicKey(validator), bool(incrementThreshold));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller removeValidator(ECKeyPair.ECPublicKey validator, boolean decrementThreshold) {
        return base.invokeWrite("removeValidator", publicKey(validator), bool(decrementThreshold));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller replaceValidator(ECKeyPair.ECPublicKey oldValidator, ECKeyPair.ECPublicKey newValidator) {
        return base.invokeWrite("replaceValidator", publicKey(oldValidator), publicKey(newValidator));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller setValidatorThreshold(int newThreshold) {
        return base.invokeWrite("setValidatorThreshold", integer(newThreshold));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValidator(ECKeyPair.ECPublicKey validator) throws IOException {
        return base.callFunctionReturningBool("isValidator", publicKey(validator));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller setGovernor(Hash160 newGovernor) {
        return base.invokeWrite("setGovernor", hash160(newGovernor));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller setSecurityGuard(Hash160 newSecurityGuard) {
        return base.invokeWrite("setSecurityGuard", hash160(newSecurityGuard));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Hash160 owner() throws IOException {
        return base.callFunctionReturningScriptHash("owner");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Hash160 relayer() throws IOException {
        return base.callFunctionReturningScriptHash("relayer");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ECKeyPair.ECPublicKey> validators() throws IOException {
        List<StackItem> stackItems = base.invokeReadFirstStackItem("validators").getList();
        ArrayList<ECKeyPair.ECPublicKey> validators = new ArrayList<>();
        for (StackItem item : stackItems) {
            validators.add(new ECKeyPair.ECPublicKey(item.getByteArray()));
        }
        return validators;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int validatorThreshold() throws IOException {
        return base.callFunctionReturningInt("validatorThreshold").intValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean verifyValidatorSignatures(int linkedChainId, String root,
            Map<ContractParameter, ContractParameter> signatures) throws IOException {
        return base.callFunctionReturningBool("verifyValidatorSignatures", integer(linkedChainId), byteArray(root),
                map(signatures));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Hash160 governor() throws IOException {
        return base.callFunctionReturningScriptHash("governor");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Hash160 securityGuard() throws IOException {
        return base.callFunctionReturningScriptHash("securityGuard");
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
}
