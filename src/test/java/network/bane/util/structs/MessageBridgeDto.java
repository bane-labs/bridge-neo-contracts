package network.bane.util.structs;

import java.math.BigInteger;
import java.util.Objects;

public class MessageBridgeDto {
    public boolean paused;
    public State evmToN3MessageState;
    public State n3ToEvmMessageState;
    public MessageConfig config;

    public MessageBridgeDto(boolean paused, State evmToN3MessageState, State n3ToEvmMessageState, MessageConfig config) {
        this.paused = paused;
        this.evmToN3MessageState = evmToN3MessageState;
        this.n3ToEvmMessageState = n3ToEvmMessageState;
        this.config = config;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof MessageBridgeDto)) {
            return false;
        }
        MessageBridgeDto that = (MessageBridgeDto) other;
        return this.paused == that.paused &&
                this.evmToN3MessageState.equals(that.evmToN3MessageState) &&
                this.n3ToEvmMessageState.equals(that.n3ToEvmMessageState) &&
                this.config.equals(that.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paused, evmToN3MessageState, n3ToEvmMessageState, config);
    }

    public static class MessageConfig {
        public BigInteger sendingFee;
        public int maxBytesForSending;
        public int maxNrMessagesForStoring;

        public MessageConfig(BigInteger sendingFee, int maxBytesForSending, int maxNrMessagesForStoring) {
            this.sendingFee = sendingFee;
            this.maxBytesForSending = maxBytesForSending;
            this.maxNrMessagesForStoring = maxNrMessagesForStoring;
        }

        @Override
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            if (!(other instanceof MessageConfig)) {
                return false;
            }
            MessageConfig that = (MessageConfig) other;
            return this.sendingFee.equals(that.sendingFee) &&
                    this.maxBytesForSending == that.maxBytesForSending &&
                    this.maxNrMessagesForStoring == that.maxNrMessagesForStoring;
        }

        @Override
        public int hashCode() {
            return Objects.hash(sendingFee, maxBytesForSending, maxNrMessagesForStoring);
        }

    }

}
