package network.bane.bridge;

import io.neow3j.test.ContractTest;
import io.neow3j.test.ContractTestExtension;
import io.neow3j.test.DeployConfig;
import io.neow3j.test.DeployConfiguration;
import network.bane.management.BridgeManagementContract;
import network.bane.testhelper.TestContract;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import static network.bane.util.helper.TestHelper.createBridgeDeployConfig;
import static network.bane.util.helper.TestHelper.createBridgeManagementDeployConfig;
import static network.bane.util.helper.TestHelper.setup;
import static network.bane.util.helper.TestHelper.setupBridge;

@ContractTest(
        blockTime = 1,
        contracts = {BridgeManagementContract.class, BridgeContract.class, TestContract.class},
        batchFile = "setup.batch"
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TokenBridgePauseTest {

    @RegisterExtension
    public static final ContractTestExtension ext = new ContractTestExtension();

    @BeforeAll
    public static void setUp() throws Throwable {
        setup(ext);
        setupBridge(ext);
    }

    @DeployConfig(BridgeManagementContract.class)
    public static DeployConfiguration deployConfigManagement() {
        return createBridgeManagementDeployConfig();
    }

    @DeployConfig(BridgeContract.class)
    public static DeployConfiguration deployConfigBridge() {
        return createBridgeDeployConfig();
    }

    // region successful test cases

    @Order(0)
    @Test
    public void testTokenBridgePause() {
        // TODO: Pause a token bridge and check its event TokenBridgePause and paused entry in getTokenBridge(Hash160)
    }

    // TODO: Once paused, check pause state of token bridge
    // TODO: Unpause a paused token bridge and check its event TokenBridgeUnpause and paused entry in getTokenBridge(Hash160)

    // endregion
    // region invalid test cases

    // TODO: Fail to use pauseTokenBridge() if not security guard
    // TODO: Fail to use unpauseTokenBridge() if not governor
    // TODO: Fail to use pauseTokenBridge() if the token bridge is already paused
    // TODO: Fail to use unpauseTokenBridge() if the token bridge is already unpaused
    // TODO: Fail to use depositToken() if token bridge is paused - expect abort
    // TODO: Fail to use withdrawToken() if token bridge is paused - expect abort
    // TODO: Fail to use claimToken() if token bridge is paused - expect abort

    // endregion

}
