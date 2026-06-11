package network.bane.utils.bridge;

import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;
import network.bane.utils.structs.NativeBridge;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;

/**
 * Native bridge operations exposed by the on-chain bridge contract.
 * <p>
 * Signer model:
 * <ul>
 *   <li>Most write methods use {@code calledByEntry(sender)}.</li>
 *   <li>{@code depositNative(...)} and {@code claimNative(...)} use restricted signers and limit
 *   allowed contracts to GAS ({@code GasToken.SCRIPT_HASH}).</li>
 * </ul>
 */
public interface BridgeNativeOps {

    /**
     * Sets native bridge configuration.
     *
     * @param sender                account used as {@code calledByEntry(sender)} signer.
     * @param tokenForNativeBridge  native token representative on N3.
     * @param decimalsOnLinkedChain decimals of the linked-chain token representation.
     * @param depositFee            fee charged in GAS for each deposit.
     * @param minAmount             minimum accepted deposit amount.
     * @param maxAmount             maximum accepted deposit amount.
     * @param maxWithdrawals        max withdrawals allowed per relayed withdrawal call.
     * @param maxTotalDeposited     cap for cumulative deposited amount in native bridge state.
     * @return transaction hash.
     */
    Hash256 setNativeBridge(Account sender, Hash160 tokenForNativeBridge, int decimalsOnLinkedChain,
            BigInteger depositFee, BigInteger minAmount, BigInteger maxAmount, int maxWithdrawals,
            BigInteger maxTotalDeposited) throws Throwable;

    /**
     * Pauses native bridge operations.
     *
     * @param sender account used as {@code calledByEntry(sender)} signer.
     * @return transaction hash.
     */
    Hash256 pauseNativeBridge(Account sender) throws Throwable;

    /**
     * Unpauses native bridge operations.
     *
     * @param sender account used as {@code calledByEntry(sender)} signer.
     * @return transaction hash.
     */
    Hash256 unpauseNativeBridge(Account sender) throws Throwable;

    /**
     * Deposits native token to be bridged to the linked chain.
     * Mirrors {@code BridgeContract.depositNative(from, to, amount, maxFee)}.
     *
     * @param sender signer account; submitted with a restricted signer scope that only allows GAS
     *               transfers required for paying bridge fees.
     * @param from   source account script hash on N3.
     * @param to     recipient address on the linked chain encoded as {@code Hash160}.
     * @param amount native token amount to deposit.
     * @param maxFee max GAS fee the depositor is willing to pay.
     * @return transaction hash.
     */
    Hash256 depositNative(Account sender, Hash160 from, Hash160 to, BigInteger amount, BigInteger maxFee)
            throws Throwable;

    /**
     * Deposits native token with an explicit fee sponsor.
     * Mirrors {@code BridgeContract.depositNative(from, to, amount, maxFee, feeSponsor)}.
     *
     * @param sender     signer account; submitted with a restricted signer scope that only allows GAS
     *                   transfers required for paying bridge fees.
     * @param from       source account script hash on N3.
     * @param to         recipient address on the linked chain encoded as {@code Hash160}.
     * @param amount     native token amount to deposit.
     * @param maxFee     max GAS fee accepted for this deposit.
     * @param feeSponsor account script hash that pays the fee; if null, the contract treats {@code from}
     *                   as fee payer.
     * @return transaction hash.
     */
    Hash256 depositNative(Account sender, Hash160 from, Hash160 to, BigInteger amount, BigInteger maxFee,
            Hash160 feeSponsor) throws Throwable;

    /**
     * Executes native withdrawals proven by validator signatures.
     * Mirrors {@code BridgeContract.withdrawNative(withdrawalRoot, signatures, withdrawals)}.
     *
     * @param sender         account used as {@code calledByEntry(sender)} signer; typically relayer.
     * @param withdrawalRoot new withdrawal root that must match contract-side verification.
     * @param signatures     validator signatures over {@code withdrawalRoot}.
     * @param withdrawals    serialized list parameter of withdrawals to execute.
     * @return transaction hash.
     */
    Hash256 withdrawNative(Account sender, String withdrawalRoot,
            Map<ContractParameter, ContractParameter> signatures,
            ContractParameter withdrawals) throws Throwable;

    /**
     * Claims a previously marked claimable native withdrawal.
     *
     * @param sender signer account; submitted with a restricted signer scope that only allows GAS
     *               transfer to the claim recipient.
     * @param nonce  nonce of the claimable withdrawal.
     * @return transaction hash.
     */
    Hash256 claimNative(Account sender, BigInteger nonce) throws Throwable;

    /**
     * @return true if the native withdrawal nonce is claimable.
     */
    boolean isClaimableNative(BigInteger nonce) throws IOException;

    /**
     * @return true if native bridge state is initialized.
     */
    boolean nativeBridgeIsSet() throws IOException;

    /**
     * @return native token script hash configured for the bridge.
     */
    Hash160 nativeToken() throws IOException;

    /**
     * @return full native bridge state DTO.
     */
    NativeBridge getNativeBridge() throws IOException;

    /**
     * @return native deposit fee.
     */
    BigInteger nativeDepositFee() throws IOException;

    /**
     * Updates native deposit fee.
     *
     * @param sender account used as {@code calledByEntry(sender)} signer.
     * @param newFee new deposit fee.
     * @return transaction hash.
     */
    Hash256 setNativeDepositFee(Account sender, BigInteger newFee) throws Throwable;

    /**
     * @return minimum accepted native deposit.
     */
    BigInteger minNativeDeposit() throws IOException;

    /**
     * Updates minimum accepted native deposit.
     *
     * @param sender       account used as {@code calledByEntry(sender)} signer.
     * @param newMinAmount new minimum deposit value.
     * @return transaction hash.
     */
    Hash256 setMinNativeDeposit(Account sender, BigInteger newMinAmount) throws Throwable;

    /**
     * @return maximum accepted native deposit.
     */
    BigInteger maxNativeDeposit() throws IOException;

    /**
     * Updates maximum accepted native deposit.
     *
     * @param sender       account used as {@code calledByEntry(sender)} signer.
     * @param newMaxAmount new maximum deposit value.
     * @return transaction hash.
     */
    Hash256 setMaxNativeDeposit(Account sender, BigInteger newMaxAmount) throws Throwable;

    /**
     * @return cap for total native amount deposited through the bridge.
     */
    BigInteger maxTotalDepositedNative() throws IOException;

    /**
     * Updates cap for total native amount deposited through the bridge.
     *
     * @param sender               account used as {@code calledByEntry(sender)} signer.
     * @param newMaxTotalDeposited new cap for cumulative deposits.
     * @return transaction hash.
     */
    Hash256 setMaxTotalDepositedNative(Account sender, BigInteger newMaxTotalDeposited) throws Throwable;

    /**
     * @return current native deposit nonce.
     */
    BigInteger nativeDepositNonce() throws IOException;

    /**
     * @return current native deposit root.
     */
    String nativeDepositRoot() throws IOException;

    /**
     * @return current native withdrawal nonce.
     */
    BigInteger nativeWithdrawalNonce() throws IOException;

    /**
     * @return current native withdrawal root.
     */
    String nativeWithdrawalRoot() throws IOException;
}
