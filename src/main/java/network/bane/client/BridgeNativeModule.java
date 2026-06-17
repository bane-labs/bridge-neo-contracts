package network.bane.client;

import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import network.bane.client.interfaces.WriteCaller;
import network.bane.dto.bridge.NativeBridge;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;

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
    public WriteCaller setNativeBridge(Hash160 tokenForNativeBridge, int decimalsOnLinkedChain, BigInteger depositFee,
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
    public WriteCaller pauseNativeBridge() {
        return base.invokeWrite( "pauseNativeBridge");
    }

    @Override
    public WriteCaller unpauseNativeBridge() {
        return base.invokeWrite("unpauseNativeBridge");
    }

    @Override
    public WriteCaller depositNative(Hash160 from, Hash160 to, BigInteger amount, BigInteger maxFee) {
        return base.invokeWrite("depositNative",
                hash160(from),
                hash160(to),
                integer(amount),
                integer(maxFee)
        );
    }

    @Override
    public WriteCaller depositNative(Hash160 from, Hash160 to, BigInteger amount, BigInteger maxFee,
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
    public WriteCaller withdrawNative(String withdrawalRoot, Map<ContractParameter, ContractParameter> signatures,
            ContractParameter withdrawals) {
        return base.invokeWrite("withdrawNative", byteArray(withdrawalRoot), map(signatures), withdrawals);
    }

    @Override
    public WriteCaller claimNative(BigInteger nonce) {
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
    public WriteCaller setNativeDepositFee(BigInteger newFee) {
        return base.invokeWrite( "setNativeDepositFee", integer(newFee));
    }

    @Override
    public BigInteger minNativeDeposit() throws IOException {
        return base.callFunctionReturningInt("minNativeDeposit");
    }

    @Override
    public WriteCaller setMinNativeDeposit( BigInteger newMinAmount) {
        return base.invokeWrite("setMinNativeDeposit", integer(newMinAmount));
    }

    @Override
    public BigInteger maxNativeDeposit() throws IOException {
        return base.callFunctionReturningInt("maxNativeDeposit");
    }

    @Override
    public WriteCaller setMaxNativeDeposit(BigInteger newMaxAmount) {
        return base.invokeWrite( "setMaxNativeDeposit", integer(newMaxAmount));
    }

    @Override
    public BigInteger maxTotalDepositedNative() throws IOException {
        return base.callFunctionReturningInt("maxTotalDepositedNative");
    }

    @Override
    public WriteCaller setMaxTotalDepositedNative( BigInteger newMaxTotalDeposited) {
        return base.invokeWrite( "setMaxTotalDepositedNative", integer(newMaxTotalDeposited));
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
