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
    private static final int KEY_MESSAGE_BRIDGE = 0x02;
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
        if (isProhibitedForMessageExecution(call.target)) abort("Prohibited target");

        Object result = Contract.call(call.target, call.method, call.callFlags, call.args);

        unsetExecutingNonce();
        exitingNonReentrant();
        return result;
    }

    /**
     * Creates a serialized method call that can be passed to the {@link #executeMessage(int, ByteString)} method.
     *
     * @param target the target contract.
     * @param method the method to call.
     * @param callFlags the call flags to use.
     * @param args the arguments to pass to the method.
     * @return the serialized method call.
     */
    @Safe
    public static ByteString serializeCall(Hash160 target, String method, byte callFlags, Object[] args)
            throws Exception {
        N3MethodCall call = new N3MethodCall(target, method, callFlags, args);
        if (!N3MethodCall.isValid(call)) throw new Exception("Method call is invalid");
        return new StdLib().serialize(call);
    }

    /**
     * Checks whether the given bytes represent a valid serialized {@link N3MethodCall}.
     *
     * @param serializedCall the serialized call.
     * @return true if the call is valid, false otherwise.
     */
    @Safe
    public static boolean isValidCall(ByteString serializedCall) {
        N3MethodCall call = (N3MethodCall) new StdLib().deserialize(serializedCall);
        return N3MethodCall.isValid(call);
    }

    /**
     * Checks whether the given serialized call is allowed to be executed in a message execution context.
     *
     * @param serializedCall the serialized call.
     * @return true if the call is allowed, false otherwise.
     * @throws Exception if the call is invalid.
     */
    @Safe
    public static boolean isAllowedCall(ByteString serializedCall) throws Exception {
        N3MethodCall call = (N3MethodCall) new StdLib().deserialize(serializedCall);
        if (!N3MethodCall.isValid(call)) throw new Exception("Invalid call");
        return !isProhibitedForMessageExecution(call.target);
    }

    private static boolean isProhibitedForMessageExecution(Hash160 target) {
        Hash160 executingScriptHash = getExecutingScriptHash();
        // Disallow calling itself or the native contract management in a message execution call.
        return executingScriptHash.equals(target) || new ContractManagement().getHash().equals(target);
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
    public static Hash160 messageBridge() {
        return baseMap.getHash160(KEY_MESSAGE_BRIDGE);
    }

    // endregion
    // region deploy, update

    @Struct
    private static class MessageExecutorDeploymentData {
        Hash160 management;
        Hash160 messageBridge;

        public static boolean isValid(MessageExecutorDeploymentData data) {
            return data != null &&
                    data.management != null && Hash160.isValid(data.management) && !data.management.isZero() &&
                    data.messageBridge != null && Hash160.isValid(data.messageBridge) && !data.messageBridge.isZero();
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
            baseMap.put(KEY_MESSAGE_BRIDGE, deploymentData.messageBridge);
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
        if (!checkWitness(messageBridge())) {
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
