package network.bane.structs.message;

import io.neow3j.devpack.annotations.Struct;

@Struct
public class ExecutionState {
    public boolean executed;
    public int expirationTime;

    public ExecutionState(boolean executed, int expirationTime) {
        this.executed = executed;
        this.expirationTime = expirationTime;
    }

}
