package network.bane.util;

import io.neow3j.contract.GasToken;
import io.neow3j.protocol.Neow3j;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.Signer;
import io.neow3j.transaction.TransactionBuilder;
import io.neow3j.types.Hash160;
import io.neow3j.wallet.Account;

import java.io.IOException;
import java.math.BigInteger;

import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.utils.Numeric.prependHexPrefix;

public class Bridge extends SmartContractHelper {

    public Bridge(Hash160 scriptHash, Neow3j neow3j) {
        super(scriptHash, neow3j);
    }

    // region methods

    public TransactionBuilder deposit(Account from, Hash160 to, BigInteger amount) {
        Signer signer = AccountSigner.none(from).setAllowedContracts(GasToken.SCRIPT_HASH);
        return invokeFunction("deposit", hash160(from), hash160(to), integer(amount))
                .signers(signer);
    }

    // region static values

    public Hash160 management() throws IOException {
        return callFunctionReturningScriptHash("management");
    }

    public BigInteger depositFee() throws IOException {
        return callFunctionReturningInt("depositFee");
    }

    public BigInteger minDeposit() throws IOException {
        return callFunctionReturningInt("minDeposit");
    }

    public BigInteger maxDeposit() throws IOException {
        return callFunctionReturningInt("maxDeposit");
    }

    public boolean isLocked() throws IOException {
        return callFunctionReturningBool("isLocked");
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
