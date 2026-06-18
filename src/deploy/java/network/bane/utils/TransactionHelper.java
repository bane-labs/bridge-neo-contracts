package network.bane.utils;

import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoApplicationLog;
import io.neow3j.types.Hash256;
import io.neow3j.types.NeoVMStateType;

import java.io.IOException;

public class TransactionHelper {

    public static void validateTransactionHalted(Neow3j neow3j, Hash256 txHash) throws IOException {
        NeoApplicationLog.Execution execution = neow3j.getApplicationLog(txHash).send().getApplicationLog()
                .getFirstExecution();
        if (!NeoVMStateType.HALT.equals(execution.getState())) {
            throw new IllegalStateException(
                    "Transaction failed with state: " + execution.getState() + " and exception: " +
                            execution.getException());
        }
    }

}
