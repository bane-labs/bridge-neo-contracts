package network.bane.client;

import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import network.bane.client.interfaces.IBridgeNativeOps;
import network.bane.client.interfaces.IWriteCaller;
import network.bane.dto.bridge.NativeBridge;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;

import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.types.ContractParameter.map;
import static io.neow3j.utils.Numeric.prependHexPrefix;

class BridgeNativeModule implements IBridgeNativeOps {

    private final BridgeClientBase base;

    BridgeNativeModule(BridgeClientBase base) {
        this.base = base;
    }

    @Override
    public IWriteCaller setNativeBridge(Hash160 tokenForNativeBridge, int decimalsOnLinkedChain, BigInteger depositFee,
            BigInteger minAmount, BigInteger maxAmount, int maxWithdrawals, BigInteger maxTotalDeposited) {
        return base.invokeWrite("setNativeBridge",
                hash160(tokenForNativeBridge),
                integer(decimalsOnLinkedChain),
                integer(depositFee),
                integer(minAmount),
                integer(maxAmount),
                integer(maxWithdrawals),
                integer(maxTotalDeposited)
        );
    }

    @Override
    public IWriteCaller pauseNativeBridge() {
        return base.invokeWrite("pauseNativeBridge");
    }

    @Override
    public IWriteCaller unpauseNativeBridge() {
        return base.invokeWrite("unpauseNativeBridge");
    }

    @Override
    public IWriteCaller depositNative(Hash160 from, Hash160 to, BigInteger amount, BigInteger maxFee) {
        return base.invokeWrite("depositNative",
                hash160(from),
                hash160(to),
                integer(amount),
                integer(maxFee)
        );
    }

    @Override
    public IWriteCaller depositNative(Hash160 from, Hash160 to, BigInteger amount, BigInteger maxFee,
            Hash160 feeSponsor) {
        return base.invokeWrite("depositNative",
                hash160(from),
                hash160(to),
                integer(amount),
                integer(maxFee),
                hash160(feeSponsor)
        );
    }

    @Override
    public IWriteCaller withdrawNative(String withdrawalRoot, Map<ContractParameter, ContractParameter> signatures,
            ContractParameter withdrawals) {
        return base.invokeWrite("withdrawNative", byteArray(withdrawalRoot), map(signatures), withdrawals);
    }

    @Override
    public IWriteCaller claimNative(BigInteger nonce) {
        return base.invokeWrite("claimNative", integer(nonce));
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
    public IWriteCaller setNativeDepositFee(BigInteger newFee) {
        return base.invokeWrite("setNativeDepositFee", integer(newFee));
    }

    @Override
    public BigInteger minNativeDeposit() throws IOException {
        return base.callFunctionReturningInt("minNativeDeposit");
    }

    @Override
    public IWriteCaller setMinNativeDeposit(BigInteger newMinAmount) {
        return base.invokeWrite("setMinNativeDeposit", integer(newMinAmount));
    }

    @Override
    public BigInteger maxNativeDeposit() throws IOException {
        return base.callFunctionReturningInt("maxNativeDeposit");
    }

    @Override
    public IWriteCaller setMaxNativeDeposit(BigInteger newMaxAmount) {
        return base.invokeWrite("setMaxNativeDeposit", integer(newMaxAmount));
    }

    @Override
    public BigInteger maxTotalDepositedNative() throws IOException {
        return base.callFunctionReturningInt("maxTotalDepositedNative");
    }

    @Override
    public IWriteCaller setMaxTotalDepositedNative(BigInteger newMaxTotalDeposited) {
        return base.invokeWrite("setMaxTotalDepositedNative", integer(newMaxTotalDeposited));
    }

    @Override
    public BigInteger nativeDepositNonce() throws IOException {
        return base.callFunctionReturningInt("nativeDepositNonce");
    }

    @Override
    public String nativeDepositRoot() throws IOException {
        return prependHexPrefix(
                base.callInvokeFunction("nativeDepositRoot").getInvocationResult().getFirstStackItem().getHexString()
        );
    }

    @Override
    public BigInteger nativeWithdrawalNonce() throws IOException {
        return base.callFunctionReturningInt("nativeWithdrawalNonce");
    }

    @Override
    public String nativeWithdrawalRoot() throws IOException {
        return prependHexPrefix(
                base.callInvokeFunction("nativeWithdrawalRoot").getInvocationResult().getFirstStackItem().getHexString()
        );
    }
}
