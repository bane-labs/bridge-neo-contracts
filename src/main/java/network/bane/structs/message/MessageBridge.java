package network.bane.structs.message;

import network.bane.structs.State;

public class MessageBridge {
    public boolean paused;
    public State evmToN3MessageState;
    public State n3ToEvmMessageState;
    public MessageConfig config;

    public MessageBridge(boolean paused, State evmToN3MessageState, State n3ToEvmMessageState, MessageConfig config) {
        this.paused = paused;
        this.evmToN3MessageState = evmToN3MessageState;
        this.n3ToEvmMessageState = n3ToEvmMessageState;
        this.config = config;
    }

    public static boolean isValid(MessageBridge messageBridge) {
        return messageBridge != null &&
                State.isValid(messageBridge.evmToN3MessageState) &&
                State.isValid(messageBridge.n3ToEvmMessageState) &&
                MessageConfig.isValid(messageBridge.config);
    }

    static public class MessageConfig {
        /**
         * The fee that is charged for each message.
         */
        public int messageFee;
        // Todo: Consider using a fee factor instead of a fixed fee, to allow for dynamic fee adjustments based on
        //  message size.

        // Todo: Consider other configurations, such as maximum message size, etc.

        public MessageConfig(int messageFee) {
            this.messageFee = messageFee;
        }

        public static boolean isValid(MessageConfig config) {
            return config != null && config.messageFee >= 0;
        }
    }
}
