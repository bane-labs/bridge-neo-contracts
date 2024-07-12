package network.bane.bridge;

import io.neow3j.test.ContractTest;
import io.neow3j.test.ContractTestExtension;
import io.neow3j.test.DeployConfig;
import io.neow3j.test.DeployConfiguration;
import network.bane.management.BridgeManagementContract;
import network.bane.testhelper.Nep17TokenContract;
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
        contracts = {BridgeManagementContract.class, BridgeContract.class, Nep17TokenContract.class},
        batchFile = "setup.batch"
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TokenBridgeDepositTest {

    @RegisterExtension
    public static final ContractTestExtension ext = new ContractTestExtension();

    @BeforeAll
    public static void setUp() throws Throwable {
        setup(ext);
        setupBridge(ext);
        // TODO: Deploy simple NEP-17
        // TODO: Register simple NEP-17 in token bridge
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
    public void testTokenDeposit() {
        // TODO: Use depositToken() with the deployed NEP-17 token
    }

    // TODO: Check emitted event TokenDeposit and its parameters
    // TODO: Check updated deposit nonce and root

    // endregion
    // region invalid test cases

    // TODO: Fail to deposit if to is null/invalid/zero
    // TODO: Fail to deposit if from is null/invalid/zero
    // TODO: Fail if from is bridge contract's script hash
    // TODO: Fail if amount is lower than minimum in config
    // TODO: Fail if amount is greater than maximum in config
    // TODO: Fail if maxFee is lower than fee in config
    // TODO: Fail if from has insufficient gas to pay bridge fee
    // TODO: Fail if NEP-17 transfer returns false

    // endregion

}
