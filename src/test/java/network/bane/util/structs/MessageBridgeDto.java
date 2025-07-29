package network.bane.util.structs;

import io.neow3j.types.Hash160;

import java.math.BigInteger;
import java.util.Objects;

public class MessageBridgeDto {
    public State evmToN3MessageState;
    public State n3ToEvmMessageState;
    public MessageConfigDto config;

    public MessageBridgeDto(State evmToN3MessageState, State n3ToEvmMessageState, MessageConfigDto config) {
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
        return this.evmToN3MessageState.equals(that.evmToN3MessageState) &&
                this.n3ToEvmMessageState.equals(that.n3ToEvmMessageState) &&
                this.config.equals(that.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(evmToN3MessageState, n3ToEvmMessageState, config);
    }

    public static class MessageConfigDto {
        public BigInteger sendingFee;
        public int maxBytesForSending;
        public int maxNrMessagesForStoring;
        public Hash160 executionManager;
        public int executionWindowMilliseconds;

        public MessageConfigDto(BigInteger sendingFee, int maxBytesForSending, int maxNrMessagesForStoring,
                Hash160 executionManager, int executionWindowMilliseconds) {
            this.sendingFee = sendingFee;
            this.maxBytesForSending = maxBytesForSending;
            this.maxNrMessagesForStoring = maxNrMessagesForStoring;
            this.executionManager = executionManager;
            this.executionWindowMilliseconds = executionWindowMilliseconds;
        }

        @Override
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            if (!(other instanceof MessageConfigDto)) {
                return false;
            }
            MessageConfigDto that = (MessageConfigDto) other;
            return this.sendingFee.equals(that.sendingFee) &&
                    this.maxBytesForSending == that.maxBytesForSending &&
                    this.maxNrMessagesForStoring == that.maxNrMessagesForStoring &&
                    this.executionManager.equals(that.executionManager) &&
                    this.executionWindowMilliseconds == that.executionWindowMilliseconds;
        }

        @Override
        public int hashCode() {
            return Objects.hash(sendingFee, maxBytesForSending, maxNrMessagesForStoring, executionManager,
                    executionWindowMilliseconds);
        }

    }

}
