package network.bane.util;

import io.neow3j.crypto.ECKeyPair;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.types.Hash160;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class Management extends SmartContractHelper {

    public Management(Hash160 scriptHash, Neow3j neow3j) {
        super(scriptHash, neow3j);
    }

    public ECKeyPair.ECPublicKey owner() throws IOException {
        return new ECKeyPair.ECPublicKey(callInvokeFunction("owner").getInvocationResult().getFirstStackItem().getByteArray());
    }

    public ECKeyPair.ECPublicKey relayer() throws IOException {
        return new ECKeyPair.ECPublicKey(callInvokeFunction("relayer").getInvocationResult().getFirstStackItem().getByteArray());
    }

    public List<ECKeyPair.ECPublicKey> validators() throws IOException {
        return callInvokeFunction("validators")
                .getInvocationResult()
                .getFirstStackItem()
                .getList()
                .stream()
                .map(StackItem::getByteArray)
                .map(ECKeyPair.ECPublicKey::new)
                .collect(Collectors.toList());
    }

    public int validatorThreshold() throws IOException {
        return callInvokeFunction("validatorThreshold").getInvocationResult().getFirstStackItem().getInteger().intValue();
    }

}
