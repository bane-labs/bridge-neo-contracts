package network.bane.bridge;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.annotations.Struct;
import io.neow3j.devpack.contracts.StdLib;
import network.bane.structs.GasBridge;
import network.bane.structs.GasBridgeV1;
import network.bane.structs.GasConfig;
import network.bane.structs.GasConfigV1;

import static io.neow3j.devpack.Helper.abort;
import static network.bane.bridge.StorageConstants.KEY_GAS_BRIDGE;
import static network.bane.bridge.StorageConstants.KEY_MIGRATED;

public class MigrationT4V1ToV2 {

    @Struct
    static class MigrationInfo {
        public int totalDeposited;
        public int maxTotalDeposited;

        public MigrationInfo(int totalDeposited, int maxTotalDeposited) {
            this.totalDeposited = totalDeposited;
            this.maxTotalDeposited = maxTotalDeposited;
        }
    }

    static void migrateV1(MigrationInfo migrationInfo) {
        ByteString serialized = BridgeContract.baseMap.get(KEY_GAS_BRIDGE);
        GasBridgeV1 gasBridgeV1 = (GasBridgeV1) new StdLib().deserialize(serialized);
        GasConfigV1 gasConfigV1 = gasBridgeV1.config;
        GasConfig newGasConfig = new GasConfig(gasConfigV1.depositFee, gasConfigV1.minAmount, gasConfigV1.maxAmount,
                gasConfigV1.maxWithdrawals, migrationInfo.maxTotalDeposited);
        GasBridge newGasBridge = new GasBridge(
                gasBridgeV1.paused,
                migrationInfo.totalDeposited,
                gasBridgeV1.depositState,
                gasBridgeV1.withdrawalState,
                newGasConfig);

        if (!GasBridge.isValid(newGasBridge)) {
            abort("Invalid gas bridge state.");
        }
        BridgeContract.baseMap.put(KEY_GAS_BRIDGE, new StdLib().serialize(newGasBridge));
        BridgeContract.baseMap.put(KEY_MIGRATED, 1);
    }

}
