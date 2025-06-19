package network.bane.interfaces;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.annotations.CallFlags;
import io.neow3j.devpack.contracts.ContractInterface;
import network.bane.structs.message.Message;

public class MessageExecutor extends ContractInterface {

    public MessageExecutor(Hash160 contractHash) {
        super(contractHash);
    }

    @CallFlags(io.neow3j.devpack.constants.CallFlags.All)
    public native Hash160 executeMessage(Message message);
}
