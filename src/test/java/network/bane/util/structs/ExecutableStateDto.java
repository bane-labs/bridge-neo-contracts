package network.bane.util.structs;

import java.math.BigInteger;
import java.util.Objects;

public class ExecutableStateDto {
    public Boolean executed;
    public BigInteger expirationTimestamp;

    public ExecutableStateDto(boolean executed, BigInteger expirationTimestamp) {
        this.executed = executed;
        this.expirationTimestamp = expirationTimestamp;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof ExecutableStateDto)) {
            return false;
        }
        ExecutableStateDto that = (ExecutableStateDto) other;
        return Objects.equals(this.executed, that.executed) &&
                Objects.equals(this.expirationTimestamp, that.expirationTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(executed, expirationTimestamp);
    }
}
