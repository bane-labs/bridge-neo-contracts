package network.bane.util;

import io.neow3j.contract.GasToken;
import io.neow3j.protocol.Neow3j;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.Signer;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;

import java.io.IOException;
import java.math.BigInteger;

import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.utils.Numeric.prependHexPrefix;
import static network.bane.util.TestHelper.governor;
import static network.bane.util.TestHelper.securityGuard;

public class Bridge extends SmartContractHelper {

    public Bridge(Hash160 scriptHash, Neow3j neow3j) {
        super(scriptHash, neow3j);
    }

    // region methods

    public Hash256 depositGas(Account sender, Hash160 from, Hash160 to, BigInteger amount) throws Throwable {
        Signer signer = AccountSigner.none(sender).setAllowedContracts(GasToken.SCRIPT_HASH);
        return sendAndAwaitExecution(invokeFunction("depositGas", hash160(from), hash160(to), integer(amount))
                .signers(signer));
    }

    public Hash256 depositGas(Account from, Hash160 to, BigInteger amount) throws Throwable {
        return depositGas(from, from.getScriptHash(), to, amount);
    }

    public Hash256 claimGas(Account sender, BigInteger nonce) throws Throwable {
        Signer signer = AccountSigner.none(sender).setAllowedContracts(GasToken.SCRIPT_HASH);
        return sendAndAwaitExecution(invokeFunction("claimGas", integer(nonce)).signers(signer));
    }

    public Hash256 pause() throws Throwable {
        return pause(securityGuard);
    }

    public Hash256 pause(Account sender) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("pause").signers(signer));
    }

    public Hash256 unpause() throws Throwable {
        return unpause(governor);
    }

    public Hash256 unpause(Account sender) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("unpause").signers(signer));
    }

    // region static values

    public Hash160 management() throws IOException {
        return callFunctionReturningScriptHash("management");
    }

    public BigInteger gasDepositFee() throws IOException {
        return callFunctionReturningInt("gasDepositFee");
    }

    public BigInteger minGasDeposit() throws IOException {
        return callFunctionReturningInt("minGasDeposit");
    }

    public BigInteger maxGasDeposit() throws IOException {
        return callFunctionReturningInt("maxGasDeposit");
    }

    public boolean isPaused() throws IOException {
        return callFunctionReturningBool("isPaused");
    }

    // endregion
    // region dynamic values

    public String gasDepositRoot() throws IOException {
        return prependHexPrefix(callInvokeFunction("gasDepositRoot").getInvocationResult().getFirstStackItem().getHexString());
    }

    public BigInteger gasDepositNonce() throws IOException {
        return callFunctionReturningInt("gasDepositNonce");
    }

    public String gasWithdrawRoot() throws IOException {
        return prependHexPrefix(callInvokeFunction("gasWithdrawalRoot").getInvocationResult().getFirstStackItem().getHexString());
    }

    public BigInteger gasWithdrawalNonce() throws IOException {
        return callFunctionReturningInt("gasWithdrawalNonce");
    }

    // endregion

}
