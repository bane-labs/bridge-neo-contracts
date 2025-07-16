package network.bane.bridge;

import io.neow3j.protocol.core.response.Notification;
import io.neow3j.test.ContractTest;
import io.neow3j.test.ContractTestExtension;
import io.neow3j.test.DeployConfig;
import io.neow3j.test.DeployConfiguration;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import network.bane.management.BridgeManagementContract;
import network.bane.testhelper.TestContract;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.math.BigInteger;

import static network.bane.util.TestHelper.governor;
import static network.bane.util.TestHelper.securityGuard;
import static network.bane.util.helper.TestHelper.alice;
import static network.bane.util.helper.TestHelper.bridge;
import static network.bane.util.helper.TestHelper.createBridgeDeployConfig;
import static network.bane.util.helper.TestHelper.createBridgeManagementDeployConfig;
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
    public void test_setSendingFee_notGovernor(){
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
    public void test_setMaxBytesForSending_notGovernor(){
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
    public void test_setMaxNrMessagesForStoring_notGovernor(){
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
    public void test_setExecutionWindowSeconds_notGovernor(){
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
    public void test_setExecutionManager_notGovernor(){
        Hash160 newExecutionManager = testContract;
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.setMessageExecutionManager(alice, newExecutionManager));
        assertThat(thrown.getMessage(), containsString("No authorization - only governor"));
    }

    // endregion

}
