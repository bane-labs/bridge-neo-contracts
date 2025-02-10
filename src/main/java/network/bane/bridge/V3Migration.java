package network.bane.bridge;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Runtime;
import io.neow3j.devpack.contracts.GasToken;
import io.neow3j.devpack.contracts.StdLib;
import network.bane.structs.NativeTokenBridgeV2;
import network.bane.structs.NativeTokenBridgeV3;
import network.bane.structs.NativeTokenConfigV2;

import static io.neow3j.devpack.Helper.abort;
import static network.bane.bridge.StorageConstants.KEY_LINKED_CHAIN_ID;
import static network.bane.bridge.StorageConstants.KEY_NATIVE_BRIDGE;

public class V3Migration {

    private static final int NETWORK_N3_MAINNET = 860833102;
    private static final int NETWORK_N3_TESTNET = 894710606;

    private static final int CHAINID_NEOX_MAINNET = 47763;
    private static final int CHAINID_NEOX_TESTNET = 12227332;

    // Migrates the data from the previous version to the current version.
    static void migrate() {
        Integer linkedChain = null;
        int thisNetwork = Runtime.getNetwork();
        if (thisNetwork == NETWORK_N3_MAINNET) {
            linkedChain = CHAINID_NEOX_MAINNET;
        } else if (thisNetwork == NETWORK_N3_TESTNET) {
            linkedChain = CHAINID_NEOX_TESTNET;
        } else {
            abort("Unsupported network.");
        }
        BridgeContract.baseMap.put(KEY_LINKED_CHAIN_ID, linkedChain);

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
