package network.bane.client;

import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;

import java.io.IOException;
import java.math.BigInteger;

/**
 * General bridge operations exposed by the on-chain bridge contract.
 * <p>
 * These methods mirror contract entry points and keep the same naming to make script-to-contract
 * mapping explicit.
 * <p>
 * Signer model: all write methods in this interface use a {@code calledByEntry(sender)} signer.
 */
public interface BridgeGeneralOps {

    /**
     * Updates the bridge contract.
     *
     * @param sender       account used as {@code calledByEntry(sender)} signer.
     * @param nefBytes     new contract NEF bytes.
     * @param manifestJson new contract manifest as JSON string.
     * @param data         optional update payload passed to the on-chain {@code update} method.
     * @return update transaction hash.
     */
    Hash256 update(Account sender, byte[] nefBytes, String manifestJson, Object data) throws Throwable;

    /**
     * Pauses the full bridge.
     *
     * @param sender account used as {@code calledByEntry(sender)} signer.
     * @return pause transaction hash.
     */
    Hash256 pauseBridge(Account sender) throws Throwable;

    /**
     * Unpauses the full bridge.
     *
     * @param sender account used as {@code calledByEntry(sender)} signer.
     * @return unpause transaction hash.
     */
    Hash256 unpauseBridge(Account sender) throws Throwable;

    /**
     * @return true if the bridge is paused.
     */
    boolean isPaused() throws IOException;

    /**
     * Pauses deposits while keeping withdrawals available.
     *
     * @param sender account used as {@code calledByEntry(sender)} signer.
     * @return pause transaction hash.
     */
    Hash256 pauseDeposits(Account sender) throws Throwable;

    /**
     * Unpauses deposits.
     *
     * @param sender account used as {@code calledByEntry(sender)} signer.
     * @return unpause transaction hash.
     */
    Hash256 unpauseDeposits(Account sender) throws Throwable;

    /**
     * @return true if deposits are paused.
     */
    boolean depositsArePaused() throws IOException;

    /**
     * @return linked chain id configured in bridge state.
     */
    BigInteger linkedChainId() throws IOException;

    /**
     * @return script hash of the bridge management contract.
     */
    Hash160 management() throws IOException;

    /**
     * @return accumulated unclaimed operator rewards.
     */
    BigInteger unclaimedRewards() throws IOException;

    /**
     * @return rewards attributable to NEO holding.
     */
    BigInteger neoHoldingGasRewards() throws IOException;
}
