package network.bane.structs;

import io.neow3j.devpack.annotations.Struct;

@Struct
public class GasBridge {
    public boolean paused;
    public State depositState;
    public State withdrawalState;
    public GasConfig config;

    public GasBridge(boolean paused, State depositState, State withdrawalState, GasConfig config) {
        this.paused = paused;
        this.depositState = depositState;
        this.withdrawalState = withdrawalState;
        this.config = config;
    }

    public static boolean isValid(GasBridge gasBridge) {
        return gasBridge != null &&
                State.isValid(gasBridge.depositState) &&
                State.isValid(gasBridge.withdrawalState) &&
                GasConfig.isValid(gasBridge.config);
    }
}
