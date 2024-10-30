package network.bane.bridge;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.contracts.ContractManagement;
import network.bane.interfaces.BridgeManagement;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Runtime.checkWitness;
import static network.bane.bridge.StorageConstants.KEY_BRIDGE_MANAGEMENT;

public class BridgeHelper {

    static void onlyWhenPaused() {
        if (!BridgeContract.isPaused()) abort("Contract is not paused.");
    }

    static void onlyWhenNotPaused() {
        if (BridgeContract.isPaused()) abort("Contract is paused.");
    }

    static void onlyRelayer() {
        if (!checkWitness(managementContract().relayer())) {
            abort("Only the relayer can call this method.");
        }
    }

    static void onlyGovernor() {
        if (!checkWitness(managementContract().governor())) {
            abort("Only the governor can call this method.");
        }
    }

    static void onlyGovernorOrSecurityGuard() {
        if (!checkWitness(managementContract().governor()) && !checkWitness(managementContract().securityGuard())) {
            abort("Only the governor or security guard can call this method.");
        }
    }

    static BridgeManagement managementContract() {
        return new BridgeManagement(BridgeContract.baseMap.getHash160(KEY_BRIDGE_MANAGEMENT));
    }

    static boolean isContract(Hash160 scriptHash) {
        return new ContractManagement().getContract(scriptHash) != null;
    }

}
