package network.bane.testhelper;

import io.neow3j.devpack.ByteString;

import static io.neow3j.devpack.Helper.abort;

public class DummyExecutionManagerContract {

    public static void executeMessage(int nonce, ByteString executableCode) {
        abort("I'm only a dummy contract for testing purposes.");
    }

}
