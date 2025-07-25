package network.bane.message;

import io.neow3j.protocol.core.response.NeoApplicationLog;
import io.neow3j.protocol.core.response.Notification;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.test.ContractTest;
import io.neow3j.test.ContractTestExtension;
import io.neow3j.test.DeployConfig;
import io.neow3j.test.DeployConfiguration;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.types.CallFlags;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.types.StackItemType;
import io.neow3j.wallet.Account;
import network.bane.management.BridgeManagementContract;
import network.bane.messageexecution.ExecutionManagerContract;
import network.bane.testhelper.DummyExecutionManagerContract;
import network.bane.testhelper.MessageTestStoreContract;
import network.bane.testhelper.TestContract;
import network.bane.util.structs.N3MessageDto;
import network.bane.util.structs.N3MessageMetadataDto;
import network.bane.util.structs.N3MessageMetadataExecDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static io.neow3j.transaction.AccountSigner.none;
import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.types.ContractParameter.string;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static network.bane.util.MessageHelper.createN3MessageHash;
import static network.bane.util.TestHelper.concatAndKeccak256;
import static network.bane.util.TestHelper.securityGuard;
import static network.bane.util.TestHelper.signMsg;
import static network.bane.util.TestHelper.validator1;
import static network.bane.util.TestHelper.validator2;
import static network.bane.util.TestHelper.validator3;
import static network.bane.util.TestHelper.validator4;
import static network.bane.util.TestHelper.validator5;
import static network.bane.util.helper.DefaultTestValues.MANAGEMENT_CONTRACT_HASH;
import static network.bane.util.helper.DefaultTestValues.MESSAGE_BRIDGE_CONTRACT_HASH;
import static network.bane.util.helper.PrintHelper.printTransactionFee;
import static network.bane.util.helper.TestHelper.alice;
import static network.bane.util.helper.TestHelper.bob;
import static network.bane.util.helper.TestHelper.createBridgeManagementDeployConfig;
import static network.bane.util.helper.TestHelper.createExecutionManagerDeployConfig;
import static network.bane.util.helper.TestHelper.createMessageBridgeDeployConfig;
import static network.bane.util.helper.TestHelper.executionManager;
import static network.bane.util.helper.TestHelper.incrementAndGetN3MessageNonce;
import static network.bane.util.helper.TestHelper.management;
import static network.bane.util.helper.TestHelper.messageBridge;
import static network.bane.util.helper.TestHelper.neow3j;
import static network.bane.util.helper.TestHelper.setup;
import static network.bane.util.helper.TestHelper.setupExecutionManager;
import static network.bane.util.helper.TestHelper.setupMessageBridge;
import static network.bane.util.helper.TestHelper.messageTestStorer;
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
    // region execution

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
        BigInteger nonce = storeMessage(new byte[]{0x03, 0x04, 0x05}); // malformed N3 method call bytes
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
        BigInteger nonce = storeMessage(n3MethodCallBytes);
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
        BigInteger nonce = storeMessage(n3MethodCallBytes);
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
        BigInteger nonce = storeMessage(n3FuncCall);
        assertThat(nonce, is(nextNonce));

        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.executeMessage(none(alice), nonce));
        assertThat(thrown.getMessage(), containsString("Prohibited target"));
    }

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
        BigInteger nonce = storeMessage(serializedN3MethodCall, timestamp, sender, true);

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
        return storeMessage(getSerializedN3MethodForTestStoring(key, value));
    }

    private byte[] getSerializedN3MethodForTestStoring(String key, ContractParameter value) throws IOException {
        return messageBridge.getSerializedN3MethodCall(messageTestStorer.getScriptHash(), "storeValue", CallFlags.ALL,
                asList(string(key), value));
    }

    private BigInteger storeMessage(byte[] n3FuncCall) throws Throwable {
        Hash256 bestBlockHash = neow3j.getBestBlockHash().send().getBlockHash();
        long bestBlockTime = neow3j.getBlockHeader(bestBlockHash).send().getBlock().getTime();
        BigInteger timestamp = BigInteger.valueOf(bestBlockTime);
        Hash160 sender = alice.getScriptHash();
        return storeMessage(n3FuncCall, timestamp, sender, true);
    }

    private BigInteger storeMessage(byte[] msgBytes, BigInteger timestamp, Hash160 sender, boolean storeResult)
            throws Throwable {
        // This test only works if it is the first test in the order of storing messages, due to the use of raw data for
        // the nonce, to and amount.
        BigInteger nonce = incrementAndGetN3MessageNonce(); // Necessary if other tests are run besides this one.

        // 0000000000000000000000000000000000000000000000000000000000000001 nonce hex padded
        // 00000000000000000000000000000000000000000000000000000000687ca840 1753000000 hex padded
        // 69ecca587293047be4c59159bf8bc399985c160d alice
        // efcdab9078563412 executable code reversed

        N3MessageMetadataExecDto metadata = new N3MessageMetadataExecDto(timestamp, sender, storeResult);
        String msgHash1 = createN3MessageHash(nonce, metadata, msgBytes);
        N3MessageDto n3MessageDto = new N3MessageDto(metadata, msgBytes);

        Hash256 currentRoot = messageBridge.getMessageBridge().evmToN3MessageState.root;
        String root = concatAndKeccak256(currentRoot.toString(), msgHash1);
        List<Account> validators = asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter messageEnvelope = array(integer(nonce), n3MessageDto.toContractParameter(messageBridge));
        Hash256 txHash = messageBridge.storeMessages(root, signMsg(validators, root), array(messageEnvelope));
        printTransactionFee(neow3j, "tx with 1 message", txHash);
        return nonce;
    }

    // endregion

}
