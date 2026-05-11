package network.bane.utils.structs;

import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.types.Hash160;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

public class MessageBridge {
    public State evmToNeoState;
    public State neoToEvmState;
    public MessageBridgeConfig config;

    public MessageBridge(State evmToNeoState, State neoToEvmState, MessageBridgeConfig config) {
        this.evmToNeoState = evmToNeoState;
        this.neoToEvmState = neoToEvmState;
        this.config = config;
    }

    public static MessageBridge fromStackItem(StackItem item) {
        List<StackItem> list = item.getList();
        State evmToNeoState = State.fromStackItem(list.get(0));
        State neoToEvmState = State.fromStackItem(list.get(1));
        MessageBridgeConfig config = MessageBridgeConfig.fromStackItem(list.get(2));
        return new MessageBridge(evmToNeoState, neoToEvmState, config);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MessageBridge that = (MessageBridge) o;
        return Objects.equals(evmToNeoState, that.evmToNeoState) &&
                Objects.equals(neoToEvmState, that.neoToEvmState) &&
                Objects.equals(config, that.config);
    }

    public static class MessageBridgeConfig {
        public BigInteger fee;
        public int maxMessageSize;
        public int maxNrMessages;
        public Hash160 executionManager;
        public long executionWindowMilliseconds;

        public MessageBridgeConfig(BigInteger fee, int maxMessageSize, int maxNrMessages, Hash160 executionManager,
                long executionWindowMilliseconds) {
            this.fee = fee;
            this.maxMessageSize = maxMessageSize;
            this.maxNrMessages = maxNrMessages;
            this.executionManager = executionManager;
            this.executionWindowMilliseconds = executionWindowMilliseconds;
        }

        public static MessageBridgeConfig fromStackItem(StackItem item) {
            List<StackItem> list = item.getList();
            BigInteger fee = list.get(0).getInteger();
            int maxMessageSize = list.get(1).getInteger().intValue();
            int maxNrMessages = list.get(2).getInteger().intValue();
            Hash160 executionManager = Hash160.fromAddress(list.get(3).getAddress());
            long executionWindowMilliseconds = list.get(4).getInteger().longValue();
            return new MessageBridgeConfig(fee, maxMessageSize, maxNrMessages, executionManager,
                    executionWindowMilliseconds);
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MessageBridgeConfig that = (MessageBridgeConfig) o;
            return Objects.equals(fee, that.fee) &&
                    maxMessageSize == that.maxMessageSize &&
                    maxNrMessages == that.maxNrMessages &&
                    Objects.equals(executionManager, that.executionManager) &&
                    Objects.equals(executionWindowMilliseconds, that.executionWindowMilliseconds);
        }

        @Override
        public String toString() {
            return "MessageBridgeConfig{" +
                    "fee=" + fee +
                    ", maxMessageSize=" + maxMessageSize +
                    ", maxNrMessages=" + maxNrMessages +
                    ", executionManager=" + executionManager +
                    ", executionWindowMilliseconds=" + executionWindowMilliseconds +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "MessageBridge{" +
                "evmToNeoState=" + evmToNeoState +
                "neoToEvmState=" + neoToEvmState +
                "config=" + config +
                '}';
    }

}
