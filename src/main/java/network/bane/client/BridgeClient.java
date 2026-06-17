package network.bane.client;

import io.neow3j.contract.Iterator;
import io.neow3j.protocol.Neow3j;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import network.bane.client.interfaces.WriteCaller;
import network.bane.dto.bridge.NativeBridge;
import network.bane.dto.bridge.TokenBridge;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * Facade client for bridge contract interactions used by deploy scripts.
 * <p>
 * This class composes general, native, and token operation modules while exposing one unified API.
 * Method names intentionally follow on-chain entry points in {@code BridgeContract}.
 */
public class BridgeClient implements BridgeGeneralOps, BridgeNativeOps, BridgeTokenOps {

    private final BridgeGeneralModule general;
    private final BridgeNativeModule nativeBridge;
    private final BridgeTokenModule token;

    public BridgeClient(Hash160 scriptHash, Neow3j neow3j) {
        BridgeClientBase base = new BridgeClientBase(scriptHash, neow3j);
        this.general = new BridgeGeneralModule(base);
        this.nativeBridge = new BridgeNativeModule(base);
        this.token = new BridgeTokenModule(base);
    }

    @Override
    public WriteCaller update(byte[] nefBytes, String manifestJson, Object data) {
        return general.update(nefBytes, manifestJson, data);
    }

    @Override
    public WriteCaller pauseBridge() {
        return general.pauseBridge();
    }

    @Override
    public WriteCaller unpauseBridge() {
        return general.unpauseBridge();
    }

    @Override
    public boolean isPaused() throws IOException {
        return general.isPaused();
    }

    @Override
    public WriteCaller pauseDeposits() {
        return general.pauseDeposits();
    }

    @Override
    public WriteCaller unpauseDeposits() {
        return general.unpauseDeposits();
    }

    @Override
    public boolean depositsArePaused() throws IOException {
        return general.depositsArePaused();
    }

    @Override
    public BigInteger linkedChainId() throws IOException {
        return general.linkedChainId();
    }

    @Override
    public Hash160 management() throws IOException {
        return general.management();
    }

    @Override
    public BigInteger unclaimedRewards() throws IOException {
        return general.unclaimedRewards();
    }

    @Override
    public BigInteger neoHoldingGasRewards() throws IOException {
        return general.neoHoldingGasRewards();
    }

    @Override
    public WriteCaller setNativeBridge(Hash160 tokenForNativeBridge, int decimalsOnLinkedChain, BigInteger depositFee,
            BigInteger minAmount, BigInteger maxAmount, int maxWithdrawals, BigInteger maxTotalDeposited) {
        return nativeBridge.setNativeBridge(tokenForNativeBridge, decimalsOnLinkedChain, depositFee, minAmount,
                maxAmount, maxWithdrawals, maxTotalDeposited);
    }

    @Override
    public WriteCaller pauseNativeBridge() {
        return nativeBridge.pauseNativeBridge();
    }

    @Override
    public WriteCaller unpauseNativeBridge() {
        return nativeBridge.unpauseNativeBridge();
    }

    @Override
    public WriteCaller depositNative(Hash160 from, Hash160 to, BigInteger amount, BigInteger maxFee) {
        return nativeBridge.depositNative(from, to, amount, maxFee);
    }

    @Override
    public WriteCaller depositNative(Hash160 from, Hash160 to, BigInteger amount, BigInteger maxFee,
            Hash160 feeSponsor) {
        return nativeBridge.depositNative(from, to, amount, maxFee, feeSponsor);
    }

    @Override
    public WriteCaller withdrawNative(String withdrawalRoot, Map<ContractParameter, ContractParameter> signatures,
            ContractParameter withdrawals) {
        return nativeBridge.withdrawNative(withdrawalRoot, signatures, withdrawals);
    }

    @Override
    public WriteCaller claimNative(BigInteger nonce) {
        return nativeBridge.claimNative(nonce);
    }

    @Override
    public boolean isClaimableNative(BigInteger nonce) throws IOException {
        return nativeBridge.isClaimableNative(nonce);
    }

    @Override
    public boolean nativeBridgeIsSet() throws IOException {
        return nativeBridge.nativeBridgeIsSet();
    }

    @Override
    public Hash160 nativeToken() throws IOException {
        return nativeBridge.nativeToken();
    }

    @Override
    public NativeBridge getNativeBridge() throws IOException {
        return nativeBridge.getNativeBridge();
    }

    @Override
    public BigInteger nativeDepositFee() throws IOException {
        return nativeBridge.nativeDepositFee();
    }

    @Override
    public WriteCaller setNativeDepositFee(BigInteger newFee) {
        return nativeBridge.setNativeDepositFee(newFee);
    }

    @Override
    public BigInteger minNativeDeposit() throws IOException {
        return nativeBridge.minNativeDeposit();
    }

    @Override
    public WriteCaller setMinNativeDeposit(BigInteger newMinAmount) {
        return nativeBridge.setMinNativeDeposit(newMinAmount);
    }

    @Override
    public BigInteger maxNativeDeposit() throws IOException {
        return nativeBridge.maxNativeDeposit();
    }

    @Override
    public WriteCaller setMaxNativeDeposit(BigInteger newMaxAmount) {
        return nativeBridge.setMaxNativeDeposit(newMaxAmount);
    }

    @Override
    public BigInteger maxTotalDepositedNative() throws IOException {
        return nativeBridge.maxTotalDepositedNative();
    }

    @Override
    public WriteCaller setMaxTotalDepositedNative(BigInteger newMaxTotalDeposited) {
        return nativeBridge.setMaxTotalDepositedNative(newMaxTotalDeposited);
    }

    @Override
    public BigInteger nativeDepositNonce() throws IOException {
        return nativeBridge.nativeDepositNonce();
    }

    @Override
    public String nativeDepositRoot() throws IOException {
        return nativeBridge.nativeDepositRoot();
    }

    @Override
    public BigInteger nativeWithdrawalNonce() throws IOException {
        return nativeBridge.nativeWithdrawalNonce();
    }

    @Override
    public String nativeWithdrawalRoot() throws IOException {
        return nativeBridge.nativeWithdrawalRoot();
    }

    @Override
    public boolean isRegisteredToken(Hash160 token) throws IOException {
        return this.token.isRegisteredToken(token);
    }

    @Override
    public TokenBridge getTokenBridge(Hash160 token) throws IOException {
        return this.token.getTokenBridge(token);
    }

    @Override
    public Iterator<Hash160> getRegisteredTokensIterator() throws IOException {
        return this.token.getRegisteredTokensIterator();
    }

    @Override
    public List<Hash160> getRegisteredTokens() throws IOException {
        return this.token.getRegisteredTokens();
    }

    @Override
    public WriteCaller registerToken(Hash160 token, TokenBridge.TokenConfig tokenConfig) {
        return this.token.registerToken(token, tokenConfig);
    }

    @Override
    public WriteCaller pauseTokenBridge(Hash160 neoN3Token) {
        return this.token.pauseTokenBridge(neoN3Token);
    }

    @Override
    public WriteCaller unpauseTokenBridge(Hash160 neoN3Token) {
        return this.token.unpauseTokenBridge(neoN3Token);
    }

    @Override
    public WriteCaller depositToken(Hash160 token, Hash160 from, Hash160 to, BigInteger amount, BigInteger maxFee) {
        return this.token.depositToken(token, from, to, amount, maxFee);
    }

    @Override
    public WriteCaller depositToken(Hash160 token, Hash160 from, Hash160 to, BigInteger amount, BigInteger maxFee,
            Hash160 feeSponsor) {
        return this.token.depositToken(token, from, to, amount, maxFee, feeSponsor);
    }

    @Override
    public WriteCaller withdrawToken(Hash160 token, String withdrawalRoot,
            Map<ContractParameter, ContractParameter> signatures, ContractParameter withdrawals) {
        return this.token.withdrawToken(token, withdrawalRoot, signatures, withdrawals);
    }

    @Override
    public WriteCaller claimToken(Hash160 token, BigInteger nonce) {
        return this.token.claimToken(token, nonce);
    }

    @Override
    public boolean isClaimableToken(Hash160 token, BigInteger nonce) throws IOException {
        return this.token.isClaimableToken(token, nonce);
    }

    @Override
    public BigInteger tokenDepositFee(Hash160 token) throws IOException {
        return this.token.tokenDepositFee(token);
    }

    @Override
    public WriteCaller setTokenDepositFee(Map<Hash160, BigInteger> newDepositFees) {
        return this.token.setTokenDepositFee(newDepositFees);
    }

    @Override
    public BigInteger minTokenDeposit(Hash160 token) throws IOException {
        return this.token.minTokenDeposit(token);
    }

    @Override
    public WriteCaller setMinTokenDeposit(Map<Hash160, BigInteger> newMinDeposits) {
        return this.token.setMinTokenDeposit(newMinDeposits);
    }

    @Override
    public BigInteger maxTokenDeposit(Hash160 token) throws IOException {
        return this.token.maxTokenDeposit(token);
    }

    @Override
    public WriteCaller setMaxTokenDeposit(Map<Hash160, BigInteger> newMaxDeposits) {
        return this.token.setMaxTokenDeposit(newMaxDeposits);
    }

    @Override
    public BigInteger maxTokenWithdrawals(Hash160 token) throws IOException {
        return this.token.maxTokenWithdrawals(token);
    }

    @Override
    public WriteCaller setMaxTokenWithdrawals(Map<Hash160, Integer> newMaxWithdrawals) {
        return this.token.setMaxTokenWithdrawals(newMaxWithdrawals);
    }

    @Override
    public BigInteger tokenDepositNonce(Hash160 token) throws IOException {
        return this.token.tokenDepositNonce(token);
    }

    @Override
    public String tokenDepositRoot(Hash160 token) throws IOException {
        return this.token.tokenDepositRoot(token);
    }

    @Override
    public BigInteger tokenWithdrawalNonce(Hash160 token) throws IOException {
        return this.token.tokenWithdrawalNonce(token);
    }

    @Override
    public String tokenWithdrawalRoot(Hash160 token) throws IOException {
        return this.token.tokenWithdrawalRoot(token);
    }
}
