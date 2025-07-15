package network.bane.bridge;

import io.neow3j.test.ContractTest;
import io.neow3j.test.ContractTestExtension;
import io.neow3j.test.DeployConfig;
import io.neow3j.test.DeployConfiguration;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import network.bane.management.BridgeManagementContract;
import network.bane.testhelper.TestContract;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;

import static network.bane.util.TestHelper.governor;
import static network.bane.util.TestHelper.securityGuard;
import static network.bane.util.helper.TestHelper.alice;
import static network.bane.util.helper.TestHelper.bridge;
import static network.bane.util.helper.TestHelper.createBridgeDeployConfig;
import static network.bane.util.helper.TestHelper.createBridgeManagementDeployConfig;
import static network.bane.util.helper.TestHelper.setup;
import static network.bane.util.helper.TestHelper.setupBridge;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
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

}
