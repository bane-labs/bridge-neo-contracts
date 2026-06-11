package network.bane.utils.bridge;

import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;
import network.bane.utils.structs.NativeBridge;
import network.bane.utils.structs.TokenBridge;

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
    public Hash256 update(Account sender, byte[] nefBytes, String manifestJson, Object data) throws Throwable {
        return general.update(sender, nefBytes, manifestJson, data);
    }

    @Override
    public Hash256 pauseBridge(Account sender) throws Throwable {
        return general.pauseBridge(sender);
    }

    @Override
    public Hash256 unpauseBridge(Account sender) throws Throwable {
        return general.unpauseBridge(sender);
    }

    @Override
    public boolean isPaused() throws IOException {
        return general.isPaused();
    }

    @Override
    public Hash256 pauseDeposits(Account sender) throws Throwable {
        return general.pauseDeposits(sender);
    }

    @Override
    public Hash256 unpauseDeposits(Account sender) throws Throwable {
        return general.unpauseDeposits(sender);
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
    public Hash256 setNativeBridge(Account sender, Hash160 tokenForNativeBridge, int decimalsOnLinkedChain,
            BigInteger depositFee, BigInteger minAmount, BigInteger maxAmount, int maxWithdrawals,
            BigInteger maxTotalDeposited) throws Throwable {
        return nativeBridge.setNativeBridge(sender, tokenForNativeBridge, decimalsOnLinkedChain, depositFee, minAmount,
                maxAmount, maxWithdrawals, maxTotalDeposited);
    }

    @Override
    public Hash256 pauseNativeBridge(Account sender) throws Throwable {
        return nativeBridge.pauseNativeBridge(sender);
    }

    @Override
    public Hash256 unpauseNativeBridge(Account sender) throws Throwable {
        return nativeBridge.unpauseNativeBridge(sender);
    }

    @Override
    public Hash256 depositNative(Account sender, Hash160 from, Hash160 to, BigInteger amount, BigInteger maxFee)
            throws Throwable {
        return nativeBridge.depositNative(sender, from, to, amount, maxFee);
    }

    @Override
    public Hash256 depositNative(Account sender, Hash160 from, Hash160 to, BigInteger amount, BigInteger maxFee,
            Hash160 feeSponsor) throws Throwable {
        return nativeBridge.depositNative(sender, from, to, amount, maxFee, feeSponsor);
    }

    @Override
    public Hash256 withdrawNative(Account sender, String withdrawalRoot,
            Map<ContractParameter, ContractParameter> signatures, ContractParameter withdrawals) throws Throwable {
        return nativeBridge.withdrawNative(sender, withdrawalRoot, signatures, withdrawals);
    }

    @Override
    public Hash256 claimNative(Account sender, BigInteger nonce) throws Throwable {
        return nativeBridge.claimNative(sender, nonce);
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
    public Hash256 setNativeDepositFee(Account sender, BigInteger newFee) throws Throwable {
        return nativeBridge.setNativeDepositFee(sender, newFee);
    }

    @Override
    public BigInteger minNativeDeposit() throws IOException {
        return nativeBridge.minNativeDeposit();
    }

    @Override
    public Hash256 setMinNativeDeposit(Account sender, BigInteger newMinAmount) throws Throwable {
        return nativeBridge.setMinNativeDeposit(sender, newMinAmount);
    }

    @Override
    public BigInteger maxNativeDeposit() throws IOException {
        return nativeBridge.maxNativeDeposit();
    }

    @Override
    public Hash256 setMaxNativeDeposit(Account sender, BigInteger newMaxAmount) throws Throwable {
        return nativeBridge.setMaxNativeDeposit(sender, newMaxAmount);
    }

    @Override
    public BigInteger maxTotalDepositedNative() throws IOException {
        return nativeBridge.maxTotalDepositedNative();
    }

    @Override
    public Hash256 setMaxTotalDepositedNative(Account sender, BigInteger newMaxTotalDeposited) throws Throwable {
        return nativeBridge.setMaxTotalDepositedNative(sender, newMaxTotalDeposited);
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
    public StackItem getRegisteredTokensIterator() throws IOException {
        return this.token.getRegisteredTokensIterator();
    }

    @Override
    public List<Hash160> getRegisteredTokens() throws IOException {
        return this.token.getRegisteredTokens();
    }

    @Override
    public Hash256 registerToken(Account sender, Hash160 token, TokenBridge.TokenConfig tokenConfig) throws Throwable {
        return this.token.registerToken(sender, token, tokenConfig);
    }

    @Override
    public Hash256 pauseTokenBridge(Account sender, Hash160 neoN3Token) throws Throwable {
        return this.token.pauseTokenBridge(sender, neoN3Token);
    }

    @Override
    public Hash256 unpauseTokenBridge(Account sender, Hash160 neoN3Token) throws Throwable {
        return this.token.unpauseTokenBridge(sender, neoN3Token);
    }

    @Override
    public Hash256 depositToken(Account sender, Hash160 token, Hash160 from, Hash160 to, BigInteger amount,
            BigInteger maxFee) throws Throwable {
        return this.token.depositToken(sender, token, from, to, amount, maxFee);
    }

    @Override
    public Hash256 depositToken(Account sender, Hash160 token, Hash160 from, Hash160 to, BigInteger amount,
            BigInteger maxFee, Hash160 feeSponsor) throws Throwable {
        return this.token.depositToken(sender, token, from, to, amount, maxFee, feeSponsor);
    }

    @Override
    public Hash256 withdrawToken(Account sender, Hash160 token, String withdrawalRoot,
            Map<ContractParameter, ContractParameter> signatures, ContractParameter withdrawals) throws Throwable {
        return this.token.withdrawToken(sender, token, withdrawalRoot, signatures, withdrawals);
    }

    @Override
    public Hash256 claimToken(Account sender, Hash160 token, BigInteger nonce) throws Throwable {
        return this.token.claimToken(sender, token, nonce);
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
    public Hash256 setTokenDepositFee(Account sender, Map<Hash160, BigInteger> newDepositFees) throws Throwable {
        return this.token.setTokenDepositFee(sender, newDepositFees);
    }

    @Override
    public BigInteger minTokenDeposit(Hash160 token) throws IOException {
        return this.token.minTokenDeposit(token);
    }

    @Override
    public Hash256 setMinTokenDeposit(Account sender, Map<Hash160, BigInteger> newMinDeposits) throws Throwable {
        return this.token.setMinTokenDeposit(sender, newMinDeposits);
    }

    @Override
    public BigInteger maxTokenDeposit(Hash160 token) throws IOException {
        return this.token.maxTokenDeposit(token);
    }

    @Override
    public Hash256 setMaxTokenDeposit(Account sender, Map<Hash160, BigInteger> newMaxDeposits) throws Throwable {
        return this.token.setMaxTokenDeposit(sender, newMaxDeposits);
    }

    @Override
    public BigInteger maxTokenWithdrawals(Hash160 token) throws IOException {
        return this.token.maxTokenWithdrawals(token);
    }

    @Override
    public Hash256 setMaxTokenWithdrawals(Account sender, Map<Hash160, Integer> newMaxWithdrawals) throws Throwable {
        return this.token.setMaxTokenWithdrawals(sender, newMaxWithdrawals);
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
