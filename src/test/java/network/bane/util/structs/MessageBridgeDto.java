package network.bane.util.structs;

import io.neow3j.types.Hash160;

import java.math.BigInteger;
import java.util.Objects;

public class MessageBridgeDto {
    public network.bane.dto.State evmToN3MessageState;
    public network.bane.dto.State n3ToEvmMessageState;
    public MessageConfigDto config;

    public MessageBridgeDto(network.bane.dto.State evmToN3MessageState, network.bane.dto.State n3ToEvmMessageState,
            MessageConfigDto config) {
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
        public int maxMessageSize;
        public int maxNrMessages;
        public Hash160 executionManager;
        public int executionWindowMilliseconds;

        public MessageConfigDto(BigInteger sendingFee, int maxMessageSize, int maxNrMessages,
                Hash160 executionManager, int executionWindowMilliseconds) {
            this.sendingFee = sendingFee;
            this.maxMessageSize = maxMessageSize;
            this.maxNrMessages = maxNrMessages;
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
                    this.maxMessageSize == that.maxMessageSize &&
                    this.maxNrMessages == that.maxNrMessages &&
                    this.executionManager.equals(that.executionManager) &&
                    this.executionWindowMilliseconds == that.executionWindowMilliseconds;
        }

        @Override
        public int hashCode() {
            return Objects.hash(sendingFee, maxMessageSize, maxNrMessages, executionManager,
                    executionWindowMilliseconds);
        }

    }

}
