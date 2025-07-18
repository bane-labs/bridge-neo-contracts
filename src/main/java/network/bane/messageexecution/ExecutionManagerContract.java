package network.bane.messageexecution;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Contract;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Storage;
import io.neow3j.devpack.StorageContext;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.annotations.DisplayName;
import io.neow3j.devpack.annotations.ManifestExtra;
import io.neow3j.devpack.annotations.OnDeployment;
import io.neow3j.devpack.annotations.Permission;
import io.neow3j.devpack.annotations.Safe;
import io.neow3j.devpack.annotations.Struct;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.contracts.StdLib;
import network.bane.interfaces.BridgeManagement;
import network.bane.structs.message.N3MethodCall;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Runtime.checkWitness;
import static io.neow3j.devpack.Runtime.getExecutingScriptHash;

@DisplayName("ExecutionManager")
@Permission(contract = "*")
@ManifestExtra.ManifestExtras({@ManifestExtra(key = "Author", value = "BaneLabs"),
        @ManifestExtra(key = "Description",
                       value = "Contract for managing the execution of messages from the Neo X bridge contract."),
        @ManifestExtra(key = "Source", value = "https://github.com/bane-labs/bridge-neo-contracts")})
public class ExecutionManagerContract {

    // region constants

    private static final byte PREFIX_BASE = 0x0a;

    private static final int KEY_BRIDGE_MANAGEMENT = 0x01;
    private static final int KEY_BRIDGE = 0x02;
    private static final int KEY_PAUSE = 0x03;

    private static final int KEY_ENTERED = 0x70;
    private static final int KEY_EXECUTING_NONCE = 0x71;

    private static final int KEY_VERSION = 0x7f;

    // endregion
    // region storage

    static final StorageContext ctx = Storage.getStorageContext();
    static final StorageMap baseMap = new StorageMap(ctx, PREFIX_BASE);

    // endregion
    // region pause

    @Safe
    public static boolean isPaused() {
        return baseMap.getBoolean(KEY_PAUSE);
    }

    public static void pause() {
        onlyGovernorOrSecurityGuard();
        onlyWhenNotPaused();
        baseMap.put(KEY_PAUSE, true);
    }

    public static void unpause() {
        onlyGovernor();
        onlyWhenPaused();
        baseMap.put(KEY_PAUSE, false);
    }

    // endregion
    // region execution

    @Safe
    public static int getExecutingNonce() {
        return baseMap.getIntOrZero(KEY_EXECUTING_NONCE);
    }

    private static void setExecutingNonce(int nonce) {
        baseMap.put(KEY_EXECUTING_NONCE, nonce);
    }

    private static void unsetExecutingNonce() {
        baseMap.delete(KEY_EXECUTING_NONCE);
    }

    public static Object executeMessage(int nonce, ByteString executableCode) {
        onlyBridge();

        enteringNonReentrant();
        setExecutingNonce(nonce);

        N3MethodCall call = (N3MethodCall) new StdLib().deserialize(executableCode);
        if (!N3MethodCall.isValid(call)) abort("Method call has invalid values");
        if (getExecutingScriptHash().equals(call.target)) abort("Prohibited target");

        // Todo: Consider returning the result. This way the bridge could decide what to do with it.
        Object result = Contract.call(call.target, call.method, call.callFlags, call.args);

        unsetExecutingNonce();
        exitingNonReentrant();
        return result;
    }

    // endregion
    // region bridge/management

    @Safe
    public static Hash160 bridgeManagement() {
        return baseMap.getHash160(KEY_BRIDGE_MANAGEMENT);
    }

    private static BridgeManagement managementContract() {
        return new BridgeManagement(bridgeManagement());
    }

    @Safe
    public static Hash160 bridge() {
        return baseMap.getHash160(KEY_BRIDGE);
    }

    // endregion
    // region deploy, update

    @Struct
    private static class MessageExecutorDeploymentData {
        Hash160 management;
        Hash160 bridge;

        public static boolean isValid(MessageExecutorDeploymentData data) {
            return data.management != null && Hash160.isValid(data.management) && !data.management.isZero() &&
                    data.bridge != null && Hash160.isValid(data.bridge) && !data.bridge.isZero();
        }
    }

    @OnDeployment
    public static void deploy(Object data, boolean update) {
        if (!update) {
            MessageExecutorDeploymentData deploymentData = (MessageExecutorDeploymentData) data;
            if (!MessageExecutorDeploymentData.isValid(deploymentData)) {
                abort("Invalid deployment data for MessageExecutor.");
            }
            baseMap.put(KEY_VERSION, 4); // Using the release version of the bridge-neo-contracts repository.
            baseMap.put(KEY_BRIDGE_MANAGEMENT, deploymentData.management);
            baseMap.put(KEY_BRIDGE, deploymentData.bridge);
            baseMap.put(KEY_PAUSE, false);
        }
    }

    public static void update(ByteString nef, String manifest, Object data) {
        onlyOwner();
        onlyWhenPaused();
        new ContractManagement().update(nef, manifest, data);
    }

    // endregion
    // region modifiers

    private static boolean entered() {
        return baseMap.getBoolean(KEY_ENTERED);
    }

    private static void enteringNonReentrant() {
        if (entered()) abort("Reentrancy detected");
        baseMap.put(KEY_ENTERED, true);
    }

    private static void exitingNonReentrant() {
        baseMap.put(KEY_ENTERED, false);
    }

    private static void onlyWhenPaused() {
        if (!isPaused()) abort("Contract not paused");
    }

    private static void onlyWhenNotPaused() {
        if (isPaused()) abort("Contract paused");
    }

    private static void onlyBridge() {
        if (!checkWitness(bridge())) {
            abort("No authorization - only bridge");
        }
    }

    private static void onlyOwner() {
        if (!checkWitness(managementContract().owner())) {
            abort("No authorization - only owner");
        }
    }

    private static void onlyGovernor() {
        if (!checkWitness(managementContract().governor())) {
            abort("No authorization - only governor");
        }
    }

    private static void onlyGovernorOrSecurityGuard() {
        BridgeManagement managementContract = managementContract();
        if (!checkWitness(managementContract.governor()) && !checkWitness(managementContract.securityGuard())) {
            abort("No authorization - only governor or security guard");
        }
    }

    // endregion

}
