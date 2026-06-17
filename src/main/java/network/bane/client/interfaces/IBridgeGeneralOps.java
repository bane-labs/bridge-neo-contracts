package network.bane.client.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.neow3j.contract.NefFile;
import io.neow3j.protocol.core.response.ContractManifest;
import io.neow3j.types.Hash160;

import java.io.IOException;
import java.math.BigInteger;

/**
 * General bridge operations exposed by the on-chain bridge contract.
 * <p>
 * These methods mirror contract entry points and keep the same naming to make script-to-contract
 * mapping explicit.
 */
public interface IBridgeGeneralOps {

    /**
     * Updates the bridge contract.
     *
     * @param nefFile  new contract NEF file.
     * @param manifest new contract manifest.
     * @param data     optional update payload passed to the on-chain {@code update} method.
     * @return update transaction hash.
     * @throws JsonProcessingException if the manifest cannot be serialized to JSON.
     */
    IWriteCaller update(NefFile nefFile, ContractManifest manifest, Object data) throws JsonProcessingException;

    /**
     * Pauses the full bridge.
     *
     * @return pause transaction hash.
     */
    IWriteCaller pauseBridge();

    /**
     * Unpauses the full bridge.
     *
     * @return unpause transaction hash.
     */
    IWriteCaller unpauseBridge();

    /**
     * @return true if the bridge is paused.
     * @throws IOException if the RPC call fails.
     */
    boolean isPaused() throws IOException;

    /**
     * Pauses deposits while keeping withdrawals available.
     *
     * @return pause transaction hash.
     */
    IWriteCaller pauseDeposits();

    /**
     * Unpauses deposits.
     *
     * @return unpause transaction hash.
     */
    IWriteCaller unpauseDeposits();

    /**
     * @return true if deposits are paused.
     * @throws IOException if the RPC call fails.
     */
    boolean depositsArePaused() throws IOException;

    /**
     * @return linked chain id configured in bridge state.
     * @throws IOException if the RPC call fails.
     */
    BigInteger linkedChainId() throws IOException;

    /**
     * @return script hash of the bridge management contract.
     * @throws IOException if the RPC call fails.
     */
    Hash160 management() throws IOException;

    /**
     * @return accumulated unclaimed operator rewards.
     * @throws IOException if the RPC call fails.
     */
    BigInteger unclaimedRewards() throws IOException;

    /**
     * @return rewards attributable to NEO holding.
     * @throws IOException if the RPC call fails.
     */
    BigInteger neoHoldingGasRewards() throws IOException;
}
