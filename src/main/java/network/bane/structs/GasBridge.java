package network.bane.structs;

import io.neow3j.devpack.annotations.Struct;

@Struct
public class GasBridge {
    public boolean paused;
    public int totalDeposited;
    public State depositState;
    public State withdrawalState;
    public GasConfig config;

    public GasBridge(boolean paused, Integer totalDeposited, State depositState, State withdrawalState,
            GasConfig config) {
        this.paused = paused;
        this.totalDeposited = totalDeposited;
        this.depositState = depositState;
        this.withdrawalState = withdrawalState;
        this.config = config;
    }

    public static boolean isValid(GasBridge gasBridge) {
        return gasBridge != null &&
                gasBridge.totalDeposited >= 0 &&
                State.isValid(gasBridge.depositState) &&
                State.isValid(gasBridge.withdrawalState) &&
                GasConfig.isValid(gasBridge.config);
    }
}
