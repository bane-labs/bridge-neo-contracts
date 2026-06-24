package network.bane.structs;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.annotations.Struct;

@Struct
public class State {
    public int nonce;
    public ByteString root;

    public State(int nonce, ByteString root) {
        this.nonce = nonce;
        this.root = root;
    }

    public static boolean isValid(State state) {
        return state != null &&
                state.nonce >= 0 &&
                state.root != null &&
                state.root.length() == 32;
    }
}
