package network.bane.client;

import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import network.bane.client.interfaces.IWriteCaller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static network.bane.client.WriteCall.newWriteCall;

class BridgeClientBase extends SmartContract {

    BridgeClientBase(Hash160 scriptHash, Neow3j neow3j) {
        super(scriptHash, neow3j);
    }

    IWriteCaller invokeWrite(String function, ContractParameter... params) {
        return newWriteCall(neow3j, invokeFunction(function, params));
    }

    StackItem invokeReadFirstStackItem(String function, ContractParameter... params) throws IOException {
        return callInvokeFunction(function, toParamsList(params)).getInvocationResult().getFirstStackItem();
    }

    private List<ContractParameter> toParamsList(ContractParameter... params) {
        ArrayList<ContractParameter> list = new ArrayList<>();
        if (params == null) {
            return list;
        }
        Collections.addAll(list, params);
        return list;
    }

}
