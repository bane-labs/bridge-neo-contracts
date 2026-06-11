package network.bane.utils.bridge;

import io.neow3j.contract.GasToken;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;
import network.bane.utils.structs.NativeBridge;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;

import static io.neow3j.transaction.AccountSigner.none;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.types.ContractParameter.map;

class BridgeNativeModule implements BridgeNativeOps {

    private final BridgeClientBase base;

    BridgeNativeModule(BridgeClientBase base) {
        this.base = base;
    }

    @Override
    public Hash256 setNativeBridge(Account sender, Hash160 tokenForNativeBridge, int decimalsOnLinkedChain,
            BigInteger depositFee, BigInteger minAmount, BigInteger maxAmount, int maxWithdrawals,
            BigInteger maxTotalDeposited) throws Throwable {
        return base.invokeWrite(sender, "setNativeBridge",
                hash160(tokenForNativeBridge),
                integer(decimalsOnLinkedChain),
                integer(depositFee),
                integer(minAmount),
                integer(maxAmount),
                integer(maxWithdrawals),
                integer(maxTotalDeposited));
    }

    @Override
    public Hash256 pauseNativeBridge(Account sender) throws Throwable {
        return base.invokeWrite(sender, "pauseNativeBridge");
    }

    @Override
    public Hash256 unpauseNativeBridge(Account sender) throws Throwable {
        return base.invokeWrite(sender, "unpauseNativeBridge");
    }

    @Override
    public Hash256 depositNative(Account sender, Hash160 from, Hash160 to, BigInteger amount, BigInteger maxFee)
            throws Throwable {
        return base.invokeWriteWithSigners("depositNative",
                new ContractParameter[]{
                        hash160(from),
                        hash160(to),
                        integer(amount),
                        integer(maxFee)
                },
                none(sender).setAllowedContracts(GasToken.SCRIPT_HASH));
    }

    @Override
    public Hash256 depositNative(Account sender, Hash160 from, Hash160 to, BigInteger amount, BigInteger maxFee,
            Hash160 feeSponsor) throws Throwable {
        return base.invokeWriteWithSigners("depositNative",
                new ContractParameter[]{
                        hash160(from),
                        hash160(to),
                        integer(amount),
                        integer(maxFee),
                        hash160(feeSponsor)
                },
                none(sender).setAllowedContracts(GasToken.SCRIPT_HASH));
    }

    @Override
    public Hash256 withdrawNative(Account sender, String withdrawalRoot,
            Map<ContractParameter, ContractParameter> signatures,
            ContractParameter withdrawals) throws Throwable {
        return base.invokeWrite(sender, "withdrawNative", byteArray(withdrawalRoot), map(signatures), withdrawals);
    }

    @Override
    public Hash256 claimNative(Account sender, BigInteger nonce) throws Throwable {
        return base.invokeWriteWithSigners("claimNative",
                new ContractParameter[]{integer(nonce)},
                none(sender).setAllowedContracts(GasToken.SCRIPT_HASH));
    }

    @Override
    public boolean isClaimableNative(BigInteger nonce) throws IOException {
        return base.callFunctionReturningBool("isClaimableNative", integer(nonce));
    }

    @Override
    public boolean nativeBridgeIsSet() throws IOException {
        return base.callFunctionReturningBool("nativeBridgeIsSet");
    }

    @Override
    public Hash160 nativeToken() throws IOException {
        return base.callFunctionReturningScriptHash("nativeToken");
    }

    @Override
    public NativeBridge getNativeBridge() throws IOException {
        return NativeBridge.fromStackItem(base.invokeReadFirstStackItem("getNativeBridge"));
    }

    @Override
    public BigInteger nativeDepositFee() throws IOException {
        return base.callFunctionReturningInt("nativeDepositFee");
    }

    @Override
    public Hash256 setNativeDepositFee(Account sender, BigInteger newFee) throws Throwable {
        return base.invokeWrite(sender, "setNativeDepositFee", integer(newFee));
    }

    @Override
    public BigInteger minNativeDeposit() throws IOException {
        return base.callFunctionReturningInt("minNativeDeposit");
    }

    @Override
    public Hash256 setMinNativeDeposit(Account sender, BigInteger newMinAmount) throws Throwable {
        return base.invokeWrite(sender, "setMinNativeDeposit", integer(newMinAmount));
    }

    @Override
    public BigInteger maxNativeDeposit() throws IOException {
        return base.callFunctionReturningInt("maxNativeDeposit");
    }

    @Override
    public Hash256 setMaxNativeDeposit(Account sender, BigInteger newMaxAmount) throws Throwable {
        return base.invokeWrite(sender, "setMaxNativeDeposit", integer(newMaxAmount));
    }

    @Override
    public BigInteger maxTotalDepositedNative() throws IOException {
        return base.callFunctionReturningInt("maxTotalDepositedNative");
    }

    @Override
    public Hash256 setMaxTotalDepositedNative(Account sender, BigInteger newMaxTotalDeposited) throws Throwable {
        return base.invokeWrite(sender, "setMaxTotalDepositedNative", integer(newMaxTotalDeposited));
    }

    @Override
    public BigInteger nativeDepositNonce() throws IOException {
        return base.callFunctionReturningInt("nativeDepositNonce");
    }

    @Override
    public String nativeDepositRoot() throws IOException {
        return base.callFunctionReturningString("nativeDepositRoot");
    }

    @Override
    public BigInteger nativeWithdrawalNonce() throws IOException {
        return base.callFunctionReturningInt("nativeWithdrawalNonce");
    }

    @Override
    public String nativeWithdrawalRoot() throws IOException {
        return base.callFunctionReturningString("nativeWithdrawalRoot");
    }
}
