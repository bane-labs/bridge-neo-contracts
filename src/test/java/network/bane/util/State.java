package network.bane.util;

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

    public boolean equals(State other) {
        if (other == null) {
            return false;
        }
        if (this == other) {
            return true;
        }
        return this.nonce.equals(other.nonce) && this.root.equals(other.root);
    }
}
