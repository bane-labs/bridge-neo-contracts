package network.bane.testhelper;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Storage;
import io.neow3j.devpack.annotations.CallFlags;
import io.neow3j.devpack.annotations.Permission;
import io.neow3j.devpack.contracts.ContractInterface;

@Permission(contract = "*")
public class TestMessageSenderContract {

    private static final byte KEY_MESSAGE_BRIDGE = 0x01;

    public static void setMessageBridge(Hash160 messageBridge) {
        Storage.put(Storage.getStorageContext(), KEY_MESSAGE_BRIDGE, messageBridge);
    }

    private static IMessageBridge getMessageBridge() {
        return new IMessageBridge(Storage.getHash160(Storage.getStorageContext(), KEY_MESSAGE_BRIDGE));
    }

    public static int sendMessage(ByteString rawMessage, Hash160 feeSponsor, int maxFee) {
        return getMessageBridge().sendMessage(rawMessage, feeSponsor, maxFee);
    }

    public static int sendExecutableMessage(ByteString rawMessage, boolean storeResult, Hash160 feeSponsor,
            int maxFee) {
        return getMessageBridge().sendExecutableMessage(rawMessage, storeResult, feeSponsor, maxFee);
    }

    public static int sendResultMessage(int relatedMessageNonce, Hash160 feeSponsor, int maxFee) {
        return getMessageBridge().sendResultMessage(relatedMessageNonce, feeSponsor, maxFee);
    }

    private static class IMessageBridge extends ContractInterface {
        public IMessageBridge(Hash160 contractHash) {
            super(contractHash);
        }

        @CallFlags(io.neow3j.devpack.constants.CallFlags.All)
        public native int sendMessage(ByteString rawMessage, Hash160 feeSponsor, int maxFee);

        @CallFlags(io.neow3j.devpack.constants.CallFlags.All)
        public native int sendExecutableMessage(ByteString rawMessage, boolean storeResult, Hash160 feeSponsor,
                int maxFee);

        @CallFlags(io.neow3j.devpack.constants.CallFlags.All)
        public native int sendResultMessage(int relatedMessageNonce, Hash160 feeSponsor, int maxFee);
    }

}
