package network.bane.bridge;

import io.neow3j.test.ContractTest;
import io.neow3j.test.ContractTestExtension;
import io.neow3j.test.DeployConfig;
import io.neow3j.test.DeployConfiguration;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.types.Hash160;
import io.neow3j.wallet.Account;
import network.bane.management.BridgeManagementContract;
import network.bane.testhelper.TestContract;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;

import static network.bane.util.helper.TestHelper.bob;
import static network.bane.util.helper.TestHelper.bridge;
import static network.bane.util.helper.TestHelper.createBridgeDeployConfig;
import static network.bane.util.helper.TestHelper.createBridgeManagementDeployConfig;
import static network.bane.util.helper.TestHelper.gasToken;
import static network.bane.util.helper.TestHelper.neoN3NeoTokenHash;
import static network.bane.util.helper.TestHelper.registerNeoTokenBridge;
import static network.bane.util.helper.TestHelper.setup;
import static network.bane.util.helper.TestHelper.setupBridge;
import static network.bane.util.helper.TokenDeployment.deployTestToken;
import static network.bane.util.helper.TokenHelper.dummyTokenConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ContractTest(blockTime = 1, contracts = {BridgeManagementContract.class, BridgeContract.class, TestContract.class},
              batchFile = "setup.batch")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TokenBridgeConfigTest {

    @RegisterExtension
    public static final ContractTestExtension ext = new ContractTestExtension();

    private static Hash160 testTokenHash;
    private static Hash160 unregisteredTokenHash = Account.create().getScriptHash();

    @BeforeAll
    public static void setUp() throws Throwable {
        setup(ext);
        setupBridge(ext);

        registerNeoTokenBridge();

        testTokenHash = deployTestToken(ext).getScriptHash();
        bridge.registerToken(testTokenHash, dummyTokenConfig());
    }

    @DeployConfig(BridgeManagementContract.class)
    public static DeployConfiguration deployConfigManagement() {
        return createBridgeManagementDeployConfig();
    }

    @DeployConfig(BridgeContract.class)
    public static DeployConfiguration deployConfigBridge() {
        return createBridgeDeployConfig();
    }

    // region deposit fee

    // Set one deposit fee and make sure the deposit fees of other tokens remain the same.
    @Order(0)
    @Test
    public void testDepositFee() throws Throwable {
        BigInteger initValueNeo = bridge.tokenDepositFee(neoN3NeoTokenHash);
        BigInteger initValueTestToken = bridge.tokenDepositFee(testTokenHash);

        BigInteger newValueNeo = initValueNeo.add(gasToken.toFractions(new BigDecimal("0.1")));

        HashMap<Hash160, BigInteger> newValues = new HashMap<>();
        newValues.put(neoN3NeoTokenHash, newValueNeo);

        bridge.setTokenDepositFee(newValues);

        assertThat(bridge.tokenDepositFee(neoN3NeoTokenHash), is(newValueNeo));
        assertThat(bridge.tokenDepositFee(testTokenHash), is(initValueTestToken));
    }

    // Set multiple deposit fees.
    @Order(0)
    @Test
    public void testDepositFee_setMultiple() throws Throwable {
        BigInteger initValueNeo = bridge.tokenDepositFee(neoN3NeoTokenHash);
        BigInteger initValueTestToken = bridge.tokenDepositFee(testTokenHash);

        BigInteger newValueNeo = initValueNeo.add(gasToken.toFractions(new BigDecimal("0.005")));
        BigInteger newValueTestToken = initValueTestToken.subtract(gasToken.toFractions(new BigDecimal("0.009")));

        HashMap<Hash160, BigInteger> newValues = new HashMap<>();
        newValues.put(neoN3NeoTokenHash, newValueNeo);
        newValues.put(testTokenHash, newValueTestToken);

        bridge.setTokenDepositFee(newValues);

        assertThat(bridge.tokenDepositFee(neoN3NeoTokenHash), is(newValueNeo));
        assertThat(bridge.tokenDepositFee(testTokenHash), is(newValueTestToken));
    }

    // Abort when a non-governor tries to set the deposit fee.
    @Order(0)
    @Test
    public void testDepositFee_unauthorized() throws IOException {
        BigInteger initValueNeo = bridge.tokenDepositFee(neoN3NeoTokenHash);
        BigInteger newValue = initValueNeo.add(BigInteger.ONE);

        HashMap<Hash160, BigInteger> newValues = new HashMap<>();
        newValues.put(neoN3NeoTokenHash, newValue);

        TransactionConfigurationException thrown = assertThrows(
                TransactionConfigurationException.class, () -> bridge.setTokenDepositFee(bob, newValues));
        assertThat(thrown.getMessage(), containsString("No authorization - only governor"));
    }

    // Abort when trying to set a deposit fee value for a token that is not registered.
    @Order(0)
    @Test
    public void testDepositFee_unregistered() {
        HashMap<Hash160, BigInteger> newValues = new HashMap<>();
        newValues.put(unregisteredTokenHash, BigInteger.ONE);

        TransactionConfigurationException thrown = assertThrows(
                TransactionConfigurationException.class, () -> bridge.setTokenDepositFee(newValues));

        assertThat(thrown.getMessage(), containsString("Token not registered"));
    }

    // endregion
    // region min deposit

    // Set one min deposit value and make sure the min deposit values of other tokens remain the same.
    @Order(0)
    @Test
    public void testMinDeposit() throws Throwable {
        BigInteger initValueNeo = bridge.minTokenDeposit(neoN3NeoTokenHash);
        BigInteger initValueTestToken = bridge.minTokenDeposit(testTokenHash);

        BigInteger newValueNeo = initValueNeo.subtract(gasToken.toFractions(new BigDecimal("0.1")));

        HashMap<Hash160, BigInteger> newValues = new HashMap<>();
        newValues.put(neoN3NeoTokenHash, newValueNeo);

        bridge.setMinTokenDeposit(newValues);

        assertThat(bridge.minTokenDeposit(neoN3NeoTokenHash), is(newValueNeo));
        assertThat(bridge.minTokenDeposit(testTokenHash), is(initValueTestToken));
    }

    // Set multiple min deposit values.
    @Order(0)
    @Test
    public void testMinDepositFee_setMultiple() throws Throwable {
        BigInteger initValueNeo = bridge.minTokenDeposit(neoN3NeoTokenHash);
        BigInteger initValueTestToken = bridge.minTokenDeposit(testTokenHash);

        BigInteger newValueNeo = initValueNeo.subtract(gasToken.toFractions(new BigDecimal("0.001")));
        BigInteger newValueTestToken = initValueTestToken.subtract(gasToken.toFractions(new BigDecimal("0.002")));

        HashMap<Hash160, BigInteger> newValues = new HashMap<>();
        newValues.put(neoN3NeoTokenHash, newValueNeo);
        newValues.put(testTokenHash, newValueTestToken);

        bridge.setMinTokenDeposit(newValues);

        assertThat(bridge.minTokenDeposit(neoN3NeoTokenHash), is(newValueNeo));
        assertThat(bridge.minTokenDeposit(testTokenHash), is(newValueTestToken));
    }

    // Abort when a non-governor tries to set the min deposit value.
    @Order(0)
    @Test
    public void testMinDeposit_unauthorized() throws IOException {
        BigInteger initValueNeo = bridge.minTokenDeposit(neoN3NeoTokenHash);

        BigInteger newValue = initValueNeo.add(BigInteger.ONE);

        HashMap<Hash160, BigInteger> newValues = new HashMap<>();
        newValues.put(neoN3NeoTokenHash, newValue);

        TransactionConfigurationException thrown = assertThrows(
                TransactionConfigurationException.class, () -> bridge.setMinTokenDeposit(bob, newValues));
        assertThat(thrown.getMessage(), containsString("No authorization - only governor"));
    }

    // Abort when trying to set a min deposit value that is greater than the max deposit value.
    @Order(0)
    @Test
    public void testMinDeposit_greaterThanMax() throws IOException {
        BigInteger minValue = bridge.minTokenDeposit(neoN3NeoTokenHash);
        BigInteger maxValue = bridge.maxTokenDeposit(neoN3NeoTokenHash);

        assertThat(minValue, lessThan(maxValue));

        HashMap<Hash160, BigInteger> newValues = new HashMap<>();
        newValues.put(neoN3NeoTokenHash, maxValue.add(BigInteger.ONE));

        TransactionConfigurationException thrown = assertThrows(
                TransactionConfigurationException.class, () -> bridge.setMinTokenDeposit(newValues));

        assertThat(thrown.getMessage(),
                containsString("Minimum deposit must not be greater than the maximum deposit."));
    }

    // Abort when trying to set a min deposit value for a token that is not registered.
    @Order(0)
    @Test
    public void testMinDeposit_unregistered() {
        HashMap<Hash160, BigInteger> newValues = new HashMap<>();
        newValues.put(unregisteredTokenHash, BigInteger.ONE);

        TransactionConfigurationException thrown = assertThrows(
                TransactionConfigurationException.class, () -> bridge.setMinTokenDeposit(newValues));

        assertThat(thrown.getMessage(), containsString("Token not registered"));
    }

    // endregion
    // region max deposit

    // Set one max deposit value and make sure the max deposit values of other tokens remain the same.
    @Order(0)
    @Test
    public void testMaxDeposit() throws Throwable {
        BigInteger initValueNeo = bridge.maxTokenDeposit(neoN3NeoTokenHash);
        BigInteger initValueTestToken = bridge.maxTokenDeposit(testTokenHash);

        BigInteger newValueNeo = initValueNeo.add(gasToken.toFractions(new BigDecimal("0.1")));

        HashMap<Hash160, BigInteger> newValues = new HashMap<>();
        newValues.put(neoN3NeoTokenHash, newValueNeo);

        bridge.setMaxTokenDeposit(newValues);

        assertThat(bridge.maxTokenDeposit(neoN3NeoTokenHash), is(newValueNeo));
        assertThat(bridge.maxTokenDeposit(testTokenHash), is(initValueTestToken));
    }

    // Set multiple max deposit values.
    @Order(0)
    @Test
    public void testMaxDepositFee_setMultiple() throws Throwable {
        BigInteger initValueNeo = bridge.maxTokenDeposit(neoN3NeoTokenHash);
        BigInteger initValueTestToken = bridge.maxTokenDeposit(testTokenHash);

        BigInteger newValueNeo = initValueNeo.add(gasToken.toFractions(new BigDecimal("0.001")));
        BigInteger newValueTestToken = initValueTestToken.subtract(gasToken.toFractions(new BigDecimal("0.002")));

        HashMap<Hash160, BigInteger> newValues = new HashMap<>();
        newValues.put(neoN3NeoTokenHash, newValueNeo);
        newValues.put(testTokenHash, newValueTestToken);

        bridge.setMaxTokenDeposit(newValues);

        assertThat(bridge.maxTokenDeposit(neoN3NeoTokenHash), is(newValueNeo));
        assertThat(bridge.maxTokenDeposit(testTokenHash), is(newValueTestToken));
    }

    // Abort when a non-governor tries to set the max deposit value.
    @Order(0)
    @Test
    public void testMaxDeposit_unauthorized() throws IOException {
        BigInteger initValueNeo = bridge.maxTokenDeposit(neoN3NeoTokenHash);
        BigInteger newValue = initValueNeo.add(BigInteger.ONE);

        HashMap<Hash160, BigInteger> newValues = new HashMap<>();
        newValues.put(neoN3NeoTokenHash, newValue);

        TransactionConfigurationException thrown = assertThrows(
                TransactionConfigurationException.class, () -> bridge.setMaxTokenDeposit(bob, newValues));
        assertThat(thrown.getMessage(), containsString("No authorization - only governor"));
    }

    // Abort when trying to set a max deposit value that is less than the min deposit value.
    @Order(0)
    @Test
    public void testMaxDeposit_lessThanMin() throws IOException {
        BigInteger minValue = bridge.minTokenDeposit(neoN3NeoTokenHash);
        BigInteger maxValue = bridge.maxTokenDeposit(neoN3NeoTokenHash);

        assertThat(maxValue, greaterThan(minValue));

        HashMap<Hash160, BigInteger> newValues = new HashMap<>();
        newValues.put(neoN3NeoTokenHash, minValue.subtract(BigInteger.ONE));

        TransactionConfigurationException thrown = assertThrows(
                TransactionConfigurationException.class, () -> bridge.setMaxTokenDeposit(newValues));

        assertThat(thrown.getMessage(), containsString("Maximum deposit must not be less than the minimum deposit."));
    }

    // Abort when trying to set a max deposit value for a token that is not registered.
    @Order(0)
    @Test
    public void testMaxDeposit_unregistered() {
        HashMap<Hash160, BigInteger> newValues = new HashMap<>();
        newValues.put(unregisteredTokenHash, BigInteger.TEN);

        TransactionConfigurationException thrown = assertThrows(
                TransactionConfigurationException.class, () -> bridge.setMaxTokenDeposit(newValues));

        assertThat(thrown.getMessage(), containsString("Token not registered"));
    }

    // endregion
    // region max withdrawals

    // Set one max withdrawals value and make sure the max withdrawals values of other tokens remain the same.
    @Order(0)
    @Test
    public void testMaxWithdrawals() throws Throwable {
        int initValueNeo = bridge.maxTokenWithdrawals(neoN3NeoTokenHash);
        int initValueTestToken = bridge.maxTokenWithdrawals(testTokenHash);

        int newValueNeo = initValueNeo + 1;

        HashMap<Hash160, Integer> newValues = new HashMap<>();
        newValues.put(neoN3NeoTokenHash, newValueNeo);

        bridge.setMaxTokenWithdrawals(newValues);

        assertThat(bridge.maxTokenWithdrawals(neoN3NeoTokenHash), is(newValueNeo));
        assertThat(bridge.maxTokenWithdrawals(testTokenHash), is(initValueTestToken));
    }

    // Set multiple max withdrawals values.
    @Order(0)
    @Test
    public void testMaxWithdrawals_setMultiple() throws Throwable {
        int initValueNeo = bridge.maxTokenWithdrawals(neoN3NeoTokenHash);
        int initValueTestToken = bridge.maxTokenWithdrawals(testTokenHash);

        int newValueNeo = initValueNeo + 1;
        int newValueTestToken = initValueTestToken - 14;

        HashMap<Hash160, Integer> newValues = new HashMap<>();
        newValues.put(neoN3NeoTokenHash, newValueNeo);
        newValues.put(testTokenHash, newValueTestToken);

        bridge.setMaxTokenWithdrawals(newValues);

        assertThat(bridge.maxTokenWithdrawals(neoN3NeoTokenHash), is(newValueNeo));
        assertThat(bridge.maxTokenWithdrawals(testTokenHash), is(newValueTestToken));
    }

    // Abort when a non-governor tries to set the max withdrawals value.
    @Order(0)
    @Test
    public void testMaxWithdrawals_unauthorized() throws IOException {
        int initValueNeo = bridge.maxTokenWithdrawals(neoN3NeoTokenHash);
        int newValue = initValueNeo + 1;

        HashMap<Hash160, Integer> newValues = new HashMap<>();
        newValues.put(neoN3NeoTokenHash, newValue);

        TransactionConfigurationException thrown = assertThrows(
                TransactionConfigurationException.class, () -> bridge.setMaxTokenWithdrawals(bob, newValues));
        assertThat(thrown.getMessage(), containsString("No authorization - only governor"));
    }

    // Abort when trying to set a max withdrawals value for a token that is not registered.
    @Order(0)
    @Test
    public void testMaxWithdrawals_unregistered() {
        HashMap<Hash160, Integer> newValues = new HashMap<>();
        newValues.put(unregisteredTokenHash, 15);

        TransactionConfigurationException thrown = assertThrows(
                TransactionConfigurationException.class, () -> bridge.setMaxTokenWithdrawals(newValues));

        assertThat(thrown.getMessage(), containsString("Token not registered"));
    }

    // endregion
}
