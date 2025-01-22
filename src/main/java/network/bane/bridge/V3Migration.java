package network.bane.bridge;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.annotations.Struct;
import io.neow3j.devpack.contracts.GasToken;
import io.neow3j.devpack.contracts.StdLib;
import network.bane.structs.NativeTokenBridgeV2;
import network.bane.structs.NativeTokenBridgeV3;
import network.bane.structs.NativeTokenConfigV2;

import static io.neow3j.devpack.Helper.abort;
import static network.bane.bridge.StorageConstants.KEY_LINKED_CHAIN_ID;
import static network.bane.bridge.StorageConstants.KEY_NATIVE_BRIDGE;

public class V3Migration {
    @Struct
    private static class V3UpdateData {
        public Integer linkedChainId;
    }

    // Checks that the provided data's content is valid.
    static boolean isValid(Object data) {
        V3UpdateData migrationData = (V3UpdateData) data;
        return migrationData.linkedChainId != null && migrationData.linkedChainId > 0;
    }

    // Migrates the data from the previous version to the current version.
    static void migrate(Object data) {
        // Todo: Consider hardcoding this migration for Neo X testnet and mainnet to reduce manual input errors. If we
        //  deploy for other networks, we'll only deploy using v3, thus, no migration from v2 will be needed there.
        V3UpdateData migrationData = (V3UpdateData) data;
        if (!isValid(migrationData)) {
            abort("Invalid migration data");
        }
        BridgeContract.baseMap.put(KEY_LINKED_CHAIN_ID, migrationData.linkedChainId);

        // Read V2 of native bridge
        NativeTokenBridgeV2 nativeBridgeV2 = (NativeTokenBridgeV2) new StdLib().deserialize(
                BridgeContract.baseMap.get(KEY_NATIVE_BRIDGE));
        NativeTokenConfigV2 configV2 = nativeBridgeV2.config;

        // Build V3 of native bridge based on V2 data
        NativeTokenBridgeV3.NativeTokenConfigV3 configV3 = new NativeTokenBridgeV3.NativeTokenConfigV3(
                new GasToken().getHash(), 18, configV2.depositFee, configV2.minAmount, configV2.maxAmount,
                configV2.maxWithdrawals, configV2.maxTotalDeposited);
        NativeTokenBridgeV3 nativeBridgeV3 = new NativeTokenBridgeV3(nativeBridgeV2.paused,
                nativeBridgeV2.totalDeposited, nativeBridgeV2.depositState,
                nativeBridgeV2.withdrawalState, configV3);

        // Store the migrated native bridge V3
        ByteString serializedV3 = new StdLib().serialize(nativeBridgeV3);
        BridgeContract.baseMap.put(KEY_NATIVE_BRIDGE, serializedV3);
    }
}
