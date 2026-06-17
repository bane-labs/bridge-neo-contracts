package network.bane.support.contract;

import io.neow3j.protocol.Neow3j;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;

import java.math.BigInteger;

import static io.neow3j.transaction.AccountSigner.global;
import static io.neow3j.types.ContractParameter.bool;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static network.bane.support.TestEnvironment.alice;
import static network.bane.support.TestEnvironment.messageBridge;

public class TestMessageSender extends SmartContractHelper {

    public TestMessageSender(Hash160 scriptHash, Neow3j neow3j) {
        super(scriptHash, neow3j);
    }

    public Hash256 sendExecutableMessage(byte[] rawMessage, boolean storeResult) throws Throwable {
        return sendExecutableMessage(global(alice), rawMessage, storeResult);
    }

    public Hash256 sendExecutableMessage(AccountSigner signer, byte[] rawMessage, boolean storeResult)
            throws Throwable {
        return sendAndAwaitExecution(
                invokeFunction("sendExecutableMessage", byteArray(rawMessage), bool(storeResult),
                        hash160(signer.getAccount()), integer(messageBridge.sendingFee())
                ).signers(signer)
        );
    }

    public Hash256 sendStoreOnlyMessage(byte[] rawMessage) throws Throwable {
        return sendStoreOnlyMessage(global(alice), rawMessage);
    }

    public Hash256 sendStoreOnlyMessage(AccountSigner signer, byte[] rawMessage) throws Throwable {
        return sendAndAwaitExecution(
                invokeFunction("sendStoreOnlyMessage", byteArray(rawMessage),
                        hash160(signer.getAccount()), integer(messageBridge.sendingFee())
                ).signers(signer)
        );
    }

    public Hash256 sendResultMessage(BigInteger relatedMessageNonce) throws Throwable {
        return sendResultMessage(global(alice), relatedMessageNonce);
    }

    public Hash256 sendResultMessage(AccountSigner signer, BigInteger relatedMessageNonce) throws Throwable {
        return sendAndAwaitExecution(
                invokeFunction("sendResultMessage", integer(relatedMessageNonce),
                        hash160(signer.getAccount()), integer(messageBridge.sendingFee())
                ).signers(signer)
        );
    }

    public void setMessageBridge(Hash160 messageBridge) throws Throwable {
        sendAndAwaitExecution(invokeFunction("setMessageBridge", hash160(messageBridge)).signers(global(alice)));
    }
}
