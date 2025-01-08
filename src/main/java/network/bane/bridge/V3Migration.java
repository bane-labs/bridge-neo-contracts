package network.bane.bridge;

import io.neow3j.devpack.annotations.Struct;

import static io.neow3j.devpack.Helper.abort;
import static network.bane.bridge.StorageConstants.KEY_TARGET_CHAIN_ID;

public class V3Migration {
    @Struct
    private static class V3UpdateData {
        public Integer targetChainId;
    }

    // Checks that the provided data's content is valid.
    static boolean isValid(Object data) {
        V3UpdateData migrationData = (V3UpdateData) data;
        return migrationData.targetChainId != null && migrationData.targetChainId > 0;
    }

    // Migrates the data from the previous version to the current version.
    static void migrate(Object data) {
        V3UpdateData migrationData = (V3UpdateData) data;
        if (!isValid(migrationData)) {
            abort("Invalid migration data");
        }
        BridgeContract.baseMap.put(KEY_TARGET_CHAIN_ID, migrationData.targetChainId);
    }
}
