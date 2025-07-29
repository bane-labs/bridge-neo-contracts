package network.bane.structs.message;

import io.neow3j.devpack.annotations.Struct;

@Struct
public class ExecutableState {
    public boolean executed;
    public int expirationTime;

    public ExecutableState(boolean executed, int expirationTime) {
        this.executed = executed;
        this.expirationTime = expirationTime;
    }

}
