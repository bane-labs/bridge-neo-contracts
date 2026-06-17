package network.bane.client;

import io.neow3j.contract.Iterator;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import network.bane.client.interfaces.WriteCaller;
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
    Iterator<Hash160> getRegisteredTokensIterator() throws IOException;

    /**
     * @return registered token script hashes.
     */
    List<Hash160> getRegisteredTokens() throws IOException;

    /**
     * Registers a token bridge.
     *
     * @param token       N3 token script hash.
     * @param tokenConfig destination token and bridge config.
     * @return transaction hash.
     */
    WriteCaller registerToken(Hash160 token, TokenBridge.TokenConfig tokenConfig) ;

    /**
     * Pauses a token bridge.
     *
     * @param neoN3Token token bridge to pause.
     * @return transaction hash.
     */
    WriteCaller pauseTokenBridge(Hash160 neoN3Token) ;

    /**
     * Unpauses a token bridge.
     *
     * @param neoN3Token token bridge to unpause.
     * @return transaction hash.
     */
    WriteCaller unpauseTokenBridge(Hash160 neoN3Token);

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
    WriteCaller depositToken(Hash160 token, Hash160 from, Hash160 to, BigInteger amount, BigInteger maxFee);

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
    WriteCaller depositToken(Hash160 token, Hash160 from, Hash160 to, BigInteger amount, BigInteger maxFee,
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
    WriteCaller withdrawToken(Hash160 token, String withdrawalRoot,
            Map<ContractParameter, ContractParameter> signatures, ContractParameter withdrawals);

    /**
     * Claims a previously marked claimable token withdrawal.
     *
     * @param token  token bridge containing the claimable withdrawal.
     * @param nonce  nonce of the claimable withdrawal.
     * @return transaction hash.
     */
    WriteCaller claimToken(Hash160 token, BigInteger nonce);

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
     * @param newDepositFees map of token hash to new fee.
     * @return transaction hash.
     */
    WriteCaller setTokenDepositFee(Map<Hash160, BigInteger> newDepositFees);

    /**
     * @return minimum token deposit.
     */
    BigInteger minTokenDeposit(Hash160 token) throws IOException;

    /**
     * Updates minimum token deposits.
     *
     * @param newMinDeposits map of token hash to new minimum.
     * @return transaction hash.
     */
    WriteCaller setMinTokenDeposit(Map<Hash160, BigInteger> newMinDeposits);

    /**
     * @return maximum token deposit.
     */
    BigInteger maxTokenDeposit(Hash160 token) throws IOException;

    /**
     * Updates maximum token deposits.
     *
     * @param newMaxDeposits map of token hash to new maximum.
     * @return transaction hash.
     */
    WriteCaller setMaxTokenDeposit(Map<Hash160, BigInteger> newMaxDeposits);

    /**
     * @return maximum number of token withdrawals processed in one operation.
     */
    BigInteger maxTokenWithdrawals(Hash160 token) throws IOException;

    /**
     * Updates maximum token withdrawals per operation.
     *
     * @param newMaxWithdrawals map of token hash to new max withdrawals.
     * @return transaction hash.
     */
    WriteCaller setMaxTokenWithdrawals(Map<Hash160, Integer> newMaxWithdrawals);

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
