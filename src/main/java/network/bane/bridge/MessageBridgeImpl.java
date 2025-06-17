package network.bane.bridge;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Hash160;

import static io.neow3j.devpack.Helper.abort;

public class MessageBridgeImpl {

    // region pausing

    static void onlyWhenMessageBridgePaused() {
        abort("Not implemented yet");
    }

    static void onlyWhenMessageBridgeNotPaused() {
        abort("Not implemented yet");
    }

    static void pauseMessageBridge() {
        abort("Not implemented yet");
    }

    static void unpauseMessageBridge() {
        abort("Not implemented yet");
    }

    // endregion
    // region message sending

    static void sendMessage(ByteString msgCall) {
        abort("Not implemented yet");
    }

    // endregion
    // region message storing

    static void storeMessage(int timestamp, Hash160 msgSender, ByteString serializedInvocation) {
        abort("Not implemented yet");
    }

    // endregion
    // region message execution

    static void executeMessage(int messageId) {
        abort("Not implemented yet");
    }

    // endregion

}
