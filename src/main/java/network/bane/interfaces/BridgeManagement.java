package network.bane.interfaces;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.contracts.ContractInterface;

public class BridgeManagement extends ContractInterface {

    public BridgeManagement(Hash160 contractHash) {
        super(contractHash);
    }

    public native Hash160 owner();
    public native Hash160 relayer();

}
