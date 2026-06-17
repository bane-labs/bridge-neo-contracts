package network.bane.client.interfaces;

import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import network.bane.dto.bridge.NativeBridge;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;

/**
 * Native bridge operations exposed by the on-chain bridge contract.
 * <p>
 * Signer model:
 * <ul>
 *   <li>{@code depositNative(...)} and {@code claimNative(...)} use restricted signers and limit
 *   allowed contracts to GAS ({@code GasToken.SCRIPT_HASH}).</li>
 * </ul>
 */
public interface IBridgeNativeOps {

    /**
     * Sets native bridge configuration.
     *
     * @param tokenForNativeBridge  native token representative on N3.
     * @param decimalsOnLinkedChain decimals of the linked-chain token representation.
     * @param depositFee            fee charged in GAS for each deposit.
     * @param minAmount             minimum accepted deposit amount.
     * @param maxAmount             maximum accepted deposit amount.
     * @param maxWithdrawals        max withdrawals allowed per relayed withdrawal call.
     * @param maxTotalDeposited     cap for cumulative deposited amount in native bridge state.
     * @return transaction hash.
     */
    IWriteCaller setNativeBridge(Hash160 tokenForNativeBridge, int decimalsOnLinkedChain, BigInteger depositFee,
            BigInteger minAmount, BigInteger maxAmount, int maxWithdrawals, BigInteger maxTotalDeposited);

    /**
     * Pauses native bridge operations.
     *
     * @return transaction hash.
     */
    IWriteCaller pauseNativeBridge();

    /**
     * Unpauses native bridge operations.
     *
     * @return transaction hash.
     */
    IWriteCaller unpauseNativeBridge();

    /**
     * Deposits native token to be bridged to the linked chain.
     * Mirrors {@code BridgeContract.depositNative(from, to, amount, maxFee)}.
     *
     * @param from   source account script hash on N3.
     * @param to     recipient address on the linked chain encoded as {@code Hash160}.
     * @param amount native token amount to deposit.
     * @param maxFee max GAS fee the depositor is willing to pay.
     * @return transaction hash.
     */
    IWriteCaller depositNative(Hash160 from, Hash160 to, BigInteger amount, BigInteger maxFee);

    /**
     * Deposits native token with an explicit fee sponsor.
     * Mirrors {@code BridgeContract.depositNative(from, to, amount, maxFee, feeSponsor)}.
     *
     * @param from       source account script hash on N3.
     * @param to         recipient address on the linked chain encoded as {@code Hash160}.
     * @param amount     native token amount to deposit.
     * @param maxFee     max GAS fee accepted for this deposit.
     * @param feeSponsor account script hash that pays the fee; if null, the contract treats {@code from}
     *                   as fee payer.
     * @return transaction hash.
     */
    IWriteCaller depositNative(Hash160 from, Hash160 to, BigInteger amount, BigInteger maxFee, Hash160 feeSponsor);

    /**
     * Executes native withdrawals proven by validator signatures.
     * Mirrors {@code BridgeContract.withdrawNative(withdrawalRoot, signatures, withdrawals)}.
     *
     * @param withdrawalRoot new withdrawal root that must match contract-side verification.
     * @param signatures     validator signatures over {@code withdrawalRoot}.
     * @param withdrawals    serialized list parameter of withdrawals to execute.
     * @return transaction hash.
     */
    IWriteCaller withdrawNative(String withdrawalRoot, Map<ContractParameter, ContractParameter> signatures,
            ContractParameter withdrawals);

    /**
     * Claims a previously marked claimable native withdrawal.
     *
     * @param nonce nonce of the claimable withdrawal.
     * @return transaction hash.
     */
    IWriteCaller claimNative(BigInteger nonce);

    /**
     * @return true if the native withdrawal nonce is claimable.
     * @throws IOException if the RPC call fails.
     */
    boolean isClaimableNative(BigInteger nonce) throws IOException;

    /**
     * @return true if native bridge state is initialized.
     * @throws IOException if the RPC call fails.
     */
    boolean nativeBridgeIsSet() throws IOException;

    /**
     * @return native token script hash configured for the bridge.
     * @throws IOException if the RPC call fails.
     */
    Hash160 nativeToken() throws IOException;

    /**
     * @return full native bridge state DTO.
     * @throws IOException if the RPC call fails.
     */
    NativeBridge getNativeBridge() throws IOException;

    /**
     * @return native deposit fee.
     * @throws IOException if the RPC call fails.
     */
    BigInteger nativeDepositFee() throws IOException;

    /**
     * Updates native deposit fee.
     *
     * @param newFee new deposit fee.
     * @return transaction hash.
     */
    IWriteCaller setNativeDepositFee(BigInteger newFee);

    /**
     * @return minimum accepted native deposit.
     * @throws IOException if the RPC call fails.
     */
    BigInteger minNativeDeposit() throws IOException;

    /**
     * Updates minimum accepted native deposit.
     *
     * @param newMinAmount new minimum deposit value.
     * @return transaction hash.
     */
    IWriteCaller setMinNativeDeposit(BigInteger newMinAmount);

    /**
     * @return maximum accepted native deposit.
     * @throws IOException if the RPC call fails.
     */
    BigInteger maxNativeDeposit() throws IOException;

    /**
     * Updates maximum accepted native deposit.
     *
     * @param newMaxAmount new maximum deposit value.
     * @return transaction hash.
     */
    IWriteCaller setMaxNativeDeposit(BigInteger newMaxAmount);

    /**
     * @return cap for total native amount deposited through the bridge.
     * @throws IOException if the RPC call fails.
     */
    BigInteger maxTotalDepositedNative() throws IOException;

    /**
     * Updates cap for total native amount deposited through the bridge.
     *
     * @param newMaxTotalDeposited new cap for cumulative deposits.
     * @return transaction hash.
     */
    IWriteCaller setMaxTotalDepositedNative(BigInteger newMaxTotalDeposited);

    /**
     * @return current native deposit nonce.
     * @throws IOException if the RPC call fails.
     */
    BigInteger nativeDepositNonce() throws IOException;

    /**
     * @return current native deposit root.
     * @throws IOException if the RPC call fails.
     */
    String nativeDepositRoot() throws IOException;

    /**
     * @return current native withdrawal nonce.
     * @throws IOException if the RPC call fails.
     */
    BigInteger nativeWithdrawalNonce() throws IOException;

    /**
     * @return current native withdrawal root.
     * @throws IOException if the RPC call fails.
     */
    String nativeWithdrawalRoot() throws IOException;
}
