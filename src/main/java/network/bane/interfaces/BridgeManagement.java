package network.bane.interfaces;

import io.neow3j.devpack.ECPoint;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.List;
import io.neow3j.devpack.contracts.ContractInterface;

public class BridgeManagement extends ContractInterface {

    public BridgeManagement(Hash160 contractHash) {
        super(contractHash);
    }

    public native ECPoint owner();
    public native ECPoint relayer();
    public native List<ECPoint> validators();

    public native int validatorThreshold();

}
