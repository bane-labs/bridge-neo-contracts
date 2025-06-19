package network.bane.bridge;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.contracts.ContractManagement;
import network.bane.interfaces.BridgeManagement;
import network.bane.interfaces.MessageExecutor;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Runtime.checkWitness;
import static network.bane.bridge.StorageConstants.KEY_BRIDGE_MANAGEMENT;
import static network.bane.bridge.StorageConstants.KEY_MESSAGE_EXECUTOR;

public class BridgeHelper {

    static void onlyWhenPaused() {
        if (!BridgeContract.isPaused()) abort("Contract not paused");
    }

    static void onlyWhenNotPaused() {
        if (BridgeContract.isPaused()) abort("Contract paused");
    }

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
        if (!checkWitness(managementContract().governor()) && !checkWitness(managementContract().securityGuard())) {
            abort("No authorization - only governor or security guard");
        }
    }

    static BridgeManagement managementContract() {
        return new BridgeManagement(BridgeContract.baseMap.getHash160(KEY_BRIDGE_MANAGEMENT));
    }

    static MessageExecutor messageExecutorContract() {
        return new MessageExecutor(BridgeContract.baseMap.getHash160(KEY_MESSAGE_EXECUTOR));
    }

    static boolean isContract(Hash160 scriptHash) {
        return new ContractManagement().getContract(scriptHash) != null;
    }

}
