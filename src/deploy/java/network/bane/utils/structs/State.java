package network.bane.utils.structs;

import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.types.Hash256;

import java.math.BigInteger;
import java.util.List;

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

    public static State fromStackItem(StackItem item) {
        List<StackItem> list = item.getList();
        BigInteger nonce = list.get(0).getInteger();
        Hash256 root = new Hash256(list.get(1).getByteArray());
        return new State(nonce, root);
    }

    @Override
    public String toString() {
        return "State{" +
                "nonce=" + nonce +
                ", root=" + root +
                '}';
    }
}
