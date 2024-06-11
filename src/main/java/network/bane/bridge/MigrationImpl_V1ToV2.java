package network.bane.bridge;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.contracts.StdLib;
import network.bane.structs.GasBridge;
import network.bane.structs.GasConfig;
import network.bane.structs.State;


import static io.neow3j.devpack.Helper.abort;
import static network.bane.bridge.BridgeContract.baseMap;
import static network.bane.bridge.StorageConstants.KEY_BRIDGE_PAUSE;
import static network.bane.bridge.StorageConstants.KEY_GAS_BRIDGE;
import static network.bane.bridge.StorageConstants.KEY_MIGRATED;
import static network.bane.bridge.StorageConstants.KEY_UNCLAIMED_REWARDS;
import static network.bane.bridge.StorageConstants.OLD_KEY_GAS_DEPOSIT_FEE;
import static network.bane.bridge.StorageConstants.OLD_KEY_GAS_DEPOSIT_MAX_AMOUNT;
import static network.bane.bridge.StorageConstants.OLD_KEY_GAS_DEPOSIT_MIN_AMOUNT;
import static network.bane.bridge.StorageConstants.OLD_KEY_GAS_DEPOSIT_NONCE;
import static network.bane.bridge.StorageConstants.OLD_KEY_GAS_DEPOSIT_ROOT;
import static network.bane.bridge.StorageConstants.OLD_KEY_GAS_WITHDRAWAL_NONCE;
import static network.bane.bridge.StorageConstants.OLD_KEY_GAS_WITHDRAWAL_ROOT;
import static network.bane.bridge.StorageConstants.OLD_KEY_PAUSED;

public class MigrationImpl_V1ToV2 {

    /**
     * This method is only used for the T3 contract update migration.
     * Once that contract update is done, this method will be removed from this contract.
     */
    static void migrateT3_v1Tov2() {
        if (baseMap.get(KEY_MIGRATED) != null) abort("Already migrated.");
        if (!baseMap.getBoolean(OLD_KEY_PAUSED)) abort("Contract must be paused for migration.");
        baseMap.put(KEY_MIGRATED, 1);

        Integer gasDepositFee = baseMap.getInt(OLD_KEY_GAS_DEPOSIT_FEE);
        baseMap.delete(OLD_KEY_GAS_DEPOSIT_FEE);
        Integer gasMinDepositAmount = baseMap.getInt(OLD_KEY_GAS_DEPOSIT_MIN_AMOUNT);
        baseMap.delete(OLD_KEY_GAS_DEPOSIT_MIN_AMOUNT);
        Integer gasMaxDepositAmount = baseMap.getInt(OLD_KEY_GAS_DEPOSIT_MAX_AMOUNT);
        baseMap.delete(OLD_KEY_GAS_DEPOSIT_MAX_AMOUNT);
        Integer pausedState = baseMap.getInt(OLD_KEY_PAUSED);
        baseMap.delete(OLD_KEY_PAUSED);

        ByteString gasDepositRoot = baseMap.get(OLD_KEY_GAS_DEPOSIT_ROOT);
        baseMap.delete(OLD_KEY_GAS_DEPOSIT_ROOT);
        int gasDepositNonce = baseMap.getInt(OLD_KEY_GAS_DEPOSIT_NONCE);
        baseMap.delete(OLD_KEY_GAS_DEPOSIT_NONCE);

        ByteString gasWithdrawalRoot = baseMap.get(OLD_KEY_GAS_WITHDRAWAL_ROOT);
        baseMap.delete(OLD_KEY_GAS_WITHDRAWAL_ROOT);
        int gasWithdrawalNonce = baseMap.getInt(OLD_KEY_GAS_WITHDRAWAL_NONCE);
        baseMap.delete(OLD_KEY_GAS_WITHDRAWAL_NONCE);
        // At this stage every storage entry that needs to be migrated has been read and deleted.

        // Now we can write the new storage entries.
        baseMap.put(KEY_BRIDGE_PAUSE, pausedState);
        GasConfig gasConfig = new GasConfig(gasDepositFee, gasMinDepositAmount, gasMaxDepositAmount, 100);

        State depositState = new State(gasDepositNonce, gasDepositRoot);
        State withdrawalState = new State(gasWithdrawalNonce, gasWithdrawalRoot);
        GasBridge gasBridge = new GasBridge(false, depositState, withdrawalState, gasConfig);
        assert GasBridge.isValid(gasBridge) : "Invalid gas bridge state.";
        ByteString serialized = new StdLib().serialize(gasBridge);
        baseMap.put(KEY_GAS_BRIDGE, serialized);
        // In order to remain simple here as this is only used for the T3 contract update, the already collected fees
        // up to this migration are ignored.
        baseMap.put(KEY_UNCLAIMED_REWARDS, 0);
    }

}
