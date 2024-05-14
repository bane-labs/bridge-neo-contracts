package network.bane.bridge;

import io.neow3j.devpack.Hash160;

import static io.neow3j.devpack.Helper.abort;

public class TokenBridgeImpl {
    static void onlyTokenBridgePaused(Hash160 token) {
        if (!BridgeContract.getTokenBridge(token).paused) abort("Token bridge is unpaused.");
    }

    static void onlyTokenBridgeUnpaused(Hash160 token) {
        if (BridgeContract.getTokenBridge(token).paused) abort("Token bridge is paused.");
    }
}
