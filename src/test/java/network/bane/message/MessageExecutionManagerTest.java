package network.bane.message;

import io.neow3j.contract.ContractManagement;
import io.neow3j.contract.NefFile;
import io.neow3j.protocol.ObjectMapperFactory;
import io.neow3j.protocol.core.response.ContractManifest;
import io.neow3j.protocol.core.response.NeoApplicationLog;
import io.neow3j.protocol.core.response.Notification;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.serialization.exceptions.DeserializationException;
import io.neow3j.test.ContractTest;
import io.neow3j.test.ContractTestExtension;
import io.neow3j.test.DeployConfig;
import io.neow3j.test.DeployConfiguration;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.types.CallFlags;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.types.StackItemType;
import network.bane.management.BridgeManagementContract;
import network.bane.messageexecution.ExecutionManagerContract;
import network.bane.testhelper.DummyExecutionManagerContract;
import network.bane.testhelper.MessageTestStoreContract;
import network.bane.testhelper.TestContract;
import network.bane.util.structs.N3MessageMetadataDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.List;

import static io.neow3j.transaction.AccountSigner.none;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.types.ContractParameter.string;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static network.bane.util.TestHelper.securityGuard;
import static network.bane.util.helper.DefaultTestValues.MANAGEMENT_CONTRACT_HASH;
import static network.bane.util.helper.DefaultTestValues.MESSAGE_BRIDGE_CONTRACT_HASH;
import static network.bane.util.helper.TestHelper.alice;
import static network.bane.util.helper.TestHelper.bob;
import static network.bane.util.helper.TestHelper.createBridgeManagementDeployConfig;
import static network.bane.util.helper.TestHelper.createExecutionManagerDeployConfig;
import static network.bane.util.helper.TestHelper.createMessageBridgeDeployConfig;
import static network.bane.util.helper.TestHelper.executionManager;
import static network.bane.util.helper.TestHelper.management;
import static network.bane.util.helper.TestHelper.messageBridge;
import static network.bane.util.helper.TestHelper.neow3j;
import static network.bane.util.helper.TestHelper.setup;
import static network.bane.util.helper.TestHelper.setupExecutionManager;
import static network.bane.util.helper.TestHelper.setupMessageBridge;
import static network.bane.util.helper.TestHelper.messageTestStorer;
import static network.bane.util.helper.TestHelper.setupMessageTestStorer;
import static network.bane.util.helper.TestHelper.setupTestContract;
import static network.bane.util.structs.N3MessageDto.MESSAGE_TYPE_EXECUTABLE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ContractTest(
        blockTime = 1,
        contracts = {BridgeManagementContract.class, MessageBridgeContract.class, ExecutionManagerContract.class,
                MessageTestStoreContract.class, DummyExecutionManagerContract.class, TestContract.class},
        batchFile = "setup.batch"
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MessageExecutionManagerTest {

    @RegisterExtension
    public static final ContractTestExtension ext = new ContractTestExtension();

    @BeforeAll
    public static void setUp() throws Throwable {
        setup(ext);
        setupMessageBridge(ext);
        setupExecutionManager(ext);
        setupMessageTestStorer(ext);
        setupTestContract(ext);

        // This test requires the constant MESSAGE_BRIDGE_CONTRACT_HASH to be set correctly. The execution manager
        // requires the contract address at deployment time, so we cannot change it later.
        if (!messageBridge.getScriptHash().equals(MESSAGE_BRIDGE_CONTRACT_HASH)) {
            throw new RuntimeException(format("The message bridge contract hash is not set correctly. Update the " +
                    "script hash to 0x%s.", messageBridge.getScriptHash()));
        }
        // This test requires the constant MANAGEMENT_CONTRACT_HASH to be set correctly. The execution manager
        // requires the contract address at deployment time, so we cannot change it later.
        if (!MANAGEMENT_CONTRACT_HASH.equals(management.getScriptHash())) {
            throw new RuntimeException(format("The management contract hash is not set correctly. Update the script " +
                    "hash to 0x%s.", management.getScriptHash()));
        }

        messageBridge.setExecutionManager(executionManager.getScriptHash());

        messageBridge.unpause();
        messageTestStorer.setMessageBridge();
    }

    @DeployConfig(BridgeManagementContract.class)
    public static DeployConfiguration deployConfigManagement() {
        return createBridgeManagementDeployConfig();
    }

    @DeployConfig(MessageBridgeContract.class)
    public static DeployConfiguration deployConfigMessageBridge() {
        return createMessageBridgeDeployConfig();
    }

    @DeployConfig(ExecutionManagerContract.class)
    public static DeployConfiguration deployConfigExecutionManager() {
        return createExecutionManagerDeployConfig();
    }

    // region pause

    @Test
    @Order(0)
    public void test_pause_governor() throws Throwable {
        assertFalse(executionManager.isPaused());
        executionManager.pause();
        assertTrue(executionManager.isPaused());

        // revert the state for further tests
        executionManager.unpause();
    }

    @Test
    @Order(0)
    public void test_pause_securityGuard() throws Throwable {
        assertFalse(executionManager.isPaused());
        executionManager.pause(securityGuard);
        assertTrue(executionManager.isPaused());

        // revert the state for further tests
        executionManager.unpause();
    }

    @Test
    @Order(0)
    public void test_pause_fail_notGovernor() throws IOException {
        assertFalse(executionManager.isPaused());
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> executionManager.pause(alice));
        assertThat(thrown.getMessage(), containsString("No authorization - only governor or security guard"));
    }

    @Test
    @Order(0)
    public void test_pause_fail_alreadyPaused() throws Throwable {
        executionManager.pause();
        assertTrue(executionManager.isPaused());
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> executionManager.pause());
        assertThat(thrown.getMessage(), containsString("Contract paused"));

        // revert the state for further tests
        executionManager.unpause();
    }

    @Test
    @Order(0)
    public void test_unpause_fail_notGovernor() throws Throwable {
        executionManager.pause();
        assertTrue(executionManager.isPaused());
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> executionManager.unpause(alice));
        assertThat(thrown.getMessage(), containsString("No authorization - only governor"));

        // revert the state for further tests
        executionManager.unpause();
    }

    @Test
    @Order(0)
    public void test_unpause_fail_alreadyUnpaused() throws Throwable {
        assertFalse(executionManager.isPaused());
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> executionManager.unpause());
        assertThat(thrown.getMessage(), containsString("Contract not paused"));
    }

    // endregion
    // region execution failing

    @Test
    @Order(0)
    public void test_notAllowingCallToContractManagement_destroy() throws Throwable {
        byte[] maliciousUpdateCall = messageBridge.getSerializedN3MethodCall(ContractManagement.SCRIPT_HASH, "destroy",
                CallFlags.ALL, asList());

        BigInteger maliciousUpdateMsgNonce = messageBridge.storeMessage(maliciousUpdateCall);
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.executeMessage(AccountSigner.global(alice), maliciousUpdateMsgNonce));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Prohibited target"));
    }

    @Test
    @Order(0)
    public void test_notAllowingCallToContractManagement_update() throws Throwable {
        String contractName = "DummyExecutionManager";
        ContractParameter nefFileParam = getNefParamFromTestResources(contractName);
        ContractParameter manifestParam = getManifestParamFromTestResources(contractName);

        byte[] maliciousUpdateCall = messageBridge.getSerializedN3MethodCall(ContractManagement.SCRIPT_HASH, "update",
                CallFlags.ALL, asList(nefFileParam, manifestParam));

        BigInteger maliciousUpdateMsgNonce = messageBridge.storeMessage(maliciousUpdateCall);
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.executeMessage(AccountSigner.global(alice), maliciousUpdateMsgNonce));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Prohibited target"));
    }

    /**
     * Tests that the invocation of the execution manager's `executeMessage` method with a calling script hash other
     * than the bridge contract's script hash leads to an abort of the transaction.
     */
    @Test
    @Order(0)
    public void test_executeMessage_fail_bridgeNotCallingScriptHash() throws Throwable {
        BigInteger nonce = storeDefaultMessageForTestStoring("hello", string("world"));
        byte[] n3FuncCall = getSerializedN3MethodForTestStoring("test_exec_notViaBridge", string("hello"));
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> executionManager.executeMessage(none(alice), nonce, n3FuncCall));
        assertThat(thrown.getMessage(), containsString("No authorization - only bridge"));
    }

    /**
     * Tests that the message execution of method call bytes that do not match the expected format for deserialization
     * leads to an abort of the transaction.
     */
    @Test
    @Order(1)
    public void test_executeMessage_fail_n3MethodCallBytes_invalidFormatForDeserialization() throws Throwable {
        BigInteger nonce = messageBridge.storeMessage(new byte[]{0x03, 0x04, 0x05}); // malformed N3 method call bytes
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.executeMessage(none(alice), nonce));
        assertThat(thrown.getMessage(), containsString("invalid format"));
    }

    /**
     * Tests that using the zero address as a target aborts the transaction when trying to execute the message.
     */
    @Test
    @Order(1)
    public void test_executeMessage_fail_n3MethodCallBytes_invalidTarget() throws Throwable {
        byte[] n3MethodCallBytes = messageBridge.getSerializedN3MethodCall(Hash160.ZERO, "store", CallFlags.ALL,
                asList());
        BigInteger nonce = messageBridge.storeMessage(n3MethodCallBytes);
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.executeMessage(none(alice), nonce));
        assertThat(thrown.getMessage(), containsString("Method call has invalid values"));
    }

    /**
     * Tests that method call bytes that use an empty string for the method name are aborted when trying to execute
     * the message.
     */
    @Test
    @Order(1)
    public void test_executeMessage_fail_n3MethodCallBytes_invalidMethodName() throws Throwable {
        byte[] n3MethodCallBytes = messageBridge.getSerializedN3MethodCall(messageTestStorer.getScriptHash(), "",
                CallFlags.ALL, asList());
        BigInteger nonce = messageBridge.storeMessage(n3MethodCallBytes);
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.executeMessage(none(alice), nonce));
        assertThat(thrown.getMessage(), containsString("Method call has invalid values"));
    }

    /**
     * Tests that a method call that has the execution manager as a target fails.
     */
    @Test
    @Order(0)
    public void test_executeMessage_fail_reenteringExecutionManager() throws Throwable {
        BigInteger nextNonce = messageBridge.getMessageBridge().evmToN3MessageState.nonce.add(BigInteger.ONE);
        byte[] n3FuncCall = messageBridge.getSerializedN3MethodCall(
                executionManager.getScriptHash(), "executeMessage", CallFlags.ALL,
                asList(
                        integer(nextNonce),
                        byteArray(getSerializedN3MethodForTestStoring("test_exec_reentering", string("hello")))
                )
        );
        BigInteger nonce = messageBridge.storeMessage(n3FuncCall);
        assertThat(nonce, is(nextNonce));

        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.executeMessage(none(alice), nonce));
        assertThat(thrown.getMessage(), containsString("Prohibited target"));
    }

    // endregion
    // region message execution

    /**
     * Tests the successful execution of a message. The test flow is as follows:
     * <li>(1) A message is stored in the bridge contract using the `storeMessage` method.</li>
     * <li>(2) The `executeMessage` method of the bridge contract is called with the nonce of the stored message.</li>
     * <li>(3) During the execution via the bridge, the execution manager is expected to fire 2 events, i.e., an event
     * indicating that an execution happens and an event containing the return value of the method call.</li>
     * <li>(4) The method call that is stored in the message should invoke the `storeValue` method of the
     * `MessageTestStoreContract`, i.e., it should store a value in the contract at the given key. This storage entry
     * is asserted to be empty at the beginning of the test, and it is asserted to contain the expected value after
     * the execution.</li>
     */
    @Test
    @Order(1)
    public void test_executeMessage() throws Throwable {
        String key = "test_executeMessage";
        ContractParameter value = integer(42);
        assertNull(messageTestStorer.getStoredValue(key).getValue());

        BigInteger nonce = storeDefaultMessageForTestStoring(key, value);

        Hash256 txHash = messageBridge.executeMessage(none(alice), nonce);
        NeoApplicationLog.Execution exec = neow3j.getApplicationLog(txHash).send().getApplicationLog()
                .getFirstExecution();
        assertThat(exec.getNotifications(), hasSize(2));
        Notification event1 = exec.getNotifications().get(0);
        assertThat(event1.getContract(), is(messageBridge.getScriptHash()));
        assertThat(event1.getEventName(), is("Execute"));
        assertThat(event1.getState().getList(), hasSize(2));
        StackItem eventNonce = event1.getState().getList().get(0);
        assertThat(eventNonce.getInteger(), is(nonce));
        List<StackItem> eventMetadata = event1.getState().getList().get(1).getList();
        assertThat(eventMetadata.get(0).getInteger().intValue(), is(MESSAGE_TYPE_EXECUTABLE));
        assertThat(eventMetadata.get(1).getInteger(), lessThan(bestBlockTime()));
        assertThat(eventMetadata.get(2).getAddress(), is(alice.getAddress()));
        assertTrue(eventMetadata.get(3).getBoolean());

        Notification event2 = exec.getNotifications().get(1);
        assertThat(event2.getContract(), is(messageBridge.getScriptHash()));
        assertThat(event2.getEventName(), is("ExecutionResult"));
        assertThat(event2.getState().getList().get(0).getInteger(), is(nonce));
        assertNull(event2.getState().getList().get(1).getValue());

        assertThat(messageTestStorer.getStoredValue(key).getType(), is(StackItemType.INTEGER));
        assertThat(messageTestStorer.getStoredValue(key).getValue(), is(value.getValue()));
    }

    /**
     * This test verifies that when executing a message, it is possible for the target contract to retrieve all
     * information about the currently being executed message without having any prior knowledge passed on to it via
     * method parameters. All information of the message means: its nonce, its metadata as well as its method call
     * bytes.
     */
    @Test
    @Order(1)
    public void test_executeMessage_retrieveExecutingMessageMetadata() throws Throwable {
        assertThat(executionManager.getExecutingNonce(), is(BigInteger.ZERO));

        byte[] serializedN3MethodCall = messageBridge.getSerializedN3MethodCall(messageTestStorer.getScriptHash(),
                "storeMetadataOfExecutingMessage", CallFlags.ALL, asList());
        BigInteger timestamp = bestBlockTime();
        Hash160 sender = bob.getScriptHash();
        BigInteger nonce = messageBridge.storeMessage(serializedN3MethodCall, timestamp, sender, true);

        // Verify that nothing is stored yet - the interface returns zero values if there's nothing stored in the
        // contract.
        assertFalse(messageTestStorer.hasMetadataStored(nonce));

        messageBridge.executeMessage(none(alice), nonce);

        N3MessageMetadataDto storedMetadata = messageTestStorer.getStoredMetadata(nonce);
        assertThat(storedMetadata.timestamp, is(timestamp));
        assertThat(storedMetadata.sender, is(sender));

        assertThat(executionManager.getExecutingNonce(), is(BigInteger.ZERO));
    }

    // endregion
    // region private helpers

    private BigInteger bestBlockTime() throws IOException {
        return BigInteger.valueOf(
                neow3j.getBlockHeader(
                        neow3j.getBestBlockHash().send().getBlockHash()
                ).send().getBlock().getTime()
        );
    }

    private BigInteger storeDefaultMessageForTestStoring(String key, ContractParameter value) throws Throwable {
        return messageBridge.storeMessage(getSerializedN3MethodForTestStoring(key, value));
    }

    private byte[] getSerializedN3MethodForTestStoring(String key, ContractParameter value) throws IOException {
        return messageBridge.getSerializedN3MethodCall(messageTestStorer.getScriptHash(), "storeValue", CallFlags.ALL,
                asList(string(key), value));
    }

    private NefFile getNefFromTestResources(String contractName) throws IOException, DeserializationException {
        File contractNefFile = Paths.get("src", "test", "resources", contractName + ".nef").toFile();
        return NefFile.readFromFile(contractNefFile);
    }

    private ContractParameter getNefParamFromTestResources(String contractName) throws IOException,
            DeserializationException {
        return byteArray(getNefFromTestResources(contractName).toArray());
    }

    private ContractManifest getManifestFromTestResources(String contractName) throws IOException {
        File contractManifestFile = Paths.get("src", "test", "resources", contractName + ".manifest.json").toFile();
        ContractManifest manifest;
        try (FileInputStream s = new FileInputStream(contractManifestFile)) {
            manifest = ObjectMapperFactory.getObjectMapper().readValue(s, ContractManifest.class);
        }
        return manifest;
    }

    private ContractParameter getManifestParamFromTestResources(String contractName) throws IOException {
        ContractManifest manifest = getManifestFromTestResources(contractName);
        byte[] manifestBytes = ObjectMapperFactory.getObjectMapper().writeValueAsBytes(manifest);
        return byteArray(manifestBytes);
    }

    // endregion

}
