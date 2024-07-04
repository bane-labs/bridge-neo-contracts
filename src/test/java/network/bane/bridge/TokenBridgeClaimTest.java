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

@ContractTest(
        blockTime = 1,
        contracts = {BridgeManagementContract.class, BridgeContract.class, TestContract.class},
        batchFile = "setup.batch"
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TokenBridgeClaimTest {

    @RegisterExtension
    public static final ContractTestExtension ext = new ContractTestExtension();

    @BeforeAll
    public static void setUp() throws Throwable {
        setup(ext);
        // TODO: Deploy simple NEP-17
        // TODO: Register simple NEP-17 in token bridge
        // TODO: Allocate funds of the simple NEP-17 to the bridge contract
        // TODO: Use withdrawToken() with to as a contract
        // (TODO: Use withdrawToken() with to as an EOA (not a contract) and the bridge contract having insufficient
        //  balance for the provided amount leading to a failed transfer)
    }

    @DeployConfig(BridgeManagementContract.class)
    public static DeployConfiguration deployConfigManagement() {
        return createBridgeManagementDeployConfig();
    }

    @DeployConfig(BridgeContract.class)
    public static DeployConfiguration deployConfigBridge() {
        return createBridgeDeployConfig();
    }

    // endregion
    // region successful cases

    // Suggestion: Give the unsuccessful test cases a lower order than the successful cases in this test file, so that
    // the existing claimable can be tested and does not be "created" again.
    @Order(0)
    @Test
    public void testTokenClaimToContract() {
        // TODO: Use claimToken for (1) and check event TokenClaim
    }

    // TODO: Use claimToken for (2) and check event TokenClaim (in order to do this, make the NEP-17 allocate more
    //  funds to the bridge to make a claim transfer successful)

    // endregion
    // region test invalid token bridge registrations

    // TODO: Fail to claim if bridge is paused
    // TODO: Fail to claim if token bridge is paused
    // TODO: Fail to claim if token bridge is not registered
    // TODO: Fail to claim if there's nothing to claim for the provided nonce
    // TODO: Fail to claim if NEP-17 transfer returns false

    // endregion

}
