package network.bane.utils.bridge;

import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;
import network.bane.dto.bridge.TokenBridge;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.types.ContractParameter.map;

class BridgeTokenModule implements BridgeTokenOps {

    private final BridgeClientBase base;

    BridgeTokenModule(BridgeClientBase base) {
        this.base = base;
    }

    @Override
    public boolean isRegisteredToken(Hash160 token) throws IOException {
        return base.callFunctionReturningBool("isRegisteredToken", hash160(token));
    }

    @Override
    public TokenBridge getTokenBridge(Hash160 token) throws IOException {
        return TokenBridge.fromStackItem(base.invokeReadFirstStackItem("getTokenBridge", hash160(token)));
    }

    @Override
    public StackItem getRegisteredTokensIterator() throws IOException {
        return base.invokeReadFirstStackItem("getRegisteredTokensIterator");
    }

    @Override
    public List<Hash160> getRegisteredTokens() throws IOException {
        List<StackItem> stackItems = base.invokeReadFirstStackItem("getRegisteredTokens").getList();
        ArrayList<Hash160> tokens = new ArrayList<>();
        for (StackItem item : stackItems) {
            tokens.add(parseHash160(item));
        }
        return tokens;
    }

    @Override
    public Hash256 registerToken(Account sender, Hash160 token, TokenBridge.TokenConfig tokenConfig) throws Throwable {
        return base.invokeWrite(sender, "registerToken", hash160(token), tokenConfigAsParameter(tokenConfig));
    }

    @Override
    public Hash256 pauseTokenBridge(Account sender, Hash160 neoN3Token) throws Throwable {
        return base.invokeWrite(sender, "pauseTokenBridge", hash160(neoN3Token));
    }

    @Override
    public Hash256 unpauseTokenBridge(Account sender, Hash160 neoN3Token) throws Throwable {
        return base.invokeWrite(sender, "unpauseTokenBridge", hash160(neoN3Token));
    }

    @Override
    public Hash256 depositToken(Account sender, Hash160 token, Hash160 from, Hash160 to, BigInteger amount,
            BigInteger maxFee) throws Throwable {
        return base.invokeWrite(sender, "depositToken",
                hash160(token),
                hash160(from),
                hash160(to),
                integer(amount),
                integer(maxFee));
    }

    @Override
    public Hash256 depositToken(Account sender, Hash160 token, Hash160 from, Hash160 to, BigInteger amount,
            BigInteger maxFee, Hash160 feeSponsor) throws Throwable {
        return base.invokeWrite(sender, "depositToken",
                hash160(token),
                hash160(from),
                hash160(to),
                integer(amount),
                integer(maxFee),
                hash160(feeSponsor));
    }

    @Override
    public Hash256 withdrawToken(Account sender, Hash160 token, String withdrawalRoot,
            Map<ContractParameter, ContractParameter> signatures,
            ContractParameter withdrawals) throws Throwable {
        return base.invokeWrite(sender, "withdrawToken", hash160(token), byteArray(withdrawalRoot), map(signatures),
                withdrawals);
    }

    @Override
    public Hash256 claimToken(Account sender, Hash160 token, BigInteger nonce) throws Throwable {
        return base.invokeWrite(sender, "claimToken", hash160(token), integer(nonce));
    }

    @Override
    public boolean isClaimableToken(Hash160 token, BigInteger nonce) throws IOException {
        return base.callFunctionReturningBool("isClaimableToken", hash160(token), integer(nonce));
    }

    @Override
    public BigInteger tokenDepositFee(Hash160 token) throws IOException {
        return base.callFunctionReturningInt("tokenDepositFee", hash160(token));
    }

    @Override
    public Hash256 setTokenDepositFee(Account sender, Map<Hash160, BigInteger> newDepositFees) throws Throwable {
        return base.invokeWrite(sender, "setTokenDepositFee", map(newDepositFees));
    }

    @Override
    public BigInteger minTokenDeposit(Hash160 token) throws IOException {
        return base.callFunctionReturningInt("minTokenDeposit", hash160(token));
    }

    @Override
    public Hash256 setMinTokenDeposit(Account sender, Map<Hash160, BigInteger> newMinDeposits) throws Throwable {
        return base.invokeWrite(sender, "setMinTokenDeposit", map(newMinDeposits));
    }

    @Override
    public BigInteger maxTokenDeposit(Hash160 token) throws IOException {
        return base.callFunctionReturningInt("maxTokenDeposit", hash160(token));
    }

    @Override
    public Hash256 setMaxTokenDeposit(Account sender, Map<Hash160, BigInteger> newMaxDeposits) throws Throwable {
        return base.invokeWrite(sender, "setMaxTokenDeposit", map(newMaxDeposits));
    }

    @Override
    public BigInteger maxTokenWithdrawals(Hash160 token) throws IOException {
        return base.callFunctionReturningInt("maxTokenWithdrawals", hash160(token));
    }

    @Override
    public Hash256 setMaxTokenWithdrawals(Account sender, Map<Hash160, Integer> newMaxWithdrawals) throws Throwable {
        return base.invokeWrite(sender, "setMaxTokenWithdrawals", map(newMaxWithdrawals));
    }

    @Override
    public BigInteger tokenDepositNonce(Hash160 token) throws IOException {
        return base.callFunctionReturningInt("tokenDepositNonce", hash160(token));
    }

    @Override
    public String tokenDepositRoot(Hash160 token) throws IOException {
        return base.callFunctionReturningString("tokenDepositRoot", hash160(token));
    }

    @Override
    public BigInteger tokenWithdrawalNonce(Hash160 token) throws IOException {
        return base.callFunctionReturningInt("tokenWithdrawalNonce", hash160(token));
    }

    @Override
    public String tokenWithdrawalRoot(Hash160 token) throws IOException {
        return base.callFunctionReturningString("tokenWithdrawalRoot", hash160(token));
    }

    private ContractParameter tokenConfigAsParameter(TokenBridge.TokenConfig config) {
        return ContractParameter.array(
                hash160(config.tokenOnDestination),
                integer(config.fee),
                integer(config.minAmount),
                integer(config.maxAmount),
                integer(config.maxWithdrawals),
                integer(config.decimalScalingFactor)
        );
    }

    private Hash160 parseHash160(StackItem item) {
        try {
            return Hash160.fromAddress(item.getAddress());
        } catch (Exception ignored) {
            return new Hash160(item.getByteArray());
        }
    }
}
