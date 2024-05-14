package network.bane.interfaces;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.ECPoint;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.List;
import io.neow3j.devpack.Map;
import io.neow3j.devpack.annotations.CallFlags;
import io.neow3j.devpack.contracts.ContractInterface;

public class BridgeManagement extends ContractInterface {

    public BridgeManagement(Hash160 contractHash) {
        super(contractHash);
    }

    @CallFlags(io.neow3j.devpack.constants.CallFlags.ReadStates)
    public native ECPoint owner();

    @CallFlags(io.neow3j.devpack.constants.CallFlags.ReadStates)
    public native ECPoint relayer();

    @CallFlags(io.neow3j.devpack.constants.CallFlags.ReadStates)
    public native int validatorThreshold();

    @CallFlags(io.neow3j.devpack.constants.CallFlags.ReadStates)
    public native List<ECPoint> validators();

    @CallFlags(io.neow3j.devpack.constants.CallFlags.ReadOnly)
    public native boolean verifyValidatorSignatures(Map<ECPoint, ByteString> signatures, ByteString root);

    @CallFlags(io.neow3j.devpack.constants.CallFlags.ReadStates)
    public native ECPoint governor();

    @CallFlags(io.neow3j.devpack.constants.CallFlags.ReadStates)
    public native ECPoint securityGuard();
}
