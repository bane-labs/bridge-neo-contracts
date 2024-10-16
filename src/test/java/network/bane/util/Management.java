package network.bane.util;

import io.neow3j.crypto.ECKeyPair;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;
import network.bane.util.helper.SmartContractHelper;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.types.ContractParameter.bool;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.types.ContractParameter.publicKey;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;

public class Management extends SmartContractHelper {

    public Management(Hash160 scriptHash, Neow3j neow3j) {
        super(scriptHash, neow3j);
    }

    public Hash160 owner() throws IOException {
        return callFunctionReturningScriptHash("owner");
    }

    public Hash160 relayer() throws IOException {
        return callFunctionReturningScriptHash("relayer");
    }

    public void addValidator(Account signer, ECKeyPair.ECPublicKey pubKey, boolean incrementThreshold) throws Throwable {
        Hash256 txHash = invokeFunction("addValidator", publicKey(pubKey), bool(incrementThreshold))
                .signers(calledByEntry(signer))
                .sign()
                .send()
                .getSendRawTransaction()
                .getHash();
        waitUntilTransactionIsExecuted(txHash, neow3j);
    }

    public void removeValidator(Account signer, ECKeyPair.ECPublicKey pubKey, boolean decrementThreshold) throws Throwable {
        Hash256 txHash = invokeFunction("removeValidator", publicKey(pubKey), bool(decrementThreshold))
                .signers(calledByEntry(signer))
                .sign()
                .send()
                .getSendRawTransaction()
                .getHash();
        waitUntilTransactionIsExecuted(txHash, neow3j);
    }

    public void replaceValidator(Account signer, ECKeyPair.ECPublicKey oldValidator, ECKeyPair.ECPublicKey newValidator) throws Throwable {
        Hash256 txHash = invokeFunction("replaceValidator", publicKey(oldValidator), publicKey(newValidator))
                .signers(calledByEntry(signer))
                .sign()
                .send()
                .getSendRawTransaction()
                .getHash();
        waitUntilTransactionIsExecuted(txHash, neow3j);
    }

    public boolean isValidator(ECKeyPair.ECPublicKey pubKey) throws IOException {
        return callFunctionReturningBool("isValidator", publicKey(pubKey));
    }

    public void setValidatorThreshold(Account signer, int threshold) throws Throwable {
        Hash256 txHash = invokeFunction("setValidatorThreshold", integer(threshold))
                .signers(calledByEntry(signer))
                .sign()
                .send()
                .getSendRawTransaction()
                .getHash();
        waitUntilTransactionIsExecuted(txHash, neow3j);
    }

    public List<ECKeyPair.ECPublicKey> validators() throws IOException {
        return callInvokeFunction("validators")
                .getInvocationResult()
                .getFirstStackItem()
                .getList()
                .stream()
                .map(StackItem::getByteArray)
                .map(ECKeyPair.ECPublicKey::new)
                .collect(Collectors.toList());
    }

    public int validatorThreshold() throws IOException {
        return callInvokeFunction("validatorThreshold").getInvocationResult().getFirstStackItem().getInteger().intValue();
    }

    public Hash160 governor() throws IOException {
        return callFunctionReturningScriptHash("governor");
    }

    public Hash160 securityGuard() throws IOException {
        return callFunctionReturningScriptHash("securityGuard");
    }

}
