package network.bane.structs;

import io.neow3j.devpack.annotations.Struct;

@Struct
public class NativeTokenBridgeV2 {
    public boolean paused;
    // Note: If a future version of the bridge contract should contain a verify method, the computation of this value
    // should be kept in mind (and adapted), since otherwise the actual balance might differ from this accounting here.
    public int totalDeposited;
    public State depositState;
    public State withdrawalState;
    public NativeTokenConfigV2 config;

    public NativeTokenBridgeV2(boolean paused, Integer totalDeposited, State depositState, State withdrawalState,
            NativeTokenConfigV2 config) {
        this.paused = paused;
        this.totalDeposited = totalDeposited;
        this.depositState = depositState;
        this.withdrawalState = withdrawalState;
        this.config = config;
    }

    public static boolean isValid(NativeTokenBridgeV2 nativeTokenBridge) {
        return nativeTokenBridge != null &&
                nativeTokenBridge.totalDeposited >= 0 &&
                State.isValid(nativeTokenBridge.depositState) &&
                State.isValid(nativeTokenBridge.withdrawalState) &&
                NativeTokenConfigV2.isValid(nativeTokenBridge.config);
    }
}
