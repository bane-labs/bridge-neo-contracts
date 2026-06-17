package network.bane.client;

import io.neow3j.contract.Iterator;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import network.bane.client.interfaces.IBridgeTokenOps;
import network.bane.client.interfaces.IWriteCaller;
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
import static io.neow3j.utils.Numeric.prependHexPrefix;
import static java.util.Arrays.asList;

class BridgeTokenModule implements IBridgeTokenOps {

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
    public Iterator<Hash160> getRegisteredTokensIterator() throws IOException {
        return base.callFunctionReturningIterator(s -> Hash160.fromAddress(s.getAddress()),
                "getRegisteredTokensIterator");
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
    public IWriteCaller registerToken(Hash160 token, TokenBridge.TokenConfig tokenConfig) {
        return base.invokeWrite("registerToken", hash160(token), tokenConfigAsParameter(tokenConfig));
    }

    @Override
    public IWriteCaller pauseTokenBridge(Hash160 neoN3Token) {
        return base.invokeWrite("pauseTokenBridge", hash160(neoN3Token));
    }

    @Override
    public IWriteCaller unpauseTokenBridge(Hash160 neoN3Token) {
        return base.invokeWrite("unpauseTokenBridge", hash160(neoN3Token));
    }

    @Override
    public IWriteCaller depositToken(Hash160 token, Hash160 from, Hash160 to, BigInteger amount, BigInteger maxFee) {
        return base.invokeWrite("depositToken",
                hash160(token),
                hash160(from),
                hash160(to),
                integer(amount),
                integer(maxFee)
        );
    }

    @Override
    public IWriteCaller depositToken(Hash160 token, Hash160 from, Hash160 to, BigInteger amount, BigInteger maxFee,
            Hash160 feeSponsor) {
        return base.invokeWrite("depositToken",
                hash160(token),
                hash160(from),
                hash160(to),
                integer(amount),
                integer(maxFee),
                hash160(feeSponsor)
        );
    }

    @Override
    public IWriteCaller withdrawToken(Hash160 token, String withdrawalRoot,
            Map<ContractParameter, ContractParameter> signatures, ContractParameter withdrawals) {
        return base.invokeWrite("withdrawToken", hash160(token), byteArray(withdrawalRoot), map(signatures),
                withdrawals);
    }

    @Override
    public IWriteCaller claimToken(Hash160 token, BigInteger nonce) {
        return base.invokeWrite("claimToken", hash160(token), integer(nonce));
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
    public IWriteCaller setTokenDepositFee(Map<Hash160, BigInteger> newDepositFees) {
        return base.invokeWrite("setTokenDepositFee", map(newDepositFees));
    }

    @Override
    public BigInteger minTokenDeposit(Hash160 token) throws IOException {
        return base.callFunctionReturningInt("minTokenDeposit", hash160(token));
    }

    @Override
    public IWriteCaller setMinTokenDeposit(Map<Hash160, BigInteger> newMinDeposits) {
        return base.invokeWrite("setMinTokenDeposit", map(newMinDeposits));
    }

    @Override
    public BigInteger maxTokenDeposit(Hash160 token) throws IOException {
        return base.callFunctionReturningInt("maxTokenDeposit", hash160(token));
    }

    @Override
    public IWriteCaller setMaxTokenDeposit(Map<Hash160, BigInteger> newMaxDeposits) {
        return base.invokeWrite("setMaxTokenDeposit", map(newMaxDeposits));
    }

    @Override
    public BigInteger maxTokenWithdrawals(Hash160 token) throws IOException {
        return base.callFunctionReturningInt("maxTokenWithdrawals", hash160(token));
    }

    @Override
    public IWriteCaller setMaxTokenWithdrawals(Map<Hash160, Integer> newMaxWithdrawals) {
        return base.invokeWrite("setMaxTokenWithdrawals", map(newMaxWithdrawals));
    }

    @Override
    public BigInteger tokenDepositNonce(Hash160 token) throws IOException {
        return base.callFunctionReturningInt("tokenDepositNonce", hash160(token));
    }

    @Override
    public String tokenDepositRoot(Hash160 token) throws IOException {
        return prependHexPrefix(
                base.callInvokeFunction("tokenDepositRoot", asList(hash160(token))).getInvocationResult()
                        .getFirstStackItem().getHexString()
        );
    }

    @Override
    public BigInteger tokenWithdrawalNonce(Hash160 token) throws IOException {
        return base.callFunctionReturningInt("tokenWithdrawalNonce", hash160(token));
    }

    @Override
    public String tokenWithdrawalRoot(Hash160 token) throws IOException {
        return prependHexPrefix(
                base.callInvokeFunction("tokenWithdrawalRoot", asList(hash160(token))).getInvocationResult()
                        .getFirstStackItem().getHexString()
        );
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
