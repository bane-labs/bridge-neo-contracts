package network.bane.structs.message;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.annotations.Struct;
import network.bane.structs.State;

@Struct
public class MessageBridge {
    public State evmToN3MessageState;
    public State n3ToEvmMessageState;
    public MessageBridgeConfig config;

    public MessageBridge(State evmToN3MessageState, State n3ToEvmMessageState, MessageBridgeConfig config) {
        this.evmToN3MessageState = evmToN3MessageState;
        this.n3ToEvmMessageState = n3ToEvmMessageState;
        this.config = config;
    }

    public static boolean isValid(MessageBridge messageBridge) {
        return messageBridge != null &&
                State.isValid(messageBridge.evmToN3MessageState) &&
                State.isValid(messageBridge.n3ToEvmMessageState) &&
                MessageBridgeConfig.isValid(messageBridge.config);
    }

    @Struct
    static public class MessageBridgeConfig {
        /**
         * The fee that is charged for each message sent from N3 to EVM.
         */
        public int sendingFee;

        /**
         * The maximum size (in bytes) of a message that can be sent from N3 to EVM.
         */
        public int maxBytesForSending;

        /**
         * The maximum number of messages that can be batched together by the relayer in a single invocation of
         * {@code BridgeContract#storeMessages()}.
         */
        public int maxNrMessagesForStoring;

        /**
         * The address of the contract that manages the execution of messages.
         */
        public Hash160 executionManager;

        /**
         * The time window (in seconds) during which a message can be executed after it has been stored on-chain.
         */
        public int executionWindowMilliseconds;

        public MessageBridgeConfig(int sendingFee, int maxBytesForSending, int maxNrMessagesForStoring,
                Hash160 executionManager, int executionWindowMilliseconds) {
            this.sendingFee = sendingFee;
            this.maxBytesForSending = maxBytesForSending;
            this.maxNrMessagesForStoring = maxNrMessagesForStoring;
            this.executionManager = executionManager;
            this.executionWindowMilliseconds = executionWindowMilliseconds;
        }

        public static boolean isValid(MessageBridgeConfig config) {
            return config != null &&
                    config.sendingFee >= 0 &&
                    config.maxBytesForSending > 0 &&
                    config.maxNrMessagesForStoring > 0 &&
                    config.executionManager != null && !config.executionManager.isZero() &&
                    Hash160.isValid(config.executionManager) &&
                    config.executionWindowMilliseconds > 0;
        }

    }

}
