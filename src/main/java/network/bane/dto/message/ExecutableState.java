package network.bane.dto.message;

import java.math.BigInteger;
import java.util.Objects;

public class ExecutableState {
    public Boolean executed;
    public BigInteger expirationTimestamp;

    public ExecutableState(boolean executed, BigInteger expirationTimestamp) {
        this.executed = executed;
        this.expirationTimestamp = expirationTimestamp;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof ExecutableState)) {
            return false;
        }
        ExecutableState that = (ExecutableState) other;
        return Objects.equals(this.executed, that.executed) &&
                Objects.equals(this.expirationTimestamp, that.expirationTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(executed, expirationTimestamp);
    }
}
