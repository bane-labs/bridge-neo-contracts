package network.bane.util.structs;

public enum ExecutionType {
    NEO(0),
    NEP17(1);

    private final int value;

    ExecutionType(int i) {
        this.value = i;
    }

    public int getValue() {
        return value;
    }
}
