package network.bane.client;

import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.Signer;
import io.neow3j.transaction.TransactionBuilder;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.types.ContractParameter.any;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;

class BridgeClientBase extends SmartContract {

    BridgeClientBase(Hash160 scriptHash, Neow3j neow3j) {
        super(scriptHash, neow3j);
    }

    Hash256 invokeWrite(Account sender, String function, ContractParameter... params) throws Throwable {
        AccountSigner signer = calledByEntry(sender);
        TransactionBuilder tx = invokeFunction(function, params).signers(signer);
        return sendAndAwaitExecution(tx);
    }

    Hash256 invokeWriteWithSigners(String function, ContractParameter[] params, Signer... signers) throws Throwable {
        TransactionBuilder tx = invokeFunction(function, params).signers(signers);
        return sendAndAwaitExecution(tx);
    }

    StackItem invokeReadFirstStackItem(String function, ContractParameter... params) throws IOException {
        return callInvokeFunction(function, toParamsList(params)).getInvocationResult().getFirstStackItem();
    }

    Hash256 update(Account sender, byte[] nefBytes, String manifestJson, Object data) throws Throwable {
        AccountSigner signer = calledByEntry(sender);
        ContractParameter dataParameter = data == null ? any(null) : any(data);
        TransactionBuilder tx = invokeFunction("update", byteArray(nefBytes),
                byteArray(manifestJson.getBytes(StandardCharsets.UTF_8)), dataParameter).signers(signer);
        return sendAndAwaitExecution(tx);
    }

    private List<ContractParameter> toParamsList(ContractParameter... params) {
        ArrayList<ContractParameter> list = new ArrayList<>();
        if (params == null) {
            return list;
        }
        Collections.addAll(list, params);
        return list;
    }

    private Hash256 sendAndAwaitExecution(TransactionBuilder txBuilder) throws Throwable {
        NeoSendRawTransaction response = txBuilder.sign().send();
        if (response.hasError()) {
            throw new Exception("Error sending transaction: " + response.getError().getMessage());
        }
        Hash256 txHash = response.getSendRawTransaction().getHash();
        waitUntilTransactionIsExecuted(txHash, neow3j);
        return txHash;
    }
}
