package network.bane.util;

import static io.neow3j.types.ContractParameter.*;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static java.util.Arrays.asList;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.InvocationResult;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.Signer;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.utils.Numeric;
import io.neow3j.wallet.Account;
import network.bane.util.helper.SmartContractHelper;
import network.bane.util.structs.GasBridge;
import network.bane.util.structs.State;
import network.bane.util.structs.TokenBridge;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static network.bane.util.TestHelper.owner;
import static network.bane.util.helper.TestHelper.bridge;

public class Nep17Token extends SmartContractHelper {

    public Nep17Token(Hash160 scriptHash, Neow3j neow3j) {
        super(scriptHash, neow3j);
    }

    public Hash256 transfer(Account sender,  Hash160 to, BigInteger amount) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("transfer", hash160(sender.getScriptHash()),  hash160(to),
                integer(amount), null).signers(signer));
    }

    public BigInteger balanceOf(Hash160 address) throws Throwable {
        return callInvokeFunction("balanceOf", asList(hash160(address)))
                .getInvocationResult().getFirstStackItem().getInteger();
    }
}
