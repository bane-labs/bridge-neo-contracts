package network.bane.client;

import io.neow3j.types.Hash160;
import network.bane.client.interfaces.WriteCaller;

import java.io.IOException;
import java.math.BigInteger;

/**
 * General bridge operations exposed by the on-chain bridge contract.
 * <p>
 * These methods mirror contract entry points and keep the same naming to make script-to-contract
 * mapping explicit.
 */
public interface BridgeGeneralOps {

    /**
     * Updates the bridge contract.
     *
     * @param nefBytes     new contract NEF bytes.
     * @param manifestJson new contract manifest as JSON string.
     * @param data         optional update payload passed to the on-chain {@code update} method.
     * @return update transaction hash.
     */
    WriteCaller update(byte[] nefBytes, String manifestJson, Object data);

    /**
     * Pauses the full bridge.
     *
     * @return pause transaction hash.
     */
    WriteCaller pauseBridge();

    /**
     * Unpauses the full bridge.
     *
     * @return unpause transaction hash.
     */
    WriteCaller unpauseBridge();

    /**
     * @return true if the bridge is paused.
     */
    boolean isPaused() throws IOException;

    /**
     * Pauses deposits while keeping withdrawals available.
     *
     * @return pause transaction hash.
     */
    WriteCaller pauseDeposits();

    /**
     * Unpauses deposits.
     *
     * @return unpause transaction hash.
     */
    WriteCaller unpauseDeposits();

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
