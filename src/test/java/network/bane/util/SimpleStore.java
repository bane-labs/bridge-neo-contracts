package network.bane.util;

import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.types.Hash160;
import network.bane.util.helper.SmartContractHelper;

import java.io.IOException;
import java.math.BigInteger;

import static io.neow3j.types.ContractParameter.integer;
import static java.util.Arrays.asList;

public class SimpleStore extends SmartContractHelper {

    public SimpleStore(Hash160 scriptHash, Neow3j neow3j) {
        super(scriptHash, neow3j);
    }

    public StackItem get(BigInteger key) throws IOException {
        return callInvokeFunction("get", asList(integer(key))).getInvocationResult().getFirstStackItem();
    }

}
