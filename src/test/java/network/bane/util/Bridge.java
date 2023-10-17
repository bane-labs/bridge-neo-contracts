package network.bane.util;

import io.neow3j.protocol.Neow3j;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;

import java.io.IOException;
import java.math.BigInteger;

public class Bridge extends SmartContractHelper {

    public Bridge(Hash160 scriptHash, Neow3j neow3j) {
        super(scriptHash, neow3j);
    }

    public Hash256 depositRoot() throws IOException {
        return new Hash256(callInvokeFunction("depositRoot").getInvocationResult().getFirstStackItem().getByteArray());
    }

    public Hash160 management() throws IOException {
        return callFunctionReturningScriptHash("management");
    }

    public BigInteger depositPrice() throws IOException {
        return callFunctionReturningInt("depositPrice");
    }

    public BigInteger minDeposit() throws IOException {
        return callFunctionReturningInt("minDeposit");
    }

    public BigInteger maxDeposit() throws IOException {
        return callFunctionReturningInt("maxDeposit");
    }

    public BigInteger depositsProcessed() throws IOException {
        return callFunctionReturningInt("depositsProcessed");
    }

    public BigInteger withdrawalsProcessed() throws IOException {
        return callFunctionReturningInt("withdrawalsProcessed");
    }

}
