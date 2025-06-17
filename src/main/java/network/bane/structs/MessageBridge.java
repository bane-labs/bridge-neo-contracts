package network.bane.structs;

public class MessageBridge {
    public boolean paused;
    public State evmMessageState;
    public State n3MessageState;
    public MessageConfig config;

    public MessageBridge(boolean paused, State evmMessageState, State n3MessageState, MessageConfig config) {
        this.paused = paused;
        this.evmMessageState = evmMessageState;
        this.n3MessageState = n3MessageState;
        this.config = config;
    }

    public static boolean isValid(MessageBridge messageBridge) {
        return messageBridge != null &&
                State.isValid(messageBridge.evmMessageState) &&
                State.isValid(messageBridge.n3MessageState) &&
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
