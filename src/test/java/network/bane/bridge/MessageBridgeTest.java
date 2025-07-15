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
        assertThat(thrown.getMessage(), containsString("ExecutionManager must be a contract"));
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
