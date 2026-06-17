package network.bane.client.interfaces;

import io.neow3j.contract.Iterator;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import network.bane.dto.bridge.TokenBridge;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * Token bridge operations exposed by the on-chain bridge contract.
 * <p>
 * If your script needs stricter scopes (e.g., allowed-contract restrictions), construct the
 * invocation manually.
 */
public interface IBridgeTokenOps {

    /**
     * @return true if token is registered on the bridge.
     * @throws IOException if the RPC call fails.
     */
    boolean isRegisteredToken(Hash160 token) throws IOException;

    /**
     * @return full token bridge state DTO.
     * @throws IOException if the RPC call fails.
     */
    TokenBridge getTokenBridge(Hash160 token) throws IOException;

    /**
     * @return iterator stack item over registered tokens.
     * @throws IOException if the RPC call fails.
     */
    Iterator<Hash160> getRegisteredTokensIterator() throws IOException;

    /**
     * @return registered token script hashes.
     * @throws IOException if the RPC call fails.
     */
    List<Hash160> getRegisteredTokens() throws IOException;

    /**
     * Registers a token bridge.
     *
     * @param token       N3 token script hash.
     * @param tokenConfig destination token and bridge config.
     * @return transaction hash.
     */
    IWriteCaller registerToken(Hash160 token, TokenBridge.TokenConfig tokenConfig);

    /**
     * Pauses a token bridge.
     *
     * @param neoN3Token token bridge to pause.
     * @return transaction hash.
     */
    IWriteCaller pauseTokenBridge(Hash160 neoN3Token);

    /**
     * Unpauses a token bridge.
     *
     * @param neoN3Token token bridge to unpause.
     * @return transaction hash.
     */
    IWriteCaller unpauseTokenBridge(Hash160 neoN3Token);

    /**
     * Deposits a token to be bridged to the linked chain.
     * Mirrors {@code BridgeContract.depositToken(token, from, to, amount, maxFee)}.
     *
     * @param token  N3 token script hash.
     * @param from   source account script hash on N3.
     * @param to     recipient address on linked chain encoded as {@code Hash160}.
     * @param amount token amount to bridge.
     * @param maxFee max GAS fee accepted for this deposit.
     * @return transaction hash.
     */
    IWriteCaller depositToken(Hash160 token, Hash160 from, Hash160 to, BigInteger amount, BigInteger maxFee);

    /**
     * Deposits a token with an explicit fee sponsor.
     * Mirrors {@code BridgeContract.depositToken(token, from, to, amount, maxFee, feeSponsor)}.
     *
     * @param token      N3 token script hash.
     * @param from       source account script hash on N3.
     * @param to         recipient address on linked chain encoded as {@code Hash160}.
     * @param amount     token amount to bridge.
     * @param maxFee     max GAS fee accepted for this deposit.
     * @param feeSponsor account script hash that pays the deposit fee.
     * @return transaction hash.
     */
    IWriteCaller depositToken(Hash160 token, Hash160 from, Hash160 to, BigInteger amount, BigInteger maxFee,
            Hash160 feeSponsor);

    /**
     * Executes token withdrawals proven by validator signatures.
     * Mirrors {@code BridgeContract.withdrawToken(token, withdrawalRoot, signatures, withdrawals)}.
     *
     * @param token          token bridge for the withdrawal operation.
     * @param withdrawalRoot new root that must match contract-side verification.
     * @param signatures     validator signatures over {@code withdrawalRoot}.
     * @param withdrawals    serialized list parameter of withdrawals to execute.
     * @return transaction hash.
     */
    IWriteCaller withdrawToken(Hash160 token, String withdrawalRoot,
            Map<ContractParameter, ContractParameter> signatures, ContractParameter withdrawals);

    /**
     * Claims a previously marked claimable token withdrawal.
     *
     * @param token token bridge containing the claimable withdrawal.
     * @param nonce nonce of the claimable withdrawal.
     * @return transaction hash.
     */
    IWriteCaller claimToken(Hash160 token, BigInteger nonce);

    /**
     * @return true if the token withdrawal nonce is claimable.
     * @throws IOException if the RPC call fails.
     */
    boolean isClaimableToken(Hash160 token, BigInteger nonce) throws IOException;

    /**
     * @return token deposit fee.
     * @throws IOException if the RPC call fails.
     */
    BigInteger tokenDepositFee(Hash160 token) throws IOException;

    /**
     * Updates token deposit fees.
     *
     * @param newDepositFees map of token hash to new fee.
     * @return transaction hash.
     */
    IWriteCaller setTokenDepositFee(Map<Hash160, BigInteger> newDepositFees);

    /**
     * @return minimum token deposit.
     * @throws IOException if the RPC call fails.
     */
    BigInteger minTokenDeposit(Hash160 token) throws IOException;

    /**
     * Updates minimum token deposits.
     *
     * @param newMinDeposits map of token hash to new minimum.
     * @return transaction hash.
     */
    IWriteCaller setMinTokenDeposit(Map<Hash160, BigInteger> newMinDeposits);

    /**
     * @return maximum token deposit.
     * @throws IOException if the RPC call fails.
     */
    BigInteger maxTokenDeposit(Hash160 token) throws IOException;

    /**
     * Updates maximum token deposits.
     *
     * @param newMaxDeposits map of token hash to new maximum.
     * @return transaction hash.
     */
    IWriteCaller setMaxTokenDeposit(Map<Hash160, BigInteger> newMaxDeposits);

    /**
     * @return maximum number of token withdrawals processed in one operation.
     * @throws IOException if the RPC call fails.
     */
    BigInteger maxTokenWithdrawals(Hash160 token) throws IOException;

    /**
     * Updates maximum token withdrawals per operation.
     *
     * @param newMaxWithdrawals map of token hash to new max withdrawals.
     * @return transaction hash.
     */
    IWriteCaller setMaxTokenWithdrawals(Map<Hash160, Integer> newMaxWithdrawals);

    /**
     * @return current token deposit nonce.
     * @throws IOException if the RPC call fails.
     */
    BigInteger tokenDepositNonce(Hash160 token) throws IOException;

    /**
     * @return current token deposit root.
     * @throws IOException if the RPC call fails.
     */
    String tokenDepositRoot(Hash160 token) throws IOException;

    /**
     * @return current token withdrawal nonce.
     * @throws IOException if the RPC call fails.
     */
    BigInteger tokenWithdrawalNonce(Hash160 token) throws IOException;

    /**
     * @return current token withdrawal root.
     * @throws IOException if the RPC call fails.
     */
    String tokenWithdrawalRoot(Hash160 token) throws IOException;
}
