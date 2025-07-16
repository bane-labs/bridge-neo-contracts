package network.bane.bridge;

import io.neow3j.protocol.core.response.Notification;
import io.neow3j.test.ContractTest;
import io.neow3j.test.ContractTestExtension;
import io.neow3j.test.DeployConfig;
import io.neow3j.test.DeployConfiguration;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;
import network.bane.management.BridgeManagementContract;
import network.bane.testhelper.TestContract;
import network.bane.util.TestHelper;
import network.bane.util.structs.N3MessageDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.integer;
import static java.util.Arrays.asList;
import static network.bane.util.TestHelper.concatAndKeccak256;
import static network.bane.util.TestHelper.createN3MessageHash;
import static network.bane.util.TestHelper.getMessageStorEvents;
import static network.bane.util.TestHelper.governor;
import static network.bane.util.TestHelper.securityGuard;
import static network.bane.util.TestHelper.signMsg;
import static network.bane.util.TestHelper.validator1;
import static network.bane.util.TestHelper.validator2;
import static network.bane.util.TestHelper.validator3;
import static network.bane.util.TestHelper.validator4;
import static network.bane.util.TestHelper.validator5;
import static network.bane.util.helper.PrintHelper.printTransactionFee;
import static network.bane.util.helper.TestHelper.alice;
import static network.bane.util.helper.TestHelper.bridge;
import static network.bane.util.helper.TestHelper.createBridgeDeployConfig;
import static network.bane.util.helper.TestHelper.createBridgeManagementDeployConfig;
import static network.bane.util.helper.TestHelper.decrementN3MessageNonce;
import static network.bane.util.helper.TestHelper.incrementAndGetN3MessageNonce;
import static network.bane.util.helper.TestHelper.neow3j;
import static network.bane.util.helper.TestHelper.setup;
import static network.bane.util.helper.TestHelper.setupBridge;
import static network.bane.util.helper.TestHelper.testContract;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ContractTest(
        blockTime = 1,
        contracts = {BridgeManagementContract.class, BridgeContract.class, TestContract.class},
        batchFile = "setup.batch"
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MessageBridgeTest {

    @RegisterExtension
    public static final ContractTestExtension ext = new ContractTestExtension();

    @BeforeAll
    public static void setUp() throws Throwable {
        setup(ext);
        setupBridge(ext);

        bridge.setDefaultMessageBridge();
        bridge.unpauseMessageBridge();
    }

    @DeployConfig(BridgeManagementContract.class)
    public static DeployConfiguration deployConfigManagement() {
        return createBridgeManagementDeployConfig();
    }

    @DeployConfig(BridgeContract.class)
    public static DeployConfiguration deployConfigBridge() {
        return createBridgeDeployConfig();
    }

    // region pause

    @Test
    @Order(0)
    public void test_pauseMessageBridge_governor() throws Throwable {
        assertFalse(bridge.getMessageBridge().paused);
        bridge.pauseMessageBridge(governor);
        assertTrue(bridge.getMessageBridge().paused);

        // revert the state for further tests
        bridge.unpauseMessageBridge();
        assertFalse(bridge.getMessageBridge().paused);
    }

    @Test
    @Order(0)
    public void test_pauseMessageBridge_securityGuard() throws Throwable {
        assertFalse(bridge.getMessageBridge().paused);
        bridge.pauseMessageBridge(securityGuard);
        assertTrue(bridge.getMessageBridge().paused);

        // revert the state for further tests
        bridge.unpauseMessageBridge();
    }

    @Test
    @Order(0)
    public void test_pauseMessageBridge_notGovernor() throws IOException {
        assertFalse(bridge.getMessageBridge().paused);
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.pauseMessageBridge(alice));
        assertThat(thrown.getMessage(), containsString("No authorization - only governor or security guard"));
    }

    @Test
    @Order(0)
    public void test_unpauseMessageBridge_notGovernor() throws Throwable {
        bridge.pauseMessageBridge();
        assertTrue(bridge.getMessageBridge().paused);
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.unpauseMessageBridge(alice));
        assertThat(thrown.getMessage(), containsString("No authorization - only governor"));

        // revert the state for further tests
        bridge.unpauseMessageBridge();
    }

    // endregion
    // region store

    @Test
    @Order(0)
    public void test_storeMessages_1_notRelayer() {
        // This test only works if it is the first test in the order of storing messages, due to the use of raw data for
        // the nonce, to and amount.
        BigInteger nonce = incrementAndGetN3MessageNonce(); // Necessary if other tests are run besides this one.
        BigInteger timestamp = new BigInteger("1753000000");
        Hash160 sender = alice.getScriptHash();
        String executableCode = "0x1234567890abcdef";

        String msgHash1 = createN3MessageHash(nonce, timestamp, sender, executableCode);
        N3MessageDto.N3MessageMetadataDto metadata = new N3MessageDto.N3MessageMetadataDto(timestamp, sender);
        N3MessageDto n3MessageDto = new N3MessageDto(metadata, executableCode);

        String root = concatAndKeccak256(Hash256.ZERO.toString(), msgHash1);
        List<Account> validators = asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter messageEnvelope = array(integer(nonce), n3MessageDto.toContractParameter());
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.storeMessages(governor, root, signMsg(validators, root), array(messageEnvelope)));
        assertThat(thrown.getMessage(), containsString("No authorization - only relayer"));

        decrementN3MessageNonce();
    }

    @Test
    @Order(0)
    public void test_storeMessages_1_bridgePaused() throws Throwable {
        bridge.pauseBridge();
        assertTrue(bridge.isPaused());

        // This test only works if it is the first test in the order of storing messages, due to the use of raw data for
        // the nonce, to and amount.
        BigInteger nonce = incrementAndGetN3MessageNonce(); // Necessary if other tests are run besides this one.
        BigInteger timestamp = new BigInteger("1753000000");
        Hash160 sender = alice.getScriptHash();
        String executableCode = "0x1234567890abcdef";

        String msgHash1 = createN3MessageHash(nonce, timestamp, sender, executableCode);
        N3MessageDto.N3MessageMetadataDto metadata = new N3MessageDto.N3MessageMetadataDto(timestamp, sender);
        N3MessageDto n3MessageDto = new N3MessageDto(metadata, executableCode);

        String root = concatAndKeccak256(Hash256.ZERO.toString(), msgHash1);
        List<Account> validators = asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter messageEnvelope = array(integer(nonce), n3MessageDto.toContractParameter());
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.storeMessages(root, signMsg(validators, root), array(messageEnvelope)));
        assertThat(thrown.getMessage(), containsString("Contract paused"));

        bridge.unpause();
        decrementN3MessageNonce();
    }

    @Test
    @Order(0)
    public void test_storeMessages_1_messageBridgePaused() throws Throwable {
        bridge.pauseMessageBridge();
        assertTrue(bridge.messageBridgeIsPaused());

        // This test only works if it is the first test in the order of storing messages, due to the use of raw data for
        // the nonce, to and amount.
        BigInteger nonce = incrementAndGetN3MessageNonce(); // Necessary if other tests are run besides this one.
        BigInteger timestamp = new BigInteger("1753000000");
        Hash160 sender = alice.getScriptHash();
        String executableCode = "0x1234567890abcdef";

        String msgHash1 = createN3MessageHash(nonce, timestamp, sender, executableCode);
        N3MessageDto.N3MessageMetadataDto metadata = new N3MessageDto.N3MessageMetadataDto(timestamp, sender);
        N3MessageDto n3MessageDto = new N3MessageDto(metadata, executableCode);

        String root = concatAndKeccak256(Hash256.ZERO.toString(), msgHash1);
        List<Account> validators = asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter messageEnvelope = array(integer(nonce), n3MessageDto.toContractParameter());
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.storeMessages(root, signMsg(validators, root), array(messageEnvelope)));
        assertThat(thrown.getMessage(), containsString("Message bridge paused"));

        bridge.unpauseMessageBridge();
        decrementN3MessageNonce();
    }

    @Test
    @Order(0)
    public void test_storeMessages_1_noMessages() throws Throwable {
        String dummyRoot = "";
        List<Account> validators = asList(validator1);
        Map<ContractParameter, ContractParameter> dummyValidatorSigMap = signMsg(validators, dummyRoot);
        ContractParameter emptyN3MsgEnvelopeArray = array();

        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.storeMessages(dummyRoot, dummyValidatorSigMap, emptyN3MsgEnvelopeArray));
        assertThat(thrown.getMessage(), containsString("At least one message required"));
    }

    @Test
    @Order(0)
    public void test_storeMessage_1_incorrectNextNonce() {
        // This test only works if it is the first test in the order of storing messages, due to the use of raw data for
        // the nonce, to and amount.
        BigInteger nonce = incrementAndGetN3MessageNonce().add(BigInteger.ONE);
        System.out.println(nonce);
        BigInteger timestamp = new BigInteger("1753000000");
        Hash160 sender = alice.getScriptHash();
        String executableCode = "0x1234567890abcdef";

        String msgHash1 = createN3MessageHash(nonce, timestamp, sender, executableCode);
        N3MessageDto.N3MessageMetadataDto metadata = new N3MessageDto.N3MessageMetadataDto(timestamp, sender);
        N3MessageDto n3MessageDto = new N3MessageDto(metadata, executableCode);

        String root = concatAndKeccak256(Hash256.ZERO.toString(), msgHash1);
        List<Account> validators = asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter messageEnvelope = array(integer(nonce), n3MessageDto.toContractParameter());
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.storeMessages(root, signMsg(validators, root), messageEnvelope));
        assertThat(thrown.getMessage(), containsString("Provided messages are not subsequent"));

        decrementN3MessageNonce();
    }

    @Test
    @Order(0)
    public void test_storeMessage_1_invalidRoot() {
        // This test only works if it is the first test in the order of storing messages, due to the use of raw data for
        // the nonce, to and amount.
        BigInteger nonce = incrementAndGetN3MessageNonce(); // Necessary if other tests are run besides this one.
        BigInteger timestamp = new BigInteger("1753000000");
        Hash160 sender = alice.getScriptHash();
        String executableCode = "0x1234567890abcdef";

        String msgHash1 = createN3MessageHash(nonce, timestamp, sender, executableCode);
        N3MessageDto.N3MessageMetadataDto metadata = new N3MessageDto.N3MessageMetadataDto(timestamp, sender);
        N3MessageDto n3MessageDto = new N3MessageDto(metadata, executableCode);

        String invalidRoot = concatAndKeccak256(Hash256.ZERO.toString(), msgHash1) + "01"; // Invalid root
        List<Account> validators = asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter messageEnvelope = array(integer(nonce), n3MessageDto.toContractParameter());
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                // Root signed by validators, but not matching the messages.
                () -> bridge.storeMessages(invalidRoot, signMsg(validators, invalidRoot), array(messageEnvelope)));
        assertThat(thrown.getMessage(), containsString("Invalid root"));

        decrementN3MessageNonce();
    }

    @Test
    @Order(0)
    public void test_storeMessage_1_insufficientNrSigs() {
        // This test only works if it is the first test in the order of storing messages, due to the use of raw data for
        // the nonce, to and amount.
        BigInteger nonce = incrementAndGetN3MessageNonce(); // Necessary if other tests are run besides this one.
        BigInteger timestamp = new BigInteger("1753000000");
        Hash160 sender = alice.getScriptHash();
        String executableCode = "0x1234567890abcdef";

        String msgHash1 = createN3MessageHash(nonce, timestamp, sender, executableCode);
        N3MessageDto.N3MessageMetadataDto metadata = new N3MessageDto.N3MessageMetadataDto(timestamp, sender);
        N3MessageDto n3MessageDto = new N3MessageDto(metadata, executableCode);

        String root = concatAndKeccak256(Hash256.ZERO.toString(), msgHash1);
        List<Account> validators = asList(validator1, validator2, validator3, validator4);
        ContractParameter messageEnvelope = array(integer(nonce), n3MessageDto.toContractParameter());
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.storeMessages(root, signMsg(validators, root), array(messageEnvelope)));
        assertThat(thrown.getMessage(), containsString("Insufficient signatures"));

        decrementN3MessageNonce();
    }

    @Test
    @Order(0)
    public void test_storeMessage_1_failingSigVerification() {
        // This test only works if it is the first test in the order of storing messages, due to the use of raw data for
        // the nonce, to and amount.
        BigInteger nonce = incrementAndGetN3MessageNonce(); // Necessary if other tests are run besides this one.
        BigInteger timestamp = new BigInteger("1753000000");
        Hash160 sender = alice.getScriptHash();
        String executableCode = "0x1234567890abcdef";

        String msgHash1 = createN3MessageHash(nonce, timestamp, sender, executableCode);
        N3MessageDto.N3MessageMetadataDto metadata = new N3MessageDto.N3MessageMetadataDto(timestamp, sender);
        N3MessageDto n3MessageDto = new N3MessageDto(metadata, executableCode);

        String root = concatAndKeccak256(Hash256.ZERO.toString(), msgHash1);
        List<Account> validators = asList(validator1, validator2, validator3, validator4, alice); // Non-validator sig
        ContractParameter messageEnvelope = array(integer(nonce), n3MessageDto.toContractParameter());
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.storeMessages(root, signMsg(validators, root), array(messageEnvelope)));
        assertThat(thrown.getMessage(), containsString("Invalid validator signatures"));

        decrementN3MessageNonce();
    }

    @Test
    @Order(1)
    public void test_storeMessage_1() throws Throwable {
        // This test only works if it is the first test in the order of storing messages, due to the use of raw data for
        // the nonce, to and amount.
        BigInteger nonce = incrementAndGetN3MessageNonce(); // Necessary if other tests are run besides this one.
        BigInteger timestamp = new BigInteger("1753000000");
        Hash160 sender = alice.getScriptHash();
        String executableCode = "0x1234567890abcdef";

        // 0000000000000000000000000000000000000000000000000000000000000001 nonce hex padded
        // 00000000000000000000000000000000000000000000000000000000687ca840 1753000000 hex padded
        // 69ecca587293047be4c59159bf8bc399985c160d alice
        // efcdab9078563412 executable code reversed

        String msgHash1 = createN3MessageHash(nonce, timestamp, sender, executableCode);
        N3MessageDto.N3MessageMetadataDto metadata = new N3MessageDto.N3MessageMetadataDto(timestamp, sender);
        N3MessageDto n3MessageDto = new N3MessageDto(metadata, executableCode);

        String root = concatAndKeccak256(Hash256.ZERO.toString(), msgHash1);
        List<Account> validators = asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter messageEnvelope = array(integer(nonce), n3MessageDto.toContractParameter());
        Hash256 txHash = bridge.storeMessages(root, signMsg(validators, root), array(messageEnvelope));
        printTransactionFee(neow3j, "tx with 1 message", txHash);
        List<TestHelper.N3MessageStoreEvent> n3MessageStoreEvents = getMessageStorEvents(txHash, neow3j,
                bridge.getScriptHash());
        assertThat(n3MessageStoreEvents, hasSize(1));
        TestHelper.N3MessageStoreEvent n3MessageStoreEvent = n3MessageStoreEvents.get(0);
        assertThat(n3MessageStoreEvent.nonce, is(nonce));
        N3MessageDto.N3MessageMetadataDto expectedMetadata = metadata;
        assertThat(n3MessageStoreEvent.messageMetadata, is(expectedMetadata));

        N3MessageDto message = bridge.getMessage(nonce);
        assertThat(message, is(n3MessageDto));

        assertFalse(bridge.messageHasBeenExecuted(nonce));
    }

    // endregion
    // region config

    @Test
    @Order(0)
    public void test_setSendingFee() throws Throwable {
        BigInteger sendingFeeBefore = bridge.messageSendingFee();
        BigInteger newSendingFee = new BigInteger("200");
        assertThat(newSendingFee, is(not(sendingFeeBefore)));

        Hash256 tx = bridge.setMessageSendingFee(newSendingFee);
        assertThat(bridge.messageSendingFee(), is(newSendingFee));

        Notification notification = neow3j.getApplicationLog(tx).send().getApplicationLog().getFirstExecution()
                .getFirstNotification();
        assertThat(notification.getContract(), is(bridge.getScriptHash()));
        assertThat(notification.getEventName(), is("MessageSendingFeeChange"));
        assertThat(notification.getState().getList(), hasSize(1));
        assertThat(notification.getState().getList().get(0).getInteger(), is(newSendingFee));

        // revert the state for further tests
        bridge.setMessageSendingFee(sendingFeeBefore);
    }

    @Test
    @Order(0)
    public void test_setSendingFee_invalidValue() {
        BigInteger invalidSendingFee = new BigInteger("-1");
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.setMessageSendingFee(invalidSendingFee));
        assertThat(thrown.getMessage(), containsString("Sending fee must be nonnegative"));
    }

    @Test
    @Order(0)
    public void test_setSendingFee_notGovernor() {
        BigInteger newSendingFee = new BigInteger("200");
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.setMessageSendingFee(alice, newSendingFee));
        assertThat(thrown.getMessage(), containsString("No authorization - only governor"));
    }

    @Test
    @Order(0)
    public void test_setMaxBytesForSending() throws Throwable {
        BigInteger maxBytesBefore = bridge.maxBytesForSending();
        BigInteger newMaxBytes = new BigInteger("200");
        assertThat(newMaxBytes, is(not(maxBytesBefore)));

        Hash256 tx = bridge.setMaxBytesForSending(newMaxBytes);
        assertThat(bridge.maxBytesForSending(), is(newMaxBytes));

        Notification notification = neow3j.getApplicationLog(tx).send().getApplicationLog().getFirstExecution()
                .getFirstNotification();
        assertThat(notification.getContract(), is(bridge.getScriptHash()));
        assertThat(notification.getEventName(), is("MessageMaxBytesForSendingChange"));
        assertThat(notification.getState().getList(), hasSize(1));
        assertThat(notification.getState().getList().get(0).getInteger(), is(newMaxBytes));

        // revert the state for further tests
        bridge.setMaxBytesForSending(maxBytesBefore);
    }

    @Test
    @Order(0)
    public void test_setMaxBytesForSending_invalidValue() {
        BigInteger invalidMaxBytes = BigInteger.ZERO;
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.setMaxBytesForSending(invalidMaxBytes));
        assertThat(thrown.getMessage(), containsString("Max bytes for sending must be positive"));
    }

    @Test
    @Order(0)
    public void test_setMaxBytesForSending_notGovernor() {
        BigInteger newMaxBytes = new BigInteger("200");
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.setMaxBytesForSending(alice, newMaxBytes));
        assertThat(thrown.getMessage(), containsString("No authorization - only governor"));
    }

    @Test
    @Order(0)
    public void test_setMaxNrMessagesForStoring() throws Throwable {
        BigInteger maxNrMessagesBefore = bridge.maxNrMessagesForStoring();
        BigInteger newMaxNrMessages = new BigInteger("20");
        assertThat(newMaxNrMessages, is(not(maxNrMessagesBefore)));

        Hash256 tx = bridge.setMaxNrMessagesForStoring(newMaxNrMessages);
        assertThat(bridge.maxNrMessagesForStoring(), is(newMaxNrMessages));

        Notification notification = neow3j.getApplicationLog(tx).send().getApplicationLog().getFirstExecution()
                .getFirstNotification();
        assertThat(notification.getContract(), is(bridge.getScriptHash()));
        assertThat(notification.getEventName(), is("MaxNrMessagesForStoringChange"));
        assertThat(notification.getState().getList(), hasSize(1));
        assertThat(notification.getState().getList().get(0).getInteger(), is(newMaxNrMessages));

        // revert the state for further tests
        bridge.setMaxNrMessagesForStoring(maxNrMessagesBefore);
    }

    @Test
    @Order(0)
    public void test_setMaxNrMessageForStoring_invalidValue() {
        BigInteger invalidMaxNrMessages = BigInteger.ZERO;
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.setMaxNrMessagesForStoring(invalidMaxNrMessages));
        assertThat(thrown.getMessage(), containsString("Max number of messages for storing must be positive"));
    }

    @Test
    @Order(0)
    public void test_setMaxNrMessagesForStoring_notGovernor() {
        BigInteger newMaxNrMessages = new BigInteger("20");
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.setMaxNrMessagesForStoring(alice, newMaxNrMessages));
        assertThat(thrown.getMessage(), containsString("No authorization - only governor"));
    }

    @Test
    @Order(0)
    public void test_setExecutionWindowSeconds() throws Throwable {
        BigInteger execWindowSecondsBefore = bridge.executionWindowSeconds();
        BigInteger newExecWindowSeconds = new BigInteger("3600");
        assertThat(newExecWindowSeconds, is(not(execWindowSecondsBefore)));

        Hash256 tx = bridge.setExecutionWindowSeconds(newExecWindowSeconds);
        assertThat(bridge.executionWindowSeconds(), is(newExecWindowSeconds));

        Notification notification = neow3j.getApplicationLog(tx).send().getApplicationLog().getFirstExecution()
                .getFirstNotification();
        assertThat(notification.getContract(), is(bridge.getScriptHash()));
        assertThat(notification.getEventName(), is("ExecutionWindowSecondsChange"));
        assertThat(notification.getState().getList(), hasSize(1));
        assertThat(notification.getState().getList().get(0).getInteger(), is(newExecWindowSeconds));

        // revert the state for further tests
        bridge.setExecutionWindowSeconds(execWindowSecondsBefore);
    }

    @Test
    @Order(0)
    public void test_setExecutionWindowSeconds_invalidValue() {
        BigInteger invalidExecWindowSeconds = BigInteger.ZERO;
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.setExecutionWindowSeconds(invalidExecWindowSeconds));
        assertThat(thrown.getMessage(), containsString("Execution window seconds must be positive"));
    }

    @Test
    @Order(0)
    public void test_setExecutionWindowSeconds_notGovernor() {
        BigInteger newExecWindowSeconds = new BigInteger("3600");
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.setExecutionWindowSeconds(alice, newExecWindowSeconds));
        assertThat(thrown.getMessage(), containsString("No authorization - only governor"));
    }

    @Test
    @Order(0)
    public void test_setExecutionManager() throws Throwable {
        Hash160 executionManagerBefore = bridge.messageExecutionManager();
        Hash160 newExecutionManager = testContract;
        assertThat(executionManagerBefore, is(not(newExecutionManager)));

        Hash256 tx = bridge.setMessageExecutionManager(newExecutionManager);
        assertThat(bridge.messageExecutionManager(), is(newExecutionManager));

        Notification notification = neow3j.getApplicationLog(tx).send().getApplicationLog().getFirstExecution()
                .getFirstNotification();
        assertThat(notification.getContract(), is(bridge.getScriptHash()));
        assertThat(notification.getEventName(), is("MessageExecutionManagerChange"));
        assertThat(notification.getState().getList(), hasSize(1));
        assertThat(Hash160.fromAddress(notification.getState().getList().get(0).getAddress()), is(newExecutionManager));

        // revert the state for further tests
        bridge.setMessageExecutionManager(executionManagerBefore);
    }

    @Test
    @Order(0)
    public void test_setExecutionManager_notContract() {
        Hash160 newExecutionManager = new Hash160("0x1253c2c30b51514e805ddae9ff34df1dc67871b8");
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.setMessageExecutionManager(newExecutionManager));
        assertThat(thrown.getMessage(), containsString("Execution manager must be a contract"));
    }

    @Test
    @Order(0)
    public void test_setExecutionManager_notGovernor() {
        Hash160 newExecutionManager = testContract;
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.setMessageExecutionManager(alice, newExecutionManager));
        assertThat(thrown.getMessage(), containsString("No authorization - only governor"));
    }

    // endregion

}
