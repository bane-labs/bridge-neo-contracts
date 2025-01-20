package network.bane.bridge;

import io.neow3j.contract.ContractManagement;
import io.neow3j.contract.FungibleToken;
import io.neow3j.test.ContractTest;
import io.neow3j.test.ContractTestExtension;
import io.neow3j.test.DeployConfig;
import io.neow3j.test.DeployConfiguration;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.transaction.witnessrule.CalledByContractCondition;
import io.neow3j.transaction.witnessrule.WitnessAction;
import io.neow3j.transaction.witnessrule.WitnessRule;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.types.NeoVMStateType;
import io.neow3j.wallet.Account;
import network.bane.management.BridgeManagementContract;
import network.bane.testhelper.TestContract;
import network.bane.util.structs.NativeBridge;
import network.bane.util.structs.State;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.integer;
import static java.util.Arrays.asList;
import static network.bane.util.TestHelper.concatAndKeccak256;
import static network.bane.util.TestHelper.createDepositHash;
import static network.bane.util.TestHelper.owner;
import static network.bane.util.TestHelper.recipient0;
import static network.bane.util.TestHelper.signMsg;
import static network.bane.util.TestHelper.validator1;
import static network.bane.util.TestHelper.validator2;
import static network.bane.util.TestHelper.validator3;
import static network.bane.util.TestHelper.validator4;
import static network.bane.util.TestHelper.validator5;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_LINKED_CHAIN_ID;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_DEPOSIT_FEE;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_MAX_WITHDRAWALS;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_MIN_DEPOSIT;
import static network.bane.util.helper.DefaultTestValues.MANAGEMENT_CONTRACT_HASH;
import static network.bane.util.helper.TestHelper.alice;
import static network.bane.util.helper.TestHelper.bridge;
import static network.bane.util.helper.TestHelper.createBridgeManagementDeployConfig;
import static network.bane.util.helper.TestHelper.gasToken;
import static network.bane.util.helper.TestHelper.neow3j;
import static network.bane.util.helper.TestHelper.prepareBridgeDeployParameter;
import static network.bane.util.helper.TestHelper.setup;
import static network.bane.util.helper.TestHelper.setupBridge;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ContractTest(
        blockTime = 1,
        contracts = {BridgeManagementContract.class, BridgeContract.class, TestContract.class},
        batchFile = "setup.batch"
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NativeTokenBridgeTotalDepositedTest {

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
        DeployConfiguration config = new DeployConfiguration();
        config.setDeployParam(
                prepareBridgeDeployParameter(
                        DEFAULT_LINKED_CHAIN_ID,
                        MANAGEMENT_CONTRACT_HASH,
                        DEFAULT_DEPOSIT_FEE,
                        DEFAULT_MIN_DEPOSIT,
                        FungibleToken.toFractions(new BigDecimal("500"), 8),
                        DEFAULT_MAX_WITHDRAWALS,
                        FungibleToken.toFractions(new BigDecimal("1000"), 8)
                )
        );
        AccountSigner deploySigner = AccountSigner.none(owner);
        WitnessRule deployWitnessRule = new WitnessRule(WitnessAction.ALLOW,
                new CalledByContractCondition(ContractManagement.SCRIPT_HASH));
        deploySigner.setRules(deployWitnessRule);
        config.setSigner(deploySigner);
        return config;
    }

    /**
     * Tests the calculation of the total deposited native tokens. Deposits increase the value while withdrawals reduce
     * it again.
     */
    @Order(0)
    @Test
    public void testTotalDepositedNative() throws Throwable {
        NativeBridge initialNativeBridgeState = bridge.getNativeBridge();
        BigInteger fee = initialNativeBridgeState.config.fee;
        assertThat(initialNativeBridgeState.totalDeposited, is(BigInteger.ZERO));

        // Deposit 100 Gas, then 200. Total should be 300 after both deposits.
        bridge.depositNative(alice, recipient0, gasToken.toFractions(new BigDecimal("100")));
        assertThat(bridge.getNativeBridge().totalDeposited, is(gasToken.toFractions(new BigDecimal("100"))));
        bridge.depositNative(alice, recipient0, gasToken.toFractions(new BigDecimal("200")));
        assertThat(bridge.getNativeBridge().totalDeposited, is(gasToken.toFractions(new BigDecimal("300"))));

        // Withdraw 50 gas.
        State currentWithdrawalState = bridge.getNativeBridge().withdrawalState;
        BigInteger nextNonce = currentWithdrawalState.nonce.add(BigInteger.ONE);

        Hash160 to = recipient0;
        BigInteger amount = gasToken.toFractions(new BigDecimal("50"));
        String currentWithdrawalRoot = currentWithdrawalState.root.toString();
        String newWithdrawalRoot = concatAndKeccak256(
                currentWithdrawalRoot,
                createDepositHash(nextNonce, to, amount)
        );
        List<Account> validators = asList(validator1, validator2, validator3, validator4, validator5);

        bridge.withdrawNative(
                newWithdrawalRoot,
                signMsg(validators, newWithdrawalRoot),
                array(
                        array(integer(nextNonce), to, integer(amount))
                )
        );
        assertThat(bridge.getNativeBridge().totalDeposited, is(gasToken.toFractions(new BigDecimal("250"))));

        // Deposit 120 gas.
        bridge.depositNative(alice, recipient0, gasToken.toFractions(new BigDecimal("120")));
        assertThat(bridge.getNativeBridge().totalDeposited, is(gasToken.toFractions(new BigDecimal("370"))));
    }

    /**
     * Tests that no further deposits are accepted if the maximum total deposit amount is reached.
     */
    @Order(1)
    @Test
    public void testTotalDepositedNativeToken_MaxReached() throws Throwable {
        BigInteger newMaxTotalDepositedNative = gasToken.toFractions(new BigDecimal("5000"));
        NativeBridge initialNativeBridgeState = bridge.getNativeBridge();
        assertThat(initialNativeBridgeState.totalDeposited, is(gasToken.toFractions(new BigDecimal("370"))));

        bridge.setMaxTotalDepositedNative(newMaxTotalDepositedNative);
        // Update max gas deposit as well to allow greater amounts in single deposits.
        bridge.setMaxNativeDeposit(gasToken.toFractions(new BigDecimal("4900")));
        assertThat(bridge.getNativeBridge().config.maxTotalDeposit, is(newMaxTotalDepositedNative));

        // The max amount that can still be deposited is 5000 - 370 = 4630.
        // Depositing > 4630 should fail.
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class, () ->
                bridge.depositNative(alice, recipient0, gasToken.toFractions(new BigDecimal("4631"))));
        assertThat(thrown.getMessage(),
                containsString("Max total deposited native tokens exceeded. Wait for governor to increase."));

        // Depositing 4630 should work.
        bridge.depositNative(alice, recipient0, gasToken.toFractions(new BigDecimal("4630")));
        assertThat(bridge.getNativeBridge().totalDeposited, is(newMaxTotalDepositedNative));

        // Depositing 1 should fail.
        thrown = assertThrows(TransactionConfigurationException.class, () ->
                bridge.depositNative(alice, recipient0, gasToken.toFractions(new BigDecimal("1"))));
        assertThat(thrown.getMessage(),
                containsString("Max total deposited native tokens exceeded. Wait for governor to increase."));

        // Increasing the max total deposited gas should enable a gas deposit of 1 to work again.
        bridge.setMaxTotalDepositedNative(gasToken.toFractions(new BigDecimal("5001")));
        assertThat(bridge.getNativeBridge().config.maxTotalDeposit, is(gasToken.toFractions(new BigDecimal("5001"))));
        assertThat(bridge.getNativeBridge().totalDeposited, is(gasToken.toFractions(new BigDecimal("5000"))));

        // Depositing 1 should not fail.
        Hash256 txHash = bridge.depositNative(alice, recipient0, gasToken.toFractions(new BigDecimal("1")));
        assertThat(neow3j.getApplicationLog(txHash).send().getApplicationLog().getFirstExecution().getState(),
                is(NeoVMStateType.HALT));
    }

    /**
     * The maximal total deposited gas value cannot be set lower than the current total deposited gas value.
     */
    @Order(2)
    @Test
    public void testTotalDepositedNativeToken_LessThanAlreadyDeposited() throws Throwable {
        assertThat(bridge.getNativeBridge().totalDeposited, is(gasToken.toFractions(new BigDecimal("5001"))));
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.setMaxTotalDepositedNative(gasToken.toFractions(new BigDecimal("4000"))));
        assertThat(thrown.getMessage(),
                containsString(("New value must be greater or equal to the total amount deposited.")));
    }

}
