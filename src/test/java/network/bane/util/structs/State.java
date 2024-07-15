package network.bane.util.structs;

import io.neow3j.types.Hash256;

import java.math.BigInteger;

public class State {
    public BigInteger nonce;
    public Hash256 root;

    public State(BigInteger nonce, Hash256 root) {
        this.nonce = nonce;
        this.root = root;
    }

    public static State newState() {
        return new State(BigInteger.ZERO, Hash256.ZERO);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        State state = (State) o;
        return nonce.equals(state.nonce) &&
                root.equals(state.root);
    }
}
