package network.bane.support.contract;

import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.transaction.TransactionBuilder;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;

import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;

public class SmartContractHelper extends SmartContract {

    public SmartContractHelper(Hash160 scriptHash, Neow3j neow3j) {
        super(scriptHash, neow3j);
    }

    public Hash256 sendAndAwaitExecution(TransactionBuilder b) throws Throwable {
        NeoSendRawTransaction response = b.sign().send();
        Hash256 txHash = response.getSendRawTransaction().getHash();
        waitUntilTransactionIsExecuted(txHash, neow3j);
        return txHash;
    }

}
