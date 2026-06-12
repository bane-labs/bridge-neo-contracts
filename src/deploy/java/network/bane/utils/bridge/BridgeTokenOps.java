package network.bane.utils.bridge;

import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;
import network.bane.dto.bridge.TokenBridge;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * Token bridge operations exposed by the on-chain bridge contract.
 * <p>
 * Signer model: write methods in this interface use {@code calledByEntry(sender)}.
 * If your script needs stricter scopes (e.g., allowed-contract restrictions), construct the
 * invocation manually.
 */
public interface BridgeTokenOps {

    /**
     * @return true if token is registered on the bridge.
     */
    boolean isRegisteredToken(Hash160 token) throws IOException;

    /**
     * @return full token bridge state DTO.
     */
    TokenBridge getTokenBridge(Hash160 token) throws IOException;

    /**
     * @return iterator stack item over registered tokens.
     */
    StackItem getRegisteredTokensIterator() throws IOException;

    /**
     * @return registered token script hashes.
     */
    List<Hash160> getRegisteredTokens() throws IOException;

    /**
     * Registers a token bridge.
     *
     * @param sender      account used as {@code calledByEntry(sender)} signer.
     * @param token       N3 token script hash.
     * @param tokenConfig destination token and bridge config.
     * @return transaction hash.
     */
    Hash256 registerToken(Account sender, Hash160 token, TokenBridge.TokenConfig tokenConfig) throws Throwable;

    /**
     * Pauses a token bridge.
     *
     * @param sender     account used as {@code calledByEntry(sender)} signer.
     * @param neoN3Token token bridge to pause.
     * @return transaction hash.
     */
    Hash256 pauseTokenBridge(Account sender, Hash160 neoN3Token) throws Throwable;

    /**
     * Unpauses a token bridge.
     *
     * @param sender     account used as {@code calledByEntry(sender)} signer.
     * @param neoN3Token token bridge to unpause.
     * @return transaction hash.
     */
    Hash256 unpauseTokenBridge(Account sender, Hash160 neoN3Token) throws Throwable;

    /**
     * Deposits a token to be bridged to the linked chain.
     * Mirrors {@code BridgeContract.depositToken(token, from, to, amount, maxFee)}.
     *
     * @param sender account used as {@code calledByEntry(sender)} signer.
     * @param token  N3 token script hash.
     * @param from   source account script hash on N3.
     * @param to     recipient address on linked chain encoded as {@code Hash160}.
     * @param amount token amount to bridge.
     * @param maxFee max GAS fee accepted for this deposit.
     * @return transaction hash.
     */
    Hash256 depositToken(Account sender, Hash160 token, Hash160 from, Hash160 to, BigInteger amount, BigInteger maxFee)
            throws Throwable;

    /**
     * Deposits a token with an explicit fee sponsor.
     * Mirrors {@code BridgeContract.depositToken(token, from, to, amount, maxFee, feeSponsor)}.
     *
     * @param sender     account used as {@code calledByEntry(sender)} signer.
     * @param token      N3 token script hash.
     * @param from       source account script hash on N3.
     * @param to         recipient address on linked chain encoded as {@code Hash160}.
     * @param amount     token amount to bridge.
     * @param maxFee     max GAS fee accepted for this deposit.
     * @param feeSponsor account script hash that pays the deposit fee.
     * @return transaction hash.
     */
    Hash256 depositToken(Account sender, Hash160 token, Hash160 from, Hash160 to, BigInteger amount, BigInteger maxFee,
            Hash160 feeSponsor) throws Throwable;

    /**
     * Executes token withdrawals proven by validator signatures.
     * Mirrors {@code BridgeContract.withdrawToken(token, withdrawalRoot, signatures, withdrawals)}.
     *
     * @param sender         account used as {@code calledByEntry(sender)} signer; typically relayer.
     * @param token          token bridge for the withdrawal operation.
     * @param withdrawalRoot new root that must match contract-side verification.
     * @param signatures     validator signatures over {@code withdrawalRoot}.
     * @param withdrawals    serialized list parameter of withdrawals to execute.
     * @return transaction hash.
     */
    Hash256 withdrawToken(Account sender, Hash160 token, String withdrawalRoot,
            Map<ContractParameter, ContractParameter> signatures, ContractParameter withdrawals) throws Throwable;

    /**
     * Claims a previously marked claimable token withdrawal.
     *
     * @param sender account used as {@code calledByEntry(sender)} signer.
     * @param token  token bridge containing the claimable withdrawal.
     * @param nonce  nonce of the claimable withdrawal.
     * @return transaction hash.
     */
    Hash256 claimToken(Account sender, Hash160 token, BigInteger nonce) throws Throwable;

    /**
     * @return true if the token withdrawal nonce is claimable.
     */
    boolean isClaimableToken(Hash160 token, BigInteger nonce) throws IOException;

    /**
     * @return token deposit fee.
     */
    BigInteger tokenDepositFee(Hash160 token) throws IOException;

    /**
     * Updates token deposit fees.
     *
     * @param sender         account used as {@code calledByEntry(sender)} signer.
     * @param newDepositFees map of token hash to new fee.
     * @return transaction hash.
     */
    Hash256 setTokenDepositFee(Account sender, Map<Hash160, BigInteger> newDepositFees) throws Throwable;

    /**
     * @return minimum token deposit.
     */
    BigInteger minTokenDeposit(Hash160 token) throws IOException;

    /**
     * Updates minimum token deposits.
     *
     * @param sender         account used as {@code calledByEntry(sender)} signer.
     * @param newMinDeposits map of token hash to new minimum.
     * @return transaction hash.
     */
    Hash256 setMinTokenDeposit(Account sender, Map<Hash160, BigInteger> newMinDeposits) throws Throwable;

    /**
     * @return maximum token deposit.
     */
    BigInteger maxTokenDeposit(Hash160 token) throws IOException;

    /**
     * Updates maximum token deposits.
     *
     * @param sender         account used as {@code calledByEntry(sender)} signer.
     * @param newMaxDeposits map of token hash to new maximum.
     * @return transaction hash.
     */
    Hash256 setMaxTokenDeposit(Account sender, Map<Hash160, BigInteger> newMaxDeposits) throws Throwable;

    /**
     * @return maximum number of token withdrawals processed in one operation.
     */
    BigInteger maxTokenWithdrawals(Hash160 token) throws IOException;

    /**
     * Updates maximum token withdrawals per operation.
     *
     * @param sender            account used as {@code calledByEntry(sender)} signer.
     * @param newMaxWithdrawals map of token hash to new max withdrawals.
     * @return transaction hash.
     */
    Hash256 setMaxTokenWithdrawals(Account sender, Map<Hash160, Integer> newMaxWithdrawals) throws Throwable;

    /**
     * @return current token deposit nonce.
     */
    BigInteger tokenDepositNonce(Hash160 token) throws IOException;

    /**
     * @return current token deposit root.
     */
    String tokenDepositRoot(Hash160 token) throws IOException;

    /**
     * @return current token withdrawal nonce.
     */
    BigInteger tokenWithdrawalNonce(Hash160 token) throws IOException;

    /**
     * @return current token withdrawal root.
     */
    String tokenWithdrawalRoot(Hash160 token) throws IOException;
}
