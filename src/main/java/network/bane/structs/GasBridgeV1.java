package network.bane.structs;

import io.neow3j.devpack.annotations.Struct;

@Struct
public class GasBridgeV1 {
    public boolean paused;
    public State depositState;
    public State withdrawalState;
    public GasConfigV1 config;

    public GasBridgeV1(boolean paused, State depositState, State withdrawalState, GasConfigV1 config) {
        this.paused = paused;
        this.depositState = depositState;
        this.withdrawalState = withdrawalState;
        this.config = config;
    }
}
