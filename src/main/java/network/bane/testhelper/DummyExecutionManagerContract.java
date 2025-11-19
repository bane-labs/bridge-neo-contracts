package network.bane.testhelper;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.annotations.DisplayName;

import static io.neow3j.devpack.Helper.abort;

@DisplayName("ExecutionManager")
public class DummyExecutionManagerContract {

    public static void executeMessage(int nonce, ByteString executableCode) {
        abort("I'm only a dummy contract for testing purposes.");
    }

}
