package network.bane.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.neow3j.contract.Iterator;
import io.neow3j.contract.NefFile;
import io.neow3j.contract.SmartContract;
import io.neow3j.crypto.ECKeyPair;
import io.neow3j.crypto.Sign;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.ContractManifest;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import network.bane.client.interfaces.IBridgeGeneralOps;
import network.bane.client.interfaces.IBridgeNativeOps;
import network.bane.client.interfaces.IBridgeTokenOps;
import network.bane.client.interfaces.IWriteCaller;
import network.bane.dto.bridge.NativeBridge;
import network.bane.dto.bridge.TokenBridge;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * Facade client for bridge contract interactions used by ops and test code.
 * <p>
 * This class composes general, native, and token operation modules while exposing one unified API.
 * Method names intentionally follow on-chain entry points in {@code BridgeContract}.
 */
public class BridgeClient extends SmartContract implements IBridgeGeneralOps, IBridgeNativeOps, IBridgeTokenOps {

    private final BridgeGeneralModule general;
    private final BridgeNativeModule nativeBridge;
    private final BridgeTokenModule token;

    public BridgeClient(Hash160 scriptHash, Neow3j neow3j) {
        super(scriptHash, neow3j);
        BridgeClientBase base = new BridgeClientBase(scriptHash, neow3j);
        this.general = new BridgeGeneralModule(base);
        this.nativeBridge = new BridgeNativeModule(base);
        this.token = new BridgeTokenModule(base);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller update(NefFile nefFile, ContractManifest manifest, Object data) throws JsonProcessingException {
        return general.update(nefFile, manifest, data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller pauseBridge() {
        return general.pauseBridge();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller unpauseBridge() {
        return general.unpauseBridge();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPaused() throws IOException {
        return general.isPaused();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller pauseDeposits() {
        return general.pauseDeposits();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller unpauseDeposits() {
        return general.unpauseDeposits();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean depositsArePaused() throws IOException {
        return general.depositsArePaused();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger linkedChainId() throws IOException {
        return general.linkedChainId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Hash160 management() throws IOException {
        return general.management();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger unclaimedRewards() throws IOException {
        return general.unclaimedRewards();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger neoHoldingGasRewards() throws IOException {
        return general.neoHoldingGasRewards();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller setNativeBridge(Hash160 tokenForNativeBridge, int decimalsOnLinkedChain, BigInteger depositFee,
            BigInteger minAmount, BigInteger maxAmount, int maxWithdrawals, BigInteger maxTotalDeposited) {
        return nativeBridge.setNativeBridge(tokenForNativeBridge, decimalsOnLinkedChain, depositFee, minAmount,
                maxAmount, maxWithdrawals, maxTotalDeposited);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller pauseNativeBridge() {
        return nativeBridge.pauseNativeBridge();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller unpauseNativeBridge() {
        return nativeBridge.unpauseNativeBridge();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller depositNative(Hash160 from, Hash160 to, BigInteger amount, BigInteger maxFee) {
        return nativeBridge.depositNative(from, to, amount, maxFee);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller depositNative(Hash160 from, Hash160 to, BigInteger amount, BigInteger maxFee,
            Hash160 feeSponsor) {
        return nativeBridge.depositNative(from, to, amount, maxFee, feeSponsor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller withdrawNative(String withdrawalRoot, Map<ECKeyPair.ECPublicKey, Sign.SignatureData> signatures,
            ContractParameter withdrawals) {
        return nativeBridge.withdrawNative(withdrawalRoot, signatures, withdrawals);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller claimNative(BigInteger nonce) {
        return nativeBridge.claimNative(nonce);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClaimableNative(BigInteger nonce) throws IOException {
        return nativeBridge.isClaimableNative(nonce);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean nativeBridgeIsSet() throws IOException {
        return nativeBridge.nativeBridgeIsSet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Hash160 nativeToken() throws IOException {
        return nativeBridge.nativeToken();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NativeBridge getNativeBridge() throws IOException {
        return nativeBridge.getNativeBridge();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger nativeDepositFee() throws IOException {
        return nativeBridge.nativeDepositFee();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller setNativeDepositFee(BigInteger newFee) {
        return nativeBridge.setNativeDepositFee(newFee);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger minNativeDeposit() throws IOException {
        return nativeBridge.minNativeDeposit();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller setMinNativeDeposit(BigInteger newMinAmount) {
        return nativeBridge.setMinNativeDeposit(newMinAmount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger maxNativeDeposit() throws IOException {
        return nativeBridge.maxNativeDeposit();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller setMaxNativeDeposit(BigInteger newMaxAmount) {
        return nativeBridge.setMaxNativeDeposit(newMaxAmount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger maxTotalDepositedNative() throws IOException {
        return nativeBridge.maxTotalDepositedNative();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller setMaxTotalDepositedNative(BigInteger newMaxTotalDeposited) {
        return nativeBridge.setMaxTotalDepositedNative(newMaxTotalDeposited);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger nativeDepositNonce() throws IOException {
        return nativeBridge.nativeDepositNonce();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String nativeDepositRoot() throws IOException {
        return nativeBridge.nativeDepositRoot();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger nativeWithdrawalNonce() throws IOException {
        return nativeBridge.nativeWithdrawalNonce();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String nativeWithdrawalRoot() throws IOException {
        return nativeBridge.nativeWithdrawalRoot();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRegisteredToken(Hash160 token) throws IOException {
        return this.token.isRegisteredToken(token);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TokenBridge getTokenBridge(Hash160 token) throws IOException {
        return this.token.getTokenBridge(token);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Hash160> getRegisteredTokensIterator() throws IOException {
        return this.token.getRegisteredTokensIterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Hash160> getRegisteredTokens() throws IOException {
        return this.token.getRegisteredTokens();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller registerToken(Hash160 token, TokenBridge.TokenConfig tokenConfig) {
        return this.token.registerToken(token, tokenConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller pauseTokenBridge(Hash160 neoN3Token) {
        return this.token.pauseTokenBridge(neoN3Token);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller unpauseTokenBridge(Hash160 neoN3Token) {
        return this.token.unpauseTokenBridge(neoN3Token);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller depositToken(Hash160 token, Hash160 from, Hash160 to, BigInteger amount, BigInteger maxFee) {
        return this.token.depositToken(token, from, to, amount, maxFee);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller depositToken(Hash160 token, Hash160 from, Hash160 to, BigInteger amount, BigInteger maxFee,
            Hash160 feeSponsor) {
        return this.token.depositToken(token, from, to, amount, maxFee, feeSponsor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller withdrawToken(Hash160 token, String withdrawalRoot,
            Map<ECKeyPair.ECPublicKey, Sign.SignatureData> signatures, ContractParameter withdrawals) {
        return this.token.withdrawToken(token, withdrawalRoot, signatures, withdrawals);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller claimToken(Hash160 token, BigInteger nonce) {
        return this.token.claimToken(token, nonce);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClaimableToken(Hash160 token, BigInteger nonce) throws IOException {
        return this.token.isClaimableToken(token, nonce);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger tokenDepositFee(Hash160 token) throws IOException {
        return this.token.tokenDepositFee(token);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller setTokenDepositFee(Map<Hash160, BigInteger> newDepositFees) {
        return this.token.setTokenDepositFee(newDepositFees);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger minTokenDeposit(Hash160 token) throws IOException {
        return this.token.minTokenDeposit(token);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller setMinTokenDeposit(Map<Hash160, BigInteger> newMinDeposits) {
        return this.token.setMinTokenDeposit(newMinDeposits);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger maxTokenDeposit(Hash160 token) throws IOException {
        return this.token.maxTokenDeposit(token);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller setMaxTokenDeposit(Map<Hash160, BigInteger> newMaxDeposits) {
        return this.token.setMaxTokenDeposit(newMaxDeposits);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger maxTokenWithdrawals(Hash160 token) throws IOException {
        return this.token.maxTokenWithdrawals(token);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller setMaxTokenWithdrawals(Map<Hash160, Integer> newMaxWithdrawals) {
        return this.token.setMaxTokenWithdrawals(newMaxWithdrawals);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger tokenDepositNonce(Hash160 token) throws IOException {
        return this.token.tokenDepositNonce(token);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String tokenDepositRoot(Hash160 token) throws IOException {
        return this.token.tokenDepositRoot(token);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger tokenWithdrawalNonce(Hash160 token) throws IOException {
        return this.token.tokenWithdrawalNonce(token);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String tokenWithdrawalRoot(Hash160 token) throws IOException {
        return this.token.tokenWithdrawalRoot(token);
    }
}
