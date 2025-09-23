package network.bane.message;

import io.neow3j.contract.ContractManagement;
import io.neow3j.contract.GasToken;
import io.neow3j.protocol.core.response.NeoApplicationLog;
import io.neow3j.protocol.core.response.Notification;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.protocol.exceptions.InvocationFaultStateException;
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
import network.bane.testhelper.TestContract;
import network.bane.util.MessageHelper;
import network.bane.util.structs.ExecutableStateDto;
import network.bane.util.structs.N3MessageDto;
import network.bane.util.structs.N3MessageMetadataDto;
import network.bane.util.structs.N3MessageMetadataExecDto;
import network.bane.util.structs.N3MessageMetadataResultDto;
import network.bane.util.structs.N3MessageMetadataStoreOnlyDto;
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

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.transaction.AccountSigner.global;
import static io.neow3j.types.ContractParameter.any;
import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.utils.Numeric.cleanHexPrefix;
import static io.neow3j.utils.Numeric.hexStringToByteArray;
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
import static network.bane.util.helper.DefaultTestValues.MESSAGE_BRIDGE_CONTRACT_HASH;
import static network.bane.util.helper.PrintHelper.printTransactionFee;
import static network.bane.util.helper.TestHelper.alice;
import static network.bane.util.helper.TestHelper.createBridgeManagementDeployConfig;
import static network.bane.util.helper.TestHelper.createExecutionManagerDeployConfig;
import static network.bane.util.helper.TestHelper.createMessageBridgeDeployConfig;
import static network.bane.util.helper.TestHelper.executionManager;
import static network.bane.util.helper.TestHelper.gasToken;
import static network.bane.util.helper.TestHelper.getNextEvmNonce;
import static network.bane.util.helper.TestHelper.getNextN3Nonce;
import static network.bane.util.helper.TestHelper.management;
import static network.bane.util.helper.TestHelper.messageBridge;
import static network.bane.util.helper.TestHelper.neow3j;
import static network.bane.util.helper.TestHelper.setup;
import static network.bane.util.helper.TestHelper.setupExecutionManager;
import static network.bane.util.helper.TestHelper.setupMessageBridge;
import static network.bane.util.helper.TestHelper.setupTestContract;
import static network.bane.util.helper.TestHelper.testContract;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ContractTest(
        blockTime = 1,
        contracts = {BridgeManagementContract.class, MessageBridgeContract.class, ExecutionManagerContract.class,
                TestContract.class},
        batchFile = "setup.batch"
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MessageBridgeTest {

    private static final BigInteger UPPER_LIMIT_MAX_MESSAGE_SIZE = new BigInteger("10240");
    private static final BigInteger UPPER_LIMIT_MAX_NR_MESSAGES = new BigInteger("1000");
    private static final BigInteger UPPER_LIMIT_MAX_EXECUTION_WINDOW_MILLIS = new BigInteger("31536000000");

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

        messageBridge.setExecutionManager(executionManager.getScriptHash());

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

    @DeployConfig(ExecutionManagerContract.class)
    public static DeployConfiguration deployConfigExecutionManager() {
        return createExecutionManagerDeployConfig();
    }

    private static BigInteger getBestBlockTime() throws IOException {
        return BigInteger.valueOf(
                neow3j.getBlockHeader(neow3j.getBestBlockHash().send().getBlockHash()).send().getBlock().getTime());
    }

    // region pause

    @Test
    @Order(0)
    public void test_pause_governor() throws Throwable {
        assertFalse(messageBridge.isPaused());
        Hash256 tx = messageBridge.pause(governor);
        assertTrue(messageBridge.isPaused());
        Notification firstEventInPausingTx = neow3j.getApplicationLog(tx).send().getApplicationLog().getFirstExecution()
                .getFirstNotification();
        assertThat(firstEventInPausingTx.getContract(), is(messageBridge.getScriptHash()));
        assertThat(firstEventInPausingTx.getEventName(), is("Pause"));

        // revert the state for further tests
        tx = messageBridge.unpause();
        assertFalse(messageBridge.isPaused());
        Notification firstEventInUnpausingTx = neow3j.getApplicationLog(tx).send().getApplicationLog()
                .getFirstExecution().getFirstNotification();
        assertThat(firstEventInUnpausingTx.getContract(), is(messageBridge.getScriptHash()));
        assertThat(firstEventInUnpausingTx.getEventName(), is("Unpause"));
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
    // region sending

    @Test
    @Order(1)
    public void testSending_storeOnly() throws Throwable {
        BigInteger expectedNextEvmNonce = getNextEvmNonce();

        BigInteger initialBalance = gasToken.getBalanceOf(messageBridge.getScriptHash());
        BigInteger initialUnclaimedFees = messageBridge.unclaimedFees();
        BigInteger sendingFee = messageBridge.sendingFee();

        byte[] rawMessage = hexStringToByteArray("0x1234567890abcdef");
        Hash256 tx = messageBridge.sendMessage(global(alice), rawMessage);

        BigInteger gasBalanceAfter = gasToken.getBalanceOf(messageBridge.getScriptHash());
        assertThat(gasBalanceAfter, is(initialBalance.add(sendingFee)));
        assertThat(messageBridge.unclaimedFees(), is(initialUnclaimedFees.add(sendingFee)));

        NeoApplicationLog.Execution exec = neow3j.getApplicationLog(tx).send().getApplicationLog().getFirstExecution();
        assertThat(exec.getFirstStackItem().getType(), is(StackItemType.INTEGER));
        assertThat(exec.getFirstStackItem().getInteger(), is(expectedNextEvmNonce));

        assertThat(exec.getNotifications(), hasSize(2));
        List<Notification> events = exec.getNotifications();
        assertThat(events, hasSize(2));
        Notification event1 = events.get(0);
        assertThat(event1.getContract(), is(GasToken.SCRIPT_HASH));
        assertThat(event1.getEventName(), is("Transfer"));
        List<StackItem> transferEventState = event1.getState().getList();
        assertThat(Hash160.fromAddress(transferEventState.get(0).getAddress()), is(alice.getScriptHash()));
        assertThat(Hash160.fromAddress(transferEventState.get(1).getAddress()), is(messageBridge.getScriptHash()));
        assertThat(transferEventState.get(2).getInteger(), is(sendingFee));

        Notification event2 = events.get(1);
        assertThat(event2.getContract(), is(messageBridge.getScriptHash()));
        assertThat(event2.getEventName(), is("MessageSend"));
        List<StackItem> eventItems = event2.getState().getList();
        assertThat(eventItems, hasSize(5));
        BigInteger nonce = eventItems.get(0).getInteger();
        assertThat(nonce, is(expectedNextEvmNonce));
    }

    @Test
    @Order(2)
    public void testSending_executable() throws Throwable {
        BigInteger expectedNextEvmNonce = getNextEvmNonce();

        BigInteger initialBalance = gasToken.getBalanceOf(messageBridge.getScriptHash());
        BigInteger initialUnclaimedFees = messageBridge.unclaimedFees();
        BigInteger sendingFee = messageBridge.sendingFee();

        byte[] rawMessage = hexStringToByteArray("0x1234567890abcdef");
        Hash256 tx = messageBridge.sendExecutableMessage(rawMessage, false);

        BigInteger gasBalanceAfter = gasToken.getBalanceOf(messageBridge.getScriptHash());
        assertThat(gasBalanceAfter, is(initialBalance.add(sendingFee)));
        assertThat(messageBridge.unclaimedFees(), is(initialUnclaimedFees.add(sendingFee)));

        NeoApplicationLog.Execution exec = neow3j.getApplicationLog(tx).send().getApplicationLog().getFirstExecution();
        assertThat(exec.getFirstStackItem().getType(), is(StackItemType.INTEGER));
        assertThat(exec.getFirstStackItem().getInteger(), is(expectedNextEvmNonce));

        assertThat(exec.getNotifications(), hasSize(2));
        List<Notification> events = exec.getNotifications();
        assertThat(events, hasSize(2));
        Notification event1 = events.get(0);
        assertThat(event1.getContract(), is(GasToken.SCRIPT_HASH));
        assertThat(event1.getEventName(), is("Transfer"));
        List<StackItem> transferEventState = event1.getState().getList();
        assertThat(Hash160.fromAddress(transferEventState.get(0).getAddress()), is(alice.getScriptHash()));
        assertThat(Hash160.fromAddress(transferEventState.get(1).getAddress()), is(messageBridge.getScriptHash()));
        assertThat(transferEventState.get(2).getInteger(), is(sendingFee));

        Notification event2 = events.get(1);
        assertThat(event2.getContract(), is(messageBridge.getScriptHash()));
        assertThat(event2.getEventName(), is("MessageSend"));
        List<StackItem> eventItems = event2.getState().getList();
        assertThat(eventItems, hasSize(5));
        BigInteger nonce = eventItems.get(0).getInteger();
        assertThat(nonce, is(expectedNextEvmNonce));
    }

    @Test
    @Order(0)
    public void testSending_storeOnly_fail_exceedMaxSize() throws Throwable {
        BigInteger previousMaxMsgSize = messageBridge.maxMessageSize();
        messageBridge.setMaxMessageSize(BigInteger.TEN);

        byte[] rawMessage = hexStringToByteArray("0x1234567890abcdef010203");
        assertThat(rawMessage.length, greaterThan(messageBridge.maxMessageSize().intValue()));
        BigInteger sendingFee = messageBridge.sendingFee();
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.sendMessage(global(alice), rawMessage, alice, sendingFee));
        assertThat(thrown.getMessage(), containsString("Message too large"));

        // Revert the state for further tests
        messageBridge.setMaxMessageSize(previousMaxMsgSize);
    }

    @Test
    @Order(0)
    public void testSending_executable_fail_exceedMaxSize() throws Throwable {
        BigInteger previousMaxMsgSize = messageBridge.maxMessageSize();
        messageBridge.setMaxMessageSize(BigInteger.TEN);

        byte[] rawMessage = hexStringToByteArray("0x1234567890abcdef010203");
        assertThat(rawMessage.length, greaterThan(messageBridge.maxMessageSize().intValue()));
        BigInteger sendingFee = messageBridge.sendingFee();
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.sendExecutableMessage(global(alice), rawMessage, false, alice, sendingFee));
        assertThat(thrown.getMessage(), containsString("Message too large"));

        // Revert the state for further tests
        messageBridge.setMaxMessageSize(previousMaxMsgSize);
    }

    @Test
    @Order(0)
    public void testSending_storeOnly_fail_exceedMaxFee() throws Throwable {
        byte[] rawMessage = hexStringToByteArray("0x1234567890abcdef");
        BigInteger sendingFee = messageBridge.sendingFee();
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.sendMessage(global(alice), rawMessage, alice, sendingFee.subtract(BigInteger.ONE)));
        assertThat(thrown.getMessage(), containsString("Max fee exceeded"));
    }

    @Test
    @Order(0)
    public void testSending_executable_fail_exceedMaxFee() throws Throwable {
        byte[] rawMessage = hexStringToByteArray("0x1234567890abcdef");
        BigInteger sendingFee = messageBridge.sendingFee();
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.sendExecutableMessage(global(alice), rawMessage, false, alice,
                        sendingFee.subtract(BigInteger.ONE)));
        assertThat(thrown.getMessage(), containsString("Max fee exceeded"));
    }

    @Test
    @Order(0)
    public void testSending_storeOnly_fail_prohibitedFeeSponsor() throws Throwable {
        byte[] rawMessage = hexStringToByteArray("0x1234567890abcdef");
        BigInteger sendingFee = messageBridge.sendingFee();
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.sendMessage(global(alice), rawMessage, messageBridge.getScriptHash(), sendingFee));
        assertThat(thrown.getMessage(), containsString("Prohibited 'feeSponsor'"));
    }

    @Test
    @Order(0)
    public void testSending_executable_fail_prohibitedFeeSponsor() throws Throwable {
        byte[] rawMessage = hexStringToByteArray("0x1234567890abcdef");
        BigInteger sendingFee = messageBridge.sendingFee();
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.sendExecutableMessage(global(alice), rawMessage, false,
                        messageBridge.getScriptHash(), sendingFee));
        assertThat(thrown.getMessage(), containsString("Prohibited 'feeSponsor'"));
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

        String currentN3Root = messageBridge.messageEvmToN3Root();
        String root = concatAndKeccak256(currentN3Root, msgHash1);
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

        ExecutableStateDto execState = messageBridge.getExecutableState(nonce);
        assertFalse(execState.executed);
        assertThat(execState.expirationTimestamp, greaterThan(getBestBlockTime()));
    }

    @Test
    @Order(2)
    public void test_storeMessage_checkExecutableState() throws Throwable {
        BigInteger timestamp = new BigInteger("1753000000");
        Hash160 sender = alice.getScriptHash();
        String msgBytes = "0x1234567890abcdef";

        N3MessageMetadataExecDto metadata1 = new N3MessageMetadataExecDto(timestamp, sender, true);
        N3MessageMetadataStoreOnlyDto metadata2 = new N3MessageMetadataStoreOnlyDto(timestamp, sender);
        N3MessageMetadataResultDto metadata3 = new N3MessageMetadataResultDto(timestamp, sender, BigInteger.ONE);
        N3MessageMetadataExecDto metadata4 = metadata1;
        BigInteger nonce1 = getNextN3Nonce();
        BigInteger nonce2 = nonce1.add(BigInteger.ONE);
        BigInteger nonce3 = nonce2.add(BigInteger.ONE);
        BigInteger nonce4 = nonce3.add(BigInteger.ONE);

        N3MessageDto n3MessageDto1 = new N3MessageDto(metadata1, msgBytes);
        N3MessageDto n3MessageDto2 = new N3MessageDto(metadata2, msgBytes);
        N3MessageDto n3MessageDto3 = new N3MessageDto(metadata3, msgBytes);
        N3MessageDto n3MessageDto4 = new N3MessageDto(metadata1, msgBytes);
        String msgHash1 = createN3MessageHash(nonce1, metadata1, msgBytes);
        String msgHash2 = createN3MessageHash(nonce2, metadata2, msgBytes);
        String msgHash3 = createN3MessageHash(nonce3, metadata3, msgBytes);
        String msgHash4 = createN3MessageHash(nonce4, metadata4, msgBytes);

        Hash256 currentRoot = messageBridge.getMessageBridge().evmToN3MessageState.root;

        String root1 = concatAndKeccak256(currentRoot.toString(), msgHash1);
        String root2 = concatAndKeccak256(root1, msgHash2);
        String root3 = concatAndKeccak256(root2, msgHash3);
        String root4 = concatAndKeccak256(root3, msgHash4);

        List<Account> validators = asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter messageEnvelope1 = array(integer(nonce1), n3MessageDto1.toContractParameter(messageBridge));
        ContractParameter messageEnvelope2 = array(integer(nonce2), n3MessageDto2.toContractParameter(messageBridge));
        ContractParameter messageEnvelope3 = array(integer(nonce3), n3MessageDto3.toContractParameter(messageBridge));
        ContractParameter messageEnvelope4 = array(integer(nonce4), n3MessageDto4.toContractParameter(messageBridge));
        Hash256 txHash = messageBridge.storeMessages(root4, signMsg(validators, root4), array(messageEnvelope1,
                messageEnvelope2, messageEnvelope3, messageEnvelope4));
        printTransactionFee(neow3j, "tx with 3 messages", txHash);
        List<MessageHelper.N3MessageStoreEvent> n3MessageStoreEvents = getMessageStorEvents(txHash, neow3j,
                messageBridge.getScriptHash());
        assertThat(n3MessageStoreEvents, hasSize(4));
        MessageHelper.N3MessageStoreEvent n3MessageStoreEvent1 = n3MessageStoreEvents.get(0);
        assertThat(n3MessageStoreEvent1.nonce, is(nonce1));
        MessageHelper.N3MessageStoreEvent n3MessageStoreEvent2 = n3MessageStoreEvents.get(1);
        assertThat(n3MessageStoreEvent2.nonce, is(nonce2));
        MessageHelper.N3MessageStoreEvent n3MessageStoreEvent3 = n3MessageStoreEvents.get(2);
        assertThat(n3MessageStoreEvent3.nonce, is(nonce3));
        MessageHelper.N3MessageStoreEvent n3MessageStoreEvent4 = n3MessageStoreEvents.get(3);
        assertThat(n3MessageStoreEvent4.nonce, is(nonce4));

        N3MessageDto message1 = messageBridge.getMessage(nonce1);
        assertThat(message1, is(n3MessageDto1));
        N3MessageDto message2 = messageBridge.getMessage(nonce2);
        assertThat(message2, is(n3MessageDto2));
        N3MessageDto message3 = messageBridge.getMessage(nonce3);
        assertThat(message3, is(n3MessageDto3));
        N3MessageDto message4 = messageBridge.getMessage(nonce4);
        assertThat(message4, is(n3MessageDto4));

        ExecutableStateDto executableState1 = messageBridge.getExecutableState(nonce1);
        assertFalse(executableState1.executed);
        assertThat(executableState1.expirationTimestamp, greaterThan(getBestBlockTime()));

        IllegalStateException thrown2 = assertThrows(IllegalStateException.class,
                () -> messageBridge.getExecutableState(nonce2));
        assertThat(thrown2.getMessage(), containsString("Executable state not found"));
        IllegalStateException thrown3 = assertThrows(IllegalStateException.class,
                () -> messageBridge.getExecutableState(nonce3));
        assertThat(thrown3.getMessage(), containsString("Executable state not found"));

        ExecutableStateDto executableState4 = messageBridge.getExecutableState(nonce4);
        assertFalse(executableState4.executed);
        assertThat(executableState4.expirationTimestamp, greaterThan(getBestBlockTime()));
    }

    @Test
    @Order(1)
    public void test_storeMessage_checkEvmResult() throws Throwable {
        // Send dummy message to EVM to use as related nonce for checking its dummy result.
        Hash256 tx = messageBridge.sendExecutableMessage(hexStringToByteArray("0x0a"), true);
        BigInteger nonceOfMsgSentToEvm = neow3j.getApplicationLog(tx).send().getApplicationLog().getFirstExecution()
                .getFirstStackItem().getInteger();

        // This test only works if it is the first test in the order of storing messages, due to the use of raw data for
        // the nonce, to and amount.
        BigInteger nonce = getNextN3Nonce(); // Necessary if other tests are run besides this one.
        BigInteger timestamp = new BigInteger("1753000000");
        Hash160 sender = alice.getScriptHash();
        String msgBytes = "0x12123434";

        N3MessageMetadataResultDto metadata = new N3MessageMetadataResultDto(timestamp, sender, nonceOfMsgSentToEvm);

        String msgHash1 = createN3MessageHash(nonce, metadata, msgBytes);
        N3MessageDto n3MessageDto = new N3MessageDto(metadata, msgBytes);

        String currentN3Root = messageBridge.messageEvmToN3Root();
        String root = concatAndKeccak256(currentN3Root, msgHash1);
        List<Account> validators = asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter messageEnvelope = array(integer(nonce), n3MessageDto.toContractParameter(messageBridge));
        Hash256 txHash = messageBridge.storeMessages(root, signMsg(validators, root), array(messageEnvelope));
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

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> messageBridge.getExecutableState(nonce));
        assertThat(thrown.getMessage(), containsString("Executable state not found"));

        assertThat(messageBridge.getEvmResultNonce(nonceOfMsgSentToEvm), is(nonce));
        assertThat(toHexStringNoPrefix(messageBridge.getEvmResult(nonceOfMsgSentToEvm)), is(cleanHexPrefix(msgBytes)));
    }

    // endregion
    // region execution

    @Test
    @Order(0)
    public void test_serializeCall() throws IOException {
        byte[] n3Call = messageBridge.serializeCall(GasToken.SCRIPT_HASH, "symbol", CallFlags.READ_STATES, asList());
        assertThat(toHexStringNoPrefix(n3Call),
                is("40042814cf76e28bd0062c4a478ee35561011319f3cfa4d2280673796d626f6c2101014000"));
    }

    @Test
    @Order(0)
    public void test_isValidCall() throws IOException {
        byte[] serializedCall = hexStringToByteArray(
                "40042814cf76e28bd0062c4a478ee35561011319f3cfa4d2280673796d626f6c2101014000");
        assertTrue(messageBridge.isValidCall(serializedCall));

        byte[] n3CallWithZeroTarget = hexStringToByteArray(
                "400428140000000000000000000000000000000000000000280673796d626f6c2101014000");
        assertFalse(messageBridge.isValidCall(n3CallWithZeroTarget));

        byte[] malformedCall = hexStringToByteArray("0xab100c");
        InvocationFaultStateException thrown = assertThrows(InvocationFaultStateException.class,
                () -> messageBridge.isValidCall(malformedCall));
        assertThat(thrown.getMessage(), containsString("invalid format"));
    }

    @Test
    @Order(0)
    public void test_isAllowedCall() throws IOException {
        byte[] allowedN3Call =
                hexStringToByteArray("40042814cf76e28bd0062c4a478ee35561011319f3cfa4d2280673796d626f6c2101014000");
        assertTrue(messageBridge.isAllowedCall(allowedN3Call));

        byte[] disallowedN3Call = messageBridge.serializeCall(ContractManagement.SCRIPT_HASH, "isContract",
                CallFlags.READ_ONLY, asList(hash160(GasToken.SCRIPT_HASH)));
        assertFalse(messageBridge.isAllowedCall(disallowedN3Call));
    }

    @Test
    @Order(2)
    public void test_execute_failWhenContractPaused() throws Throwable {
        byte[] n3FuncCall = messageBridge.serializeCall(GasToken.SCRIPT_HASH, "symbol", CallFlags.ALL,
                asList());
        BigInteger nonce = messageBridge.storeMessage(n3FuncCall);

        messageBridge.pause();
        assertTrue(messageBridge.isPaused());

        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.executeMessage(global(alice), nonce));
        assertThat(thrown.getMessage(), containsString("Contract paused"));

        messageBridge.unpause();
    }

    @Test
    @Order(2)
    public void test_execute_failWhenExecutingPaused() throws Throwable {
        byte[] n3FuncCall = messageBridge.serializeCall(GasToken.SCRIPT_HASH, "symbol", CallFlags.ALL,
                asList());
        BigInteger nonce = messageBridge.storeMessage(n3FuncCall);

        Hash256 tx = messageBridge.pauseExecuting();
        assertTrue(messageBridge.executingIsPaused());
        Notification firstEventInPausingTx = neow3j.getApplicationLog(tx).send().getApplicationLog().getFirstExecution()
                .getFirstNotification();
        assertThat(firstEventInPausingTx.getContract(), is(messageBridge.getScriptHash()));
        assertThat(firstEventInPausingTx.getEventName(), is("ExecutingPause"));

        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.executeMessage(global(alice), nonce));
        assertThat(thrown.getMessage(), containsString("Executing paused"));

        tx = messageBridge.unpauseExecuting();
        Notification firstEventInUnpausingTx = neow3j.getApplicationLog(tx).send().getApplicationLog()
                .getFirstExecution()
                .getFirstNotification();
        assertThat(firstEventInUnpausingTx.getContract(), is(messageBridge.getScriptHash()));
        assertThat(firstEventInUnpausingTx.getEventName(), is("ExecutingUnpause"));
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
    public void test_setMaxMessageSize() throws Throwable {
        BigInteger maxBytesBefore = messageBridge.maxMessageSize();
        BigInteger newMaxBytes = new BigInteger("200");
        assertThat(newMaxBytes, is(not(maxBytesBefore)));

        Hash256 tx = messageBridge.setMaxMessageSize(newMaxBytes);
        assertThat(messageBridge.maxMessageSize(), is(newMaxBytes));

        Notification notification = neow3j.getApplicationLog(tx).send().getApplicationLog().getFirstExecution()
                .getFirstNotification();
        assertThat(notification.getContract(), is(messageBridge.getScriptHash()));
        assertThat(notification.getEventName(), is("MaxMessageSizeChange"));
        assertThat(notification.getState().getList(), hasSize(1));
        assertThat(notification.getState().getList().get(0).getInteger(), is(newMaxBytes));

        // revert the state for further tests
        messageBridge.setMaxMessageSize(maxBytesBefore);
    }

    @Test
    @Order(0)
    public void test_setMaxMessageSize_maxValue() throws Throwable {
        BigInteger previousMaxMsgSize = messageBridge.maxMessageSize();

        messageBridge.setMaxMessageSize(UPPER_LIMIT_MAX_MESSAGE_SIZE);
        assertThat(messageBridge.maxMessageSize(), is(UPPER_LIMIT_MAX_MESSAGE_SIZE));

        messageBridge.setMaxMessageSize(previousMaxMsgSize);
    }

    @Test
    @Order(0)
    public void test_setMaxMessageSize_valueTooLarge() {
        BigInteger invalidMaxMsgSize = UPPER_LIMIT_MAX_MESSAGE_SIZE.add(BigInteger.ONE);
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.setMaxMessageSize(invalidMaxMsgSize));
        assertThat(thrown.getMessage(), containsString("Max message size too large"));
    }

    @Test
    @Order(0)
    public void test_setMaxMessageSize_invalidValue() {
        BigInteger invalidMaxMsgSize = BigInteger.ZERO;
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.setMaxMessageSize(invalidMaxMsgSize));
        assertThat(thrown.getMessage(), containsString("Max message size must be positive"));
    }

    @Test
    @Order(0)
    public void test_setMaxMessageSize_notGovernor() {
        BigInteger newMaxBytes = new BigInteger("200");
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.setMaxMessageSize(alice, newMaxBytes));
        assertThat(thrown.getMessage(), containsString("No authorization - only governor"));
    }

    @Test
    @Order(0)
    public void test_setMaxNrMessages() throws Throwable {
        BigInteger maxNrMessagesBefore = messageBridge.maxNrMessages();
        BigInteger newMaxNrMessages = new BigInteger("20");
        assertThat(newMaxNrMessages, is(not(maxNrMessagesBefore)));

        Hash256 tx = messageBridge.setMaxNrMessages(newMaxNrMessages);
        assertThat(messageBridge.maxNrMessages(), is(newMaxNrMessages));

        Notification notification = neow3j.getApplicationLog(tx).send().getApplicationLog().getFirstExecution()
                .getFirstNotification();
        assertThat(notification.getContract(), is(messageBridge.getScriptHash()));
        assertThat(notification.getEventName(), is("MaxNrMessagesChange"));
        assertThat(notification.getState().getList(), hasSize(1));
        assertThat(notification.getState().getList().get(0).getInteger(), is(newMaxNrMessages));

        // revert the state for further tests
        messageBridge.setMaxNrMessages(maxNrMessagesBefore);
    }

    @Test
    @Order(0)
    public void test_setMaxNrMessages_maxValue() throws Throwable {
        BigInteger previousMaxNrMessages = messageBridge.maxNrMessages();

        messageBridge.setMaxNrMessages(UPPER_LIMIT_MAX_NR_MESSAGES);
        assertThat(messageBridge.maxNrMessages(), is(UPPER_LIMIT_MAX_NR_MESSAGES));

        messageBridge.setMaxNrMessages(previousMaxNrMessages);
    }

    @Test
    @Order(0)
    public void test_setMaxNrMessages_valueTooLarge() {
        BigInteger invalidMaxNrMessages = UPPER_LIMIT_MAX_NR_MESSAGES.add(BigInteger.ONE);
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.setMaxNrMessages(invalidMaxNrMessages));
        assertThat(thrown.getMessage(), containsString("Max number of messages too large"));
    }

    @Test
    @Order(0)
    public void test_setMaxNrMessages_invalidValue() {
        BigInteger invalidMaxNrMessages = BigInteger.ZERO;
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.setMaxNrMessages(invalidMaxNrMessages));
        assertThat(thrown.getMessage(), containsString("Max number of messages must be positive"));
    }

    @Test
    @Order(0)
    public void test_setMaxNrMessages_notGovernor() {
        BigInteger newMaxNrMessages = new BigInteger("20");
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.setMaxNrMessages(alice, newMaxNrMessages));
        assertThat(thrown.getMessage(), containsString("No authorization - only governor"));
    }

    @Test
    @Order(0)
    public void test_setExecutionWindowMilliseconds() throws Throwable {
        BigInteger execWindowMillisBefore = messageBridge.executionWindowMilliseconds();
        BigInteger newExecWindowMillis = new BigInteger("3600").multiply(BigInteger.valueOf(1000)); // 1 hour in ms
        assertThat(newExecWindowMillis, is(not(execWindowMillisBefore)));

        Hash256 tx = messageBridge.setExecutionWindowMilliseconds(newExecWindowMillis);
        assertThat(messageBridge.executionWindowMilliseconds(), is(newExecWindowMillis));

        Notification notification = neow3j.getApplicationLog(tx).send().getApplicationLog().getFirstExecution()
                .getFirstNotification();
        assertThat(notification.getContract(), is(messageBridge.getScriptHash()));
        assertThat(notification.getEventName(), is("ExecutionWindowChange"));
        assertThat(notification.getState().getList(), hasSize(1));
        assertThat(notification.getState().getList().get(0).getInteger(), is(newExecWindowMillis));

        // revert the state for further tests
        messageBridge.setExecutionWindowMilliseconds(execWindowMillisBefore);
    }

    @Test
    @Order(0)
    public void test_setExecutionWindowMilliseconds_maxValue() throws Throwable {
        BigInteger previousExecWindowMillis = messageBridge.executionWindowMilliseconds();

        messageBridge.setExecutionWindowMilliseconds(UPPER_LIMIT_MAX_EXECUTION_WINDOW_MILLIS);
        assertThat(messageBridge.executionWindowMilliseconds(), is(UPPER_LIMIT_MAX_EXECUTION_WINDOW_MILLIS));

        messageBridge.setExecutionWindowMilliseconds(previousExecWindowMillis);
    }

    @Test
    @Order(0)
    public void test_setExecutionWindowMilliseconds_valueTooLarge() {
        BigInteger invalidExecWindowSeconds = UPPER_LIMIT_MAX_EXECUTION_WINDOW_MILLIS.add(BigInteger.ONE); // 1ms too large
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.setExecutionWindowMilliseconds(invalidExecWindowSeconds));
        assertThat(thrown.getMessage(), containsString("Execution window too large"));
    }

    @Test
    @Order(0)
    public void test_setExecutionWindowMilliseconds_invalidValue() {
        BigInteger invalidExecWindowSeconds = BigInteger.ZERO;
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.setExecutionWindowMilliseconds(invalidExecWindowSeconds));
        assertThat(thrown.getMessage(), containsString("Execution window must be positive"));
    }

    @Test
    @Order(0)
    public void test_setExecutionWindowMilliseconds_notGovernor() {
        BigInteger newExecWindowSeconds = new BigInteger("3600");
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.setExecutionWindowMilliseconds(alice, newExecWindowSeconds));
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

        // revert the state for further tests
        messageBridge.setExecutionManager(executionManagerBefore);
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
    public void test_setExecutionManager_null() {
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.invokeFunction("setExecutionManager", any(null))
                        .signers(calledByEntry(governor)).sign());
        assertThat(thrown.getMessage(), containsString("Invalid execution manager"));
    }

    @Test
    @Order(0)
    public void test_setExecutionManager_invalid() {
        byte[] newExecutionManager = hexStringToByteArray("0x1253");
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.invokeFunction("setExecutionManager", byteArray(newExecutionManager))
                        .signers(calledByEntry(governor)).sign());
        assertThat(thrown.getMessage(), containsString("Invalid execution manager"));
    }

    @Test
    @Order(0)
    public void test_setExecutionManager_zero() {
        Hash160 newExecutionManager = Hash160.ZERO;
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> messageBridge.setExecutionManager(newExecutionManager));
        assertThat(thrown.getMessage(), containsString("Invalid execution manager"));
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
