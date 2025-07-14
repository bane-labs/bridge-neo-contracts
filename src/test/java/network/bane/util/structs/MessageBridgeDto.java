package network.bane.util.structs;

import io.neow3j.protocol.core.response.ContractManifest;

import java.math.BigInteger;

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

    public static class MessageConfig {
        public BigInteger sendingFee;
        public int maxMessageSizeForSending;
        public int maxNrMessagesForStoring;

        public MessageConfig(BigInteger sendingFee, int maxMessageSizeForSending, int maxNrMessagesForStoring) {
            this.sendingFee = sendingFee;
            this.maxMessageSizeForSending = maxMessageSizeForSending;
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
                    this.maxMessageSizeForSending == that.maxMessageSizeForSending &&
                    this.maxNrMessagesForStoring == that.maxNrMessagesForStoring;
        }
    }
}
