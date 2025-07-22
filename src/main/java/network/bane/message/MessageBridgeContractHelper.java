package network.bane.message;

import network.bane.interfaces.BridgeManagement;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Runtime.checkWitness;
import static network.bane.message.StorageConstants.KEY_BRIDGE_MANAGEMENT;

class MessageBridgeContractHelper {

    // region state based modifiers

    static void onlyWhenPaused() {
        if (!MessageBridgeContract.isPaused()) abort("Contract not paused");
    }

    static void onlyWhenNotPaused() {
        if (MessageBridgeContract.isPaused()) abort("Contract paused");
    }

    static void onlyWhenSendAndExecutePaused() {
        if (!MessageBridgeContract.sendAndExecuteIsPaused()) abort("Send and execute not paused");
    }

    static void onlyWhenSendAndExecuteNotPaused() {
        if (MessageBridgeContract.sendAndExecuteIsPaused()) abort("Send and execute paused");
    }

    // endregion
    // region permission modifiers

    static void onlyRelayer() {
        if (!checkWitness(managementContract().relayer())) {
            abort("No authorization - only relayer");
        }
    }

    static void onlyGovernor() {
        if (!checkWitness(managementContract().governor())) {
            abort("No authorization - only governor");
        }
    }

    static void onlyGovernorOrSecurityGuard() {
        if (!checkWitness(managementContract().governor()) && ! checkWitness(managementContract().securityGuard())) {
            abort("No authorization - only governor or security guard");
        }
    }

    // endregion

    static BridgeManagement managementContract() {
        return new BridgeManagement(MessageBridgeContract.baseMap.getHash160(KEY_BRIDGE_MANAGEMENT));
    }

}
