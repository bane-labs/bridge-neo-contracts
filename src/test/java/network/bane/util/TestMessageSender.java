package network.bane.util;

import io.neow3j.protocol.Neow3j;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import network.bane.util.helper.SmartContractHelper;

import static io.neow3j.transaction.AccountSigner.global;
import static io.neow3j.types.ContractParameter.bool;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.utils.Numeric.hexStringToByteArray;
import static network.bane.util.helper.TestHelper.alice;
import static network.bane.util.helper.TestHelper.messageBridge;

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

    public Hash256 sendExecutableMessage(String rawMessageHex, boolean storeResult) throws Throwable {
        return sendExecutableMessage(hexStringToByteArray(rawMessageHex), storeResult);
    }

    public Hash256 sendExecutableMessage(AccountSigner signer, String rawMessageHex, boolean storeResult)
            throws Throwable {
        return sendExecutableMessage(signer, hexStringToByteArray(rawMessageHex), storeResult);
    }

    public Hash256 sendMessage(byte[] rawMessage) throws Throwable {
        return sendMessage(global(alice), rawMessage);
    }

    public Hash256 sendMessage(AccountSigner signer, byte[] rawMessage) throws Throwable {
        return sendAndAwaitExecution(
                invokeFunction("sendMessage", byteArray(rawMessage),
                        hash160(signer.getAccount()), integer(messageBridge.sendingFee())
                ).signers(signer)
        );
    }

    public Hash256 sendMessage(String rawMessageHex) throws Throwable {
        return sendMessage(hexStringToByteArray(rawMessageHex));
    }

    public Hash256 sendMessage(AccountSigner signer, String rawMessageHex) throws Throwable {
        return sendMessage(signer, hexStringToByteArray(rawMessageHex));
    }

    public void setMessageBridge(Hash160 messageBridge) throws Throwable {
        sendAndAwaitExecution(invokeFunction("setMessageBridge", hash160(messageBridge)).signers(global(alice)));
    }
}
