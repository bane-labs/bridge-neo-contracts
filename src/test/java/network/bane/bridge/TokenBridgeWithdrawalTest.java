package network.bane.bridge;

import io.neow3j.test.ContractTest;
import io.neow3j.test.ContractTestExtension;
import io.neow3j.test.DeployConfig;
import io.neow3j.test.DeployConfiguration;
import network.bane.management.BridgeManagementContract;
import network.bane.testhelper.TestContract;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import static network.bane.support.TestEnvironment.createBridgeDeployConfig;
import static network.bane.support.TestEnvironment.createBridgeManagementDeployConfig;
import static network.bane.support.TestEnvironment.setup;
import static network.bane.support.TestEnvironment.setupBridge;

@Disabled
@ContractTest(
        blockTime = 1,
        contracts = {BridgeManagementContract.class, BridgeContract.class, TestContract.class},
        batchFile = "setup.batch"
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TokenBridgeWithdrawalTest {

    @RegisterExtension
    public static final ContractTestExtension ext = new ContractTestExtension();

    @BeforeAll
    public static void setUp() throws Throwable {
        setup(ext);
        setupBridge(ext);
        // TODO: Deploy simple NEP-17
        // TODO: Register simple NEP-17 in token bridge
        // TODO: Allocate funds of the simple NEP-17 to the bridge contract
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
    public void testTokenWithdrawalToEOA() {
        // TODO: Use withdrawToken() with the deployed NEP-17 token and to as an EOA (not a contract) -> check event
        //  TokenWithdrawal
    }

    // TODO: Use withdrawToken() with the deployed NEP-17 token and to a contract -> check event TokenClaimable
    // TODO: Use withdrawToken() with withdrawals that exceed the bridge's balance -> check event TokenClaimable
    // TODO: Check event NativeWithdrawalRootUpdate for a successful withdrawal
    // TODO: Check updated root and nonce after a successful withdrawal

    // endregion
    // region invalid test cases

    // TODO: Fail to withdraw if token bridge is not registered
    // TODO: Fail to withdraw if withdrawals list parameter has size 0
    // TODO: Fail if first withdrawal in withdrawals list parameter has not the next nonce
    // TODO: Fail if withdrawals in withdrawals list parameter have no subsequent nonces
    // TODO: Fail if provided withdrawalRoot is not equal the computed new root
    // TODO: Fail if verifyValidatorSignatures() returns false (e.g., not enough signatures)

    // endregion

}
