package network.bane.bridge;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Helper;
import io.neow3j.devpack.Iterator;
import io.neow3j.devpack.Map;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.contracts.StdLib;
import network.bane.structs.TokenBridge;

import static network.bane.bridge.StorageConstants.PREFIX_TOKEN_BRIDGES;

public class BridgeMigrationV1ToV2 {

    static void migrateV1ToV2(Map<Hash160, Integer> tokenBridgesDecimalScalingFactors) {
        StorageMap tokenBridges = new StorageMap(BridgeContract.ctx, PREFIX_TOKEN_BRIDGES);
        Iterator<Hash160> it = TokenBridgeImpl.getTokenBridgeTokensIterator();
        StdLib stdLib = new StdLib();
        while (it.next()) {
            Hash160 token = it.get();
            Integer decimalScalingFactor = tokenBridgesDecimalScalingFactors.get(token);
            if (decimalScalingFactor == null) {
                // Requires that all registered tokens are in the map, so that none can be overlooked.
                Helper.abort("Missing entry for token: " + token);
            }

            TokenBridge tokenBridge = (TokenBridge) stdLib.deserialize(tokenBridges.get(token));
            TokenBridge.TokenConfig config = tokenBridge.config;

            TokenBridge.TokenConfig newConfig = new TokenBridge.TokenConfig(
                    config.neoXToken,
                    config.fee,
                    config.minAmount,
                    config.maxAmount,
                    config.maxWithdrawals,
                    decimalScalingFactor // overwrite the previous execution type with the decimal scaling factor
            );
            TokenBridge newTokenBridge = new TokenBridge(
                    tokenBridge.paused,
                    tokenBridge.depositState,
                    tokenBridge.withdrawalState,
                    newConfig
            );

            tokenBridges.put(token, stdLib.serialize(newTokenBridge));
        }
    }

}
