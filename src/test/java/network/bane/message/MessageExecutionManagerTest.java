package network.bane.message;

import io.neow3j.contract.ContractManagement;
import io.neow3j.contract.NefFile;
import io.neow3j.protocol.ObjectMapperFactory;
import io.neow3j.protocol.core.response.ContractManifest;
import io.neow3j.protocol.core.response.ContractState;
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
import static io.neow3j.types.ContractParameter.any;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.types.ContractParameter.string;
import static io.neow3j.utils.Numeric.hexStringToByteArray;
import static io.neow3j.utils.Numeric.toHexStringNoPrefix;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static network.bane.util.TestHelper.owner;
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
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        byte[] maliciousUpdateCall = messageBridge.serializeCall(ContractManagement.SCRIPT_HASH, "destroy",
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

        byte[] maliciousUpdateCall = messageBridge.serializeCall(ContractManagement.SCRIPT_HASH, "update",
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
        byte[] callWithZeroTarget =
                hexStringToByteArray("400428140000000000000000000000000000000000000000280573746f726521010f4000");

        BigInteger nonce = messageBridge.storeMessage(callWithZeroTarget);
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
        byte[] callWithEmptyMethod =
                hexStringToByteArray("0x400428143c4cac7301ea80416d69b9a09cd60a9ee3dce1c2280021010f4000");
        BigInteger nonce = messageBridge.storeMessage(callWithEmptyMethod);
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
        byte[] n3FuncCall = messageBridge.serializeCall(
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
        assertThat(messageTestStorer.getStoredValue(key).getValue(), is(not(42)));

        BigInteger nonce = storeDefaultMessageForTestStoring(key, value);

        Hash256 txHash = messageBridge.executeMessage(none(alice), nonce);
        NeoApplicationLog.Execution exec = neow3j.getApplicationLog(txHash).send().getApplicationLog()
                .getFirstExecution();
        assertThat(exec.getNotifications(), hasSize(1));

        Notification execEvent = exec.getFirstNotification();
        assertThat(execEvent.getContract(), is(messageBridge.getScriptHash()));
        assertThat(execEvent.getEventName(), is("Execution"));
        assertThat(execEvent.getState().getList().get(0).getInteger(), is(nonce));
        String expectedResultHex = "21012a"; // serialized form of int 42 (i.e., type: 0x21, size: 0x01, value: 0x2a)
        assertThat(execEvent.getState().getList().get(1).getInteger(), is(BigInteger.ONE));
        assertThat(execEvent.getState().getList().get(2).getInteger(), is(BigInteger.ZERO));
        assertThat(execEvent.getState().getList().get(3).getType(), is(StackItemType.BYTE_STRING));
        assertThat(execEvent.getState().getList().get(3).getHexString(), is(expectedResultHex));

        assertThat(messageTestStorer.getStoredValue(key).getType(), is(StackItemType.INTEGER));
        assertThat(messageTestStorer.getStoredValue(key).getValue(), is(value.getValue()));

        byte[] result = messageBridge.getResult(nonce);
        // The result object gets serialized when stored to allow arbitrary return types.
        // In this case, even though the result type is already a byte string, it gets serialized again ending up
        // with the actual result byte string being prepended with a byte string type prefix (0x28) and its size (0x03).
        assertThat(toHexStringNoPrefix(result), is("2803" + expectedResultHex));
    }

    @Test
    @Order(2)
    public void test_executeMessage_withIntegerResult() throws Throwable {
        String key = "test_executeMessage";
        ContractParameter value = string("hello");
        assertThat(messageTestStorer.getStoredValue(key).getValue(), is(not("hello")));

        BigInteger nonce = messageBridge.storeMessage(
                getSerializedN3MethodForTestStoring("storeValueAndReturnItsLength", key, value)
        );

        Hash256 txHash = messageBridge.executeMessage(none(alice), nonce);
        NeoApplicationLog.Execution exec = neow3j.getApplicationLog(txHash).send().getApplicationLog()
                .getFirstExecution();

        Notification event2 = exec.getFirstNotification();
        assertThat(event2.getState().getList().get(1).getInteger(), is(BigInteger.ONE));
        assertThat(event2.getState().getList().get(2).getInteger(), is(BigInteger.ZERO));
        assertThat(event2.getState().getList().get(3).getType(), is(StackItemType.INTEGER));
        // length of "hello" serialized is 7 - type (1 byte), size (1 byte), value (5 bytes)
        assertThat(event2.getState().getList().get(3).getInteger().intValue(), is(7));

        assertThat(messageTestStorer.getStoredValue(key).getType(), is(StackItemType.BYTE_STRING));
        assertThat(messageTestStorer.getStoredValue(key).getString(), is(value.getValue()));

        byte[] result = messageBridge.getResult(nonce);
        // The result object gets serialized when stored to allow arbitrary return types.
        // In this case, the return was a single integer. Thus, the serialization of it ends up being its type (0x21),
        // its size (0x01) followed by the actual integer value (0x07).
        assertThat(toHexStringNoPrefix(result), is("210107"));
    }

    @Test
    @Order(2)
    public void test_executeMessage_largeResult() throws Throwable {
        int resultSize = 2561;
        // Create byte array of size resultSize filled with random values
        byte[] resultArray = new byte[resultSize];
        for (int i = 0; i < resultSize; i++) {
            // Random byte
            resultArray[i] = (byte) (Math.random() * 256 - 128);
        }
        BigInteger nonce = messageBridge.storeMessage(
                serializeMethodCall(messageTestStorer.getScriptHash(), "returnValue", asList(byteArray(resultArray)))
        );

        Hash256 txHash = messageBridge.executeMessage(none(alice), nonce);
        NeoApplicationLog.Execution exec = neow3j.getApplicationLog(txHash).send().getApplicationLog()
                .getFirstExecution();

        BigInteger maxBytesPerResultEvent = messageBridge.maxMessageSize();
        // The result will be serialized, adding 4 bytes as the raw byte array will be prefixed with the type (byte
        // string) and its size (3 bytes as it is > 252 bytes).
        BigInteger resultBytesSize = BigInteger.valueOf(resultSize + 4);

        BigInteger[] nrChunksAndRemainder = resultBytesSize.divideAndRemainder(maxBytesPerResultEvent);
        int nrChunks = nrChunksAndRemainder[0].intValue();
        if (nrChunksAndRemainder[1].compareTo(BigInteger.ZERO) > 0) {
            nrChunks++;
        }
        List<Notification> notifications = exec.getNotifications();
        assertThat(notifications, hasSize(nrChunks));

        // Iterate over the notifications and append the byt3 arrays from them to reconstruct the full result array.
        // Then, assert that the reconstructed array is identical to the original array.
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < notifications.size(); i++) {
            Notification chunkEvent = notifications.get(i);
            assertThat(chunkEvent.getContract(), is(messageBridge.getScriptHash()));
            assertThat(chunkEvent.getEventName(), is("Execution"));
            List<StackItem> stateList = chunkEvent.getState().getList();
            assertThat(stateList.get(0).getInteger(), is(nonce));
            assertThat(stateList.get(1).getInteger().intValue(), is(nrChunks)); // total chunks
            assertThat(stateList.get(2).getInteger().intValue(), is(i)); // current chunk index
            assertThat(stateList.get(3).getType(), is(StackItemType.BYTE_STRING));
            b.append(stateList.get(3).getHexString());
        }
        String reconstructedHex = b.toString();
        String expectedHexResult = toHexStringNoPrefix(resultArray);
        String expectedPrefix = "28" + "fd" + "010a"; // 28:type, fd:size requires 2 bytes, 0a01 = 2561
        assertThat(reconstructedHex, is(expectedPrefix + expectedHexResult));

        byte[] result = messageBridge.getResult(nonce);
        // The result object gets serialized when stored to allow arbitrary return types.
        // In this case, the return was a single byte string. Thus, the serialization of it ends up being its type
        // (0x28), its size (0x01) followed by the actual integer value (0x07).
        assertThat(toHexStringNoPrefix(result), is(expectedPrefix + expectedHexResult));
    }

    @Test
    @Order(2)
    public void test_executeMessage_largeResult_noRemainder() throws Throwable {
        // 5 full chunks, no remainder. 4 bytes are included for type and size of the serialized byte array.
        int resultSize = messageBridge.maxMessageSize().intValue() * 5 - 4; // = 3996
        // Create byte array of size resultSize filled with random values
        byte[] resultArray = new byte[resultSize];
        for (int i = 0; i < resultSize; i++) {
            // Random byte
            resultArray[i] = (byte) (Math.random() * 256 - 128);
        }
        BigInteger nonce = messageBridge.storeMessage(
                serializeMethodCall(messageTestStorer.getScriptHash(), "returnValue", asList(byteArray(resultArray)))
        );

        Hash256 txHash = messageBridge.executeMessage(none(alice), nonce);
        NeoApplicationLog.Execution exec = neow3j.getApplicationLog(txHash).send().getApplicationLog()
                .getFirstExecution();

        BigInteger maxBytesPerResultEvent = messageBridge.maxMessageSize();
        // The result will be serialized, adding 4 bytes as the raw byte array will be prefixed with the type (byte
        // string) and its size (3 bytes as it is > 252 bytes).
        BigInteger resultBytesSize = BigInteger.valueOf(resultSize + 4);

        BigInteger[] nrChunksAndRemainder = resultBytesSize.divideAndRemainder(maxBytesPerResultEvent);
        int nrChunks = nrChunksAndRemainder[0].intValue();
        assertThat(nrChunksAndRemainder[1], is(BigInteger.ZERO));

        List<Notification> notifications = exec.getNotifications();
        assertThat(notifications, hasSize(nrChunks));

        // Iterate over the notifications and append the byt3 arrays from them to reconstruct the full result array.
        // Then, assert that the reconstructed array is identical to the original array.
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < notifications.size(); i++) {
            Notification chunkEvent = notifications.get(i);
            assertThat(chunkEvent.getContract(), is(messageBridge.getScriptHash()));
            assertThat(chunkEvent.getEventName(), is("Execution"));
            List<StackItem> stateList = chunkEvent.getState().getList();
            assertThat(stateList.get(0).getInteger(), is(nonce));
            assertThat(stateList.get(1).getInteger().intValue(), is(nrChunks)); // total chunks
            assertThat(stateList.get(2).getInteger().intValue(), is(i)); // current chunk index
            assertThat(stateList.get(3).getType(), is(StackItemType.BYTE_STRING));
            b.append(stateList.get(3).getHexString());
        }
        String reconstructedHex = b.toString();
        String expectedHexResult = toHexStringNoPrefix(resultArray);
        String expectedPrefix = "28" + "fd" + "9c0f"; // 28:type, fd:size requires 2 bytes, 0f9c = 3996
        assertThat(reconstructedHex, is(expectedPrefix + expectedHexResult));

        byte[] result = messageBridge.getResult(nonce);
        // The result object gets serialized when stored to allow arbitrary return types.
        // In this case, the return was a single byte string. Thus, the serialization of it ends up being its type
        // (0x28), its size (0x01) followed by the actual integer value (0x07).
        assertThat(toHexStringNoPrefix(result), is(expectedPrefix + expectedHexResult));
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

        byte[] serializedN3MethodCall = messageBridge.serializeCall(messageTestStorer.getScriptHash(),
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
    // region update

    @Test
    @Order(99)
    public void test_update_notPaused() throws Throwable {
        String contractFileName = "DummyExecutionManager";
        NefFile nefFile = getNefFromTestResources(contractFileName);
        ContractManifest manifest = getManifestFromTestResources(contractFileName);
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> executionManager.update(nefFile, manifest, any(null)));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Contract not paused"));
    }

    @Test
    @Order(99)
    public void test_update_notOwner() throws Throwable {
        executionManager.pause();
        String contractFileName = "DummyExecutionManager";
        NefFile nefFile = getNefFromTestResources(contractFileName);
        ContractManifest manifest = getManifestFromTestResources(contractFileName);
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> executionManager.update(bob, nefFile, manifest, any(null)));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No authorization - only owner"));
        // Revert the state for further tests
        executionManager.unpause();
    }

    @Test
    @Order(100)
    public void test_update_successful() throws Throwable {
        executionManager.pause();
        String contractFileName = "DummyExecutionManager";
        NefFile nefFile = getNefFromTestResources(contractFileName);
        ContractManifest manifest = getManifestFromTestResources(contractFileName);

        Hash160 execManagerHash = executionManager.getScriptHash();
        ContractState contractStateBefore = neow3j.getContractState(execManagerHash).send().getContractState();
        assertThat(contractStateBefore.getUpdateCounter(), is(0));
        assertThat(contractStateBefore.getManifest().getAbi().getMethods(), hasSize(greaterThan(1)));

        executionManager.update(owner, nefFile, manifest, any(null));

        ContractState contractStateAfter = neow3j.getContractState(execManagerHash).send().getContractState();
        assertThat(contractStateAfter.getUpdateCounter(), is(1));
        assertThat(contractStateAfter.getManifest().getAbi().getMethods(), hasSize(1));

        // This should be the last test. Thus, there's no need to revert the state for further tests.
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

    private byte[] serializeMethodCall(Hash160 target, String method, List<ContractParameter> params)
            throws IOException {
        return messageBridge.serializeCall(target, method, CallFlags.ALL, params);
    }

    private byte[] getSerializedN3MethodForTestStoring(String key, ContractParameter value) throws IOException {
        return getSerializedN3MethodForTestStoring("storeValueAndReturnIt", key, value);
    }

    private byte[] getSerializedN3MethodForTestStoring(String method, String key, ContractParameter value)
            throws IOException {
        return messageBridge.serializeCall(messageTestStorer.getScriptHash(), method, CallFlags.ALL,
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
