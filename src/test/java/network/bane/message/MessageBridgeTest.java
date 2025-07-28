package network.bane.message;

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
import network.bane.util.MessageHelper;
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
import java.util.Map;

import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.utils.Numeric.toHexStringNoPrefix;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static network.bane.util.MessageHelper.createN3MessageHash;
import static network.bane.util.MessageHelper.getMessageStorEvents;
import static network.bane.util.TestHelper.concatAndKeccak256;
import static network.bane.util.TestHelper.governor;
import static network.bane.util.TestHelper.securityGuard;
import static network.bane.util.TestHelper.signMsg;
import static network.bane.util.TestHelper.validator1;
import static network.bane.util.TestHelper.validator2;
import static network.bane.util.TestHelper.validator3;
import static network.bane.util.TestHelper.validator4;
import static network.bane.util.TestHelper.validator5;
import static network.bane.util.helper.DefaultTestValues.MANAGEMENT_CONTRACT_HASH;
import static network.bane.util.helper.PrintHelper.printTransactionFee;
import static network.bane.util.helper.TestHelper.alice;
import static network.bane.util.helper.TestHelper.createBridgeManagementDeployConfig;
import static network.bane.util.helper.TestHelper.createMessageBridgeDeployConfig;
import static network.bane.util.helper.TestHelper.getNextN3Nonce;
import static network.bane.util.helper.TestHelper.management;
import static network.bane.util.helper.TestHelper.messageBridge;
import static network.bane.util.helper.TestHelper.neow3j;
import static network.bane.util.helper.TestHelper.setup;
import static network.bane.util.helper.TestHelper.setupMessageBridge;
import static network.bane.util.helper.TestHelper.setupTestContract;
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
        contracts = {BridgeManagementContract.class, MessageBridgeContract.class, TestContract.class},
        batchFile = "setup.batch"
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MessageBridgeTest {

    @RegisterExtension
    public static final ContractTestExtension ext = new ContractTestExtension();

    @BeforeAll
    public static void setUp() throws Throwable {
        setup(ext);
        setupMessageBridge(ext);
        setupTestContract(ext);

        // This test requires the constant MANAGEMENT_CONTRACT_HASH to be set correctly. The execution manager
        // requires the contract address at deployment time, so we cannot change it later.
        if (!MANAGEMENT_CONTRACT_HASH.equals(management.getScriptHash())) {
            throw new RuntimeException(format("The management contract hash is not set correctly. Update the script " +
                    "hash to 0x%s.", management.getScriptHash()));
        }

        messageBridge.unpause();
    }

    @DeployConfig(BridgeManagementContract.class)
    public static DeployConfiguration deployConfigManagement() {
        return createBridgeManagementDeployConfig();
    }

    @DeployConfig(MessageBridgeContract.class)
    public static DeployConfiguration deployConfigMessageBridge() {
        return createMessageBridgeDeployConfig();
    }

    // region pause

    @Test
    @Order(0)
    public void test_pause_governor() throws Throwable {
        assertFalse(messageBridge.isPaused());
        messageBridge.pause(governor);
        assertTrue(messageBridge.isPaused());

        // revert the state for further tests
        messageBridge.unpause();
        assertFalse(messageBridge.isPaused());
    }

    @Test
    @Order(0)
    public void test_pause_securityGuard() throws Throwable {
        assertFalse(messageBridge.isPaused());
        messageBridge.pause(securityGuard);
        assertTrue(messageBridge.isPaused());

        // revert the state for further tests
        messageBridge.unpause();
    }

    @Test
    @Order(0)
    public void test_pause_notGovernor() throws IOException {
        assertFalse(messageBridge.isPaused());
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.pause(alice));
        assertThat(thrown.getMessage(), containsString("No authorization - only governor or security guard"));
    }

    @Test
    @Order(0)
    public void test_unpause_notGovernor() throws Throwable {
        messageBridge.pause();
        assertTrue(messageBridge.isPaused());
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.unpause(alice));
        assertThat(thrown.getMessage(), containsString("No authorization - only governor"));

        // revert the state for further tests
        messageBridge.unpause();
    }

    // endregion
    // region store

    @Test
    @Order(0)
    public void test_storeMessages_1_notRelayer() throws IOException {
        // This test only works if it is the first test in the order of storing messages, due to the use of raw data for
        // the nonce, to and amount.
        BigInteger nonce = getNextN3Nonce(); // Necessary if other tests are run besides this one.
        BigInteger timestamp = new BigInteger("1753000000");
        Hash160 sender = alice.getScriptHash();
        String msgBytes = "0x1234567890abcdef";

        N3MessageMetadataExecDto metadata = new N3MessageMetadataExecDto(timestamp, sender, true);
        String msgHash1 = createN3MessageHash(nonce, metadata, msgBytes);
        N3MessageDto n3MessageDto = new N3MessageDto(metadata, msgBytes);

        String root = concatAndKeccak256(Hash256.ZERO.toString(), msgHash1);
        List<Account> validators = asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter messageEnvelope = array(integer(nonce), n3MessageDto.toContractParameter(messageBridge));
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.storeMessages(governor, root, signMsg(validators, root), array(messageEnvelope)));
        assertThat(thrown.getMessage(), containsString("No authorization - only relayer"));
    }

    @Test
    @Order(0)
    public void test_storeMessages_1_bridgePaused() throws Throwable {
        messageBridge.pause();
        assertTrue(messageBridge.isPaused());

        // This test only works if it is the first test in the order of storing messages, due to the use of raw data for
        // the nonce, to and amount.
        BigInteger nonce = getNextN3Nonce(); // Necessary if other tests are run besides this one.
        BigInteger timestamp = new BigInteger("1753000000");
        Hash160 sender = alice.getScriptHash();
        String msgBytes = "0x1234567890abcdef";

        N3MessageMetadataExecDto metadata = new N3MessageMetadataExecDto(timestamp, sender, true);
        String msgHash1 = createN3MessageHash(nonce, metadata, msgBytes);
        N3MessageDto n3MessageDto = new N3MessageDto(metadata, msgBytes);

        String root = concatAndKeccak256(Hash256.ZERO.toString(), msgHash1);
        List<Account> validators = asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter messageEnvelope = array(integer(nonce), n3MessageDto.toContractParameter(messageBridge));
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.storeMessages(root, signMsg(validators, root), array(messageEnvelope)));
        assertThat(thrown.getMessage(), containsString("Contract paused"));

        messageBridge.unpause();
    }

    @Test
    @Order(0)
    public void test_storeMessages_1_messageBridgePaused() throws Throwable {
        messageBridge.pause();
        assertTrue(messageBridge.isPaused());

        // This test only works if it is the first test in the order of storing messages, due to the use of raw data for
        // the nonce, to and amount.
        BigInteger nonce = getNextN3Nonce(); // Necessary if other tests are run besides this one.
        BigInteger timestamp = new BigInteger("1753000000");
        Hash160 sender = alice.getScriptHash();
        String msgBytes = "0x1234567890abcdef";

        N3MessageMetadataExecDto metadata = new N3MessageMetadataExecDto(timestamp, sender, true);
        String msgHash1 = createN3MessageHash(nonce, metadata, msgBytes);
        N3MessageDto n3MessageDto = new N3MessageDto(metadata, msgBytes);

        String root = concatAndKeccak256(Hash256.ZERO.toString(), msgHash1);
        List<Account> validators = asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter messageEnvelope = array(integer(nonce), n3MessageDto.toContractParameter(messageBridge));
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.storeMessages(root, signMsg(validators, root), array(messageEnvelope)));
        assertThat(thrown.getMessage(), containsString("Contract paused"));

        messageBridge.unpause();
    }

    @Test
    @Order(0)
    public void test_storeMessages_1_noMessages() throws Throwable {
        String dummyRoot = "";
        List<Account> validators = asList(validator1);
        Map<ContractParameter, ContractParameter> dummyValidatorSigMap = signMsg(validators, dummyRoot);
        ContractParameter emptyN3MsgEnvelopeArray = array();

        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.storeMessages(dummyRoot, dummyValidatorSigMap, emptyN3MsgEnvelopeArray));
        assertThat(thrown.getMessage(), containsString("At least one message required"));
    }

    @Test
    @Order(0)
    public void test_storeMessage_1_incorrectNextNonce() throws IOException {
        // This test only works if it is the first test in the order of storing messages, due to the use of raw data for
        // the nonce, to and amount.
        BigInteger nonce = getNextN3Nonce().add(BigInteger.ONE);
        BigInteger timestamp = new BigInteger("1753000000");
        Hash160 sender = alice.getScriptHash();
        String msgBytes = "0x1234567890abcdef";

        N3MessageMetadataExecDto metadata = new N3MessageMetadataExecDto(timestamp, sender, true);
        String msgHash1 = createN3MessageHash(nonce, metadata, msgBytes);
        N3MessageDto n3MessageDto = new N3MessageDto(metadata, msgBytes);

        String root = concatAndKeccak256(Hash256.ZERO.toString(), msgHash1);
        List<Account> validators = asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter messageEnvelope = array(integer(nonce), n3MessageDto.toContractParameter(messageBridge));
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.storeMessages(root, signMsg(validators, root), messageEnvelope));
        assertThat(thrown.getMessage(), containsString("Provided messages are not subsequent"));
    }

    @Test
    @Order(0)
    public void test_storeMessage_1_invalidRoot() throws IOException {
        // This test only works if it is the first test in the order of storing messages, due to the use of raw data for
        // the nonce, to and amount.
        BigInteger nonce = getNextN3Nonce(); // Necessary if other tests are run besides this one.
        BigInteger timestamp = new BigInteger("1753000000");
        Hash160 sender = alice.getScriptHash();
        String msgBytes = "0x1234567890abcdef";

        N3MessageMetadataExecDto metadata = new N3MessageMetadataExecDto(timestamp, sender, true);
        String msgHash1 = createN3MessageHash(nonce, metadata, msgBytes);
        N3MessageDto n3MessageDto = new N3MessageDto(metadata, msgBytes);

        String invalidRoot = concatAndKeccak256(Hash256.ZERO.toString(), msgHash1) + "01"; // Invalid root
        List<Account> validators = asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter messageEnvelope = array(integer(nonce), n3MessageDto.toContractParameter(messageBridge));
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                // Root signed by validators, but not matching the messages.
                () -> messageBridge.storeMessages(invalidRoot, signMsg(validators, invalidRoot),
                        array(messageEnvelope)));
        assertThat(thrown.getMessage(), containsString("Invalid root"));
    }

    @Test
    @Order(0)
    public void test_storeMessage_1_insufficientNrSigs() throws IOException {
        // This test only works if it is the first test in the order of storing messages, due to the use of raw data for
        // the nonce, to and amount.
        BigInteger nonce = getNextN3Nonce(); // Necessary if other tests are run besides this one.
        BigInteger timestamp = new BigInteger("1753000000");
        Hash160 sender = alice.getScriptHash();
        String msgBytes = "0x1234567890abcdef";
        boolean storeResult = false;

        N3MessageMetadataExecDto metadata = new N3MessageMetadataExecDto(timestamp, sender, storeResult);
        String msgHash1 = createN3MessageHash(nonce, metadata, msgBytes);
        N3MessageDto n3MessageDto = new N3MessageDto(metadata, msgBytes);

        String root = concatAndKeccak256(Hash256.ZERO.toString(), msgHash1);
        List<Account> validators = asList(validator1, validator2, validator3, validator4);
        ContractParameter messageEnvelope = array(integer(nonce), n3MessageDto.toContractParameter(messageBridge));
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.storeMessages(root, signMsg(validators, root), array(messageEnvelope)));
        assertThat(thrown.getMessage(), containsString("Insufficient signatures"));
    }

    @Test
    @Order(0)
    public void test_storeMessage_1_failingSigVerification() throws IOException {
        // This test only works if it is the first test in the order of storing messages, due to the use of raw data for
        // the nonce, to and amount.
        BigInteger nonce = getNextN3Nonce(); // Necessary if other tests are run besides this one.
        BigInteger timestamp = new BigInteger("1753000000");
        Hash160 sender = alice.getScriptHash();
        String msgBytes = "0x1234567890abcdef";

        N3MessageMetadataExecDto metadata = new N3MessageMetadataExecDto(timestamp, sender, true);
        String msgHash1 = createN3MessageHash(nonce, metadata, msgBytes);
        N3MessageDto n3MessageDto = new N3MessageDto(metadata, msgBytes);

        String root = concatAndKeccak256(Hash256.ZERO.toString(), msgHash1);
        List<Account> validators = asList(validator1, validator2, validator3, validator4, alice); // Non-validator sig
        ContractParameter messageEnvelope = array(integer(nonce), n3MessageDto.toContractParameter(messageBridge));
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.storeMessages(root, signMsg(validators, root), array(messageEnvelope)));
        assertThat(thrown.getMessage(), containsString("Invalid validator signatures"));
    }

    @Test
    @Order(1)
    public void test_storeMessage_1() throws Throwable {
        // This test only works if it is the first test in the order of storing messages, due to the use of raw data for
        // the nonce, to and amount.
        BigInteger nonce = getNextN3Nonce(); // Necessary if other tests are run besides this one.
        BigInteger timestamp = new BigInteger("1753000000");
        Hash160 sender = alice.getScriptHash();
        String msgBytes = "0x1234567890abcdef";

        // 0000000000000000000000000000000000000000000000000000000000000001 nonce hex padded
        // 00000000000000000000000000000000000000000000000000000000687ca840 1753000000 hex padded
        // 69ecca587293047be4c59159bf8bc399985c160d alice
        // efcdab9078563412 executable code reversed

        N3MessageMetadataExecDto metadata = new N3MessageMetadataExecDto(timestamp, sender, true);
        String msgHash1 = createN3MessageHash(nonce, metadata, msgBytes);
        N3MessageDto n3MessageDto = new N3MessageDto(metadata, msgBytes);

        String root = concatAndKeccak256(Hash256.ZERO.toString(), msgHash1);
        List<Account> validators = asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter messageEnvelope = array(integer(nonce), n3MessageDto.toContractParameter(messageBridge));
        Hash256 txHash = messageBridge.storeMessages(root, signMsg(validators, root), array(messageEnvelope));
        printTransactionFee(neow3j, "tx with 1 message", txHash);
        List<MessageHelper.N3MessageStoreEvent> n3MessageStoreEvents = getMessageStorEvents(txHash, neow3j,
                messageBridge.getScriptHash());
        assertThat(n3MessageStoreEvents, hasSize(1));
        MessageHelper.N3MessageStoreEvent n3MessageStoreEvent = n3MessageStoreEvents.get(0);
        assertThat(n3MessageStoreEvent.nonce, is(nonce));
        N3MessageMetadataDto expectedMetadata = metadata;
        assertThat(n3MessageStoreEvent.metadataSerializedHex,
                is(toHexStringNoPrefix(expectedMetadata.serialize(messageBridge))));

        N3MessageDto message = messageBridge.getMessage(nonce);
        assertThat(message, is(n3MessageDto));

        assertTrue(messageBridge.isPending(nonce));
    }

    // endregion
    // region config

    @Test
    @Order(0)
    public void test_setSendingFee() throws Throwable {
        BigInteger sendingFeeBefore = messageBridge.sendingFee();
        BigInteger newSendingFee = new BigInteger("200");
        assertThat(newSendingFee, is(not(sendingFeeBefore)));

        Hash256 tx = messageBridge.setSendingFee(newSendingFee);
        assertThat(messageBridge.sendingFee(), is(newSendingFee));

        Notification notification = neow3j.getApplicationLog(tx).send().getApplicationLog().getFirstExecution()
                .getFirstNotification();
        assertThat(notification.getContract(), is(messageBridge.getScriptHash()));
        assertThat(notification.getEventName(), is("SendingFeeChange"));
        assertThat(notification.getState().getList(), hasSize(1));
        assertThat(notification.getState().getList().get(0).getInteger(), is(newSendingFee));

        // revert the state for further tests
        messageBridge.setSendingFee(sendingFeeBefore);
    }

    @Test
    @Order(0)
    public void test_setSendingFee_invalidValue() {
        BigInteger invalidSendingFee = new BigInteger("-1");
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.setSendingFee(invalidSendingFee));
        assertThat(thrown.getMessage(), containsString("Sending fee must be nonnegative"));
    }

    @Test
    @Order(0)
    public void test_setSendingFee_notGovernor() {
        BigInteger newSendingFee = new BigInteger("200");
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.setSendingFee(alice, newSendingFee));
        assertThat(thrown.getMessage(), containsString("No authorization - only governor"));
    }

    @Test
    @Order(0)
    public void test_setMaxBytesForSending() throws Throwable {
        BigInteger maxBytesBefore = messageBridge.maxBytesForSending();
        BigInteger newMaxBytes = new BigInteger("200");
        assertThat(newMaxBytes, is(not(maxBytesBefore)));

        Hash256 tx = messageBridge.setMaxBytesForSending(newMaxBytes);
        assertThat(messageBridge.maxBytesForSending(), is(newMaxBytes));

        Notification notification = neow3j.getApplicationLog(tx).send().getApplicationLog().getFirstExecution()
                .getFirstNotification();
        assertThat(notification.getContract(), is(messageBridge.getScriptHash()));
        assertThat(notification.getEventName(), is("MaxBytesForSendingChange"));
        assertThat(notification.getState().getList(), hasSize(1));
        assertThat(notification.getState().getList().get(0).getInteger(), is(newMaxBytes));

        // revert the state for further tests
        messageBridge.setMaxBytesForSending(maxBytesBefore);
    }

    @Test
    @Order(0)
    public void test_setMaxBytesForSending_invalidValue() {
        BigInteger invalidMaxBytes = BigInteger.ZERO;
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.setMaxBytesForSending(invalidMaxBytes));
        assertThat(thrown.getMessage(), containsString("Max bytes for sending must be positive"));
    }

    @Test
    @Order(0)
    public void test_setMaxBytesForSending_notGovernor() {
        BigInteger newMaxBytes = new BigInteger("200");
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.setMaxBytesForSending(alice, newMaxBytes));
        assertThat(thrown.getMessage(), containsString("No authorization - only governor"));
    }

    @Test
    @Order(0)
    public void test_setMaxNrMessagesForStoring() throws Throwable {
        BigInteger maxNrMessagesBefore = messageBridge.maxNrMessagesForStoring();
        BigInteger newMaxNrMessages = new BigInteger("20");
        assertThat(newMaxNrMessages, is(not(maxNrMessagesBefore)));

        Hash256 tx = messageBridge.setMaxNrMessagesForStoring(newMaxNrMessages);
        assertThat(messageBridge.maxNrMessagesForStoring(), is(newMaxNrMessages));

        Notification notification = neow3j.getApplicationLog(tx).send().getApplicationLog().getFirstExecution()
                .getFirstNotification();
        assertThat(notification.getContract(), is(messageBridge.getScriptHash()));
        assertThat(notification.getEventName(), is("MaxNrMessagesForStoringChange"));
        assertThat(notification.getState().getList(), hasSize(1));
        assertThat(notification.getState().getList().get(0).getInteger(), is(newMaxNrMessages));

        // revert the state for further tests
        messageBridge.setMaxNrMessagesForStoring(maxNrMessagesBefore);
    }

    @Test
    @Order(0)
    public void test_setMaxNrMessageForStoring_invalidValue() {
        BigInteger invalidMaxNrMessages = BigInteger.ZERO;
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.setMaxNrMessagesForStoring(invalidMaxNrMessages));
        assertThat(thrown.getMessage(), containsString("Max number of messages for storing must be positive"));
    }

    @Test
    @Order(0)
    public void test_setMaxNrMessagesForStoring_notGovernor() {
        BigInteger newMaxNrMessages = new BigInteger("20");
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.setMaxNrMessagesForStoring(alice, newMaxNrMessages));
        assertThat(thrown.getMessage(), containsString("No authorization - only governor"));
    }

    @Test
    @Order(0)
    public void test_setExecutionWindowSeconds() throws Throwable {
        BigInteger execWindowSecondsBefore = messageBridge.executionWindowSeconds();
        BigInteger newExecWindowSeconds = new BigInteger("3600");
        assertThat(newExecWindowSeconds, is(not(execWindowSecondsBefore)));

        Hash256 tx = messageBridge.setExecutionWindowSeconds(newExecWindowSeconds);
        assertThat(messageBridge.executionWindowSeconds(), is(newExecWindowSeconds));

        Notification notification = neow3j.getApplicationLog(tx).send().getApplicationLog().getFirstExecution()
                .getFirstNotification();
        assertThat(notification.getContract(), is(messageBridge.getScriptHash()));
        assertThat(notification.getEventName(), is("ExecutionWindowSecondsChange"));
        assertThat(notification.getState().getList(), hasSize(1));
        assertThat(notification.getState().getList().get(0).getInteger(), is(newExecWindowSeconds));

        // revert the state for further tests
        messageBridge.setExecutionWindowSeconds(execWindowSecondsBefore);
    }

    @Test
    @Order(0)
    public void test_setExecutionWindowSeconds_invalidValue() {
        BigInteger invalidExecWindowSeconds = BigInteger.ZERO;
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.setExecutionWindowSeconds(invalidExecWindowSeconds));
        assertThat(thrown.getMessage(), containsString("Execution window seconds must be positive"));
    }

    @Test
    @Order(0)
    public void test_setExecutionWindowSeconds_notGovernor() {
        BigInteger newExecWindowSeconds = new BigInteger("3600");
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.setExecutionWindowSeconds(alice, newExecWindowSeconds));
        assertThat(thrown.getMessage(), containsString("No authorization - only governor"));
    }

    @Test
    @Order(0)
    public void test_setExecutionManager() throws Throwable {
        Hash160 executionManagerBefore = messageBridge.executionManager();
        Hash160 newExecutionManager = testContract;
        assertThat(executionManagerBefore, is(not(newExecutionManager)));

        Hash256 tx = messageBridge.setExecutionManager(newExecutionManager);
        assertThat(messageBridge.executionManager(), is(newExecutionManager));

        Notification notification = neow3j.getApplicationLog(tx).send().getApplicationLog().getFirstExecution()
                .getFirstNotification();
        assertThat(notification.getContract(), is(messageBridge.getScriptHash()));
        assertThat(notification.getEventName(), is("ExecutionManagerChange"));
        assertThat(notification.getState().getList(), hasSize(1));
        assertThat(Hash160.fromAddress(notification.getState().getList().get(0).getAddress()), is(newExecutionManager));
    }

    @Test
    @Order(0)
    public void test_setExecutionManager_notContract() {
        Hash160 newExecutionManager = new Hash160("0x1253c2c30b51514e805ddae9ff34df1dc67871b8");
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.setExecutionManager(newExecutionManager));
        assertThat(thrown.getMessage(), containsString("Execution manager must be a contract"));
    }

    @Test
    @Order(0)
    public void test_setExecutionManager_notGovernor() {
        Hash160 newExecutionManager = testContract;
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.setExecutionManager(alice, newExecutionManager));
        assertThat(thrown.getMessage(), containsString("No authorization - only governor"));
    }

    // endregion

}
