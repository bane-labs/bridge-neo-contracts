package network.bane.util;

import io.neow3j.crypto.ECKeyPair;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.types.Hash160;
import network.bane.util.helper.SmartContractHelper;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class Management extends SmartContractHelper {

    public Management(Hash160 scriptHash, Neow3j neow3j) {
        super(scriptHash, neow3j);
    }

    public Hash160 owner() throws IOException {
        return callFunctionReturningScriptHash("owner");
    }

    public Hash160 relayer() throws IOException {
        return callFunctionReturningScriptHash("relayer");
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

    public Hash160 governor() throws IOException {
        return callFunctionReturningScriptHash("governor");
    }

    public Hash160 securityGuard() throws IOException {
        return callFunctionReturningScriptHash("securityGuard");
    }

}
