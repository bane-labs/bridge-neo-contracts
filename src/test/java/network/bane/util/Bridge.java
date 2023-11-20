package network.bane.util;

import io.neow3j.protocol.Neow3j;
import io.neow3j.types.Hash160;

import java.io.IOException;
import java.math.BigInteger;

import static io.neow3j.utils.Numeric.prependHexPrefix;

public class Bridge extends SmartContractHelper {

    public Bridge(Hash160 scriptHash, Neow3j neow3j) {
        super(scriptHash, neow3j);
    }

    // region static values

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

    public BigInteger maxWithdrawalPerRoot() throws IOException {
        return callFunctionReturningInt("maxWithdrawalPerRoot");
    }

    // endregion
    // region dynamic values

    public String depositRoot() throws IOException {
        return prependHexPrefix(callInvokeFunction("depositRoot").getInvocationResult().getFirstStackItem().getHexString());
    }

    public String withdrawRoot() throws IOException {
        return prependHexPrefix(callInvokeFunction("withdrawalRoot").getInvocationResult().getFirstStackItem().getHexString());
    }

    public BigInteger depositsProcessed() throws IOException {
        return callFunctionReturningInt("depositsProcessed");
    }

    public BigInteger withdrawalsProcessed() throws IOException {
        return callFunctionReturningInt("withdrawalsProcessed");
    }

    // endregion

}
