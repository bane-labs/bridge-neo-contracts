package network.bane.bridge;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Hash256;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.contracts.StdLib;
import network.bane.structs.State;
import network.bane.structs.message.MessageBridge;

import static io.neow3j.devpack.Helper.abort;
import static network.bane.bridge.StorageConstants.KEY_MESSAGE_BRIDGE;
import static network.bane.bridge.StorageConstants.KEY_MESSAGE_EXECUTOR;

public class MessageBridgeImpl {

    // region message bridge

    static boolean messageBridgeIsSet() {
        return BridgeContract.baseMap.get(KEY_MESSAGE_BRIDGE) != null;
    }

    static MessageBridge getMessageBridge() {
        ByteString serialized = BridgeContract.baseMap.get(KEY_MESSAGE_BRIDGE);
        if (serialized == null) abort("Message bridge not set");
        return (MessageBridge) new StdLib().deserialize(serialized);
    }

    static void setMessageBridge(Hash160 executionManager, int sendingFee, int maxMsgSizeForSending,
            int maxNrMsgsForStoring) {

        if (!new ContractManagement().isContract(executionManager)) abort("ExecutionManager must be a contract");
        BridgeContract.baseMap.put(KEY_MESSAGE_EXECUTOR, executionManager);

        ByteString zeroHash = Hash256.zero().toByteString();
        State initialState = new State(0, zeroHash);
        BridgeContract.baseMap.put(KEY_MESSAGE_BRIDGE,
                new StdLib().serialize(
                        new MessageBridge(true, initialState, initialState,
                                new MessageBridge.MessageConfig(sendingFee, maxMsgSizeForSending, maxNrMsgsForStoring)
                        )
                )
        );
    }

    // endregion

}
