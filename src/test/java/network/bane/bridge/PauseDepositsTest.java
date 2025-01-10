package network.bane.bridge;

import io.neow3j.test.ContractTest;
import io.neow3j.test.ContractTestExtension;
import io.neow3j.test.DeployConfig;
import io.neow3j.test.DeployConfiguration;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;
import network.bane.management.BridgeManagementContract;
import network.bane.testhelper.TestContract;
import network.bane.util.helper.DepositHelper;
import network.bane.util.structs.State;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static network.bane.util.TestHelper.computeNewTokenRootNoPrefix;
import static network.bane.util.TestHelper.governor;
import static network.bane.util.TestHelper.securityGuard;
import static network.bane.util.TestHelper.concatAndKeccak256;
import static network.bane.util.TestHelper.createDepositHash;
import static network.bane.util.TestHelper.recipient0;
import static network.bane.util.TestHelper.relayer;
import static network.bane.util.TestHelper.signMsg;
import static network.bane.util.TestHelper.validator1;
import static network.bane.util.TestHelper.validator2;
import static network.bane.util.TestHelper.validator3;
import static network.bane.util.TestHelper.validator4;
import static network.bane.util.TestHelper.validator5;
import static network.bane.util.helper.TestHelper.alice;
import static network.bane.util.helper.TestHelper.bridge;
import static network.bane.util.helper.TestHelper.createBridgeDeployConfig;
import static network.bane.util.helper.TestHelper.createBridgeManagementDeployConfig;
import static network.bane.util.helper.TestHelper.neoN3NeoTokenHash;
import static network.bane.util.helper.TestHelper.neoXNeoTokenHash;
import static network.bane.util.helper.TestHelper.registerNeoTokenBridge;
import static network.bane.util.helper.TestHelper.setup;
import static network.bane.util.helper.TestHelper.setupBridge;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ContractTest(
        blockTime = 1,
        contracts = {BridgeManagementContract.class, BridgeContract.class, TestContract.class},
        batchFile = "setup.batch"
)
public class PauseDepositsTest {

    @RegisterExtension
    public static final ContractTestExtension ext = new ContractTestExtension();

    @BeforeAll
    public static void setUp() throws Throwable {
        setup(ext);
        setupBridge(ext);
        registerNeoTokenBridge();
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
    // region deposit pausing

    @Test
    public void testPauseDeposits_onlyGovernor() {
        TransactionConfigurationException thrown =
                assertThrows(TransactionConfigurationException.class, () -> bridge.pauseDeposits(relayer));
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Only the governor can call this method."));
    }

    @Test
    public void testUnpauseDeposits_onlyGovernor() throws Throwable {
        assertFalse(bridge.depositsArePaused());
        bridge.pauseDeposits();
        assertTrue(bridge.depositsArePaused());
        TransactionConfigurationException thrown =
                assertThrows(TransactionConfigurationException.class, () -> bridge.unpauseDeposits(securityGuard));
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Only the governor can call this method."));
        bridge.unpauseDeposits();
    }

    @Test
    public void testPauseAndUnpauseDeposits_governorAllowed() throws Throwable {
        assertFalse(bridge.depositsArePaused());
        bridge.pauseDeposits(governor);
        assertTrue(bridge.depositsArePaused());
        bridge.unpauseDeposits(governor);
        assertFalse(bridge.depositsArePaused());
    }

    @Test
    public void testUnpauseDeposits_alreadyUnpaused() throws Throwable {
        assertFalse(bridge.depositsArePaused());
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.unpauseDeposits());
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Deposits are not paused."));
    }

    // endregion
    // region deposits when deposits are paused

    @Test
    public void testPausedDeposit_rejectDepositGas() throws Throwable {
        assertFalse(bridge.depositsArePaused());
        bridge.pauseDeposits();

        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> DepositHelper.depositNative(alice, recipient0, BigInteger.ONE));
        assertThat(thrown.getMessage(), containsString("Deposits are paused."));

        bridge.unpauseDeposits();
    }

    // endregion
    // region withdrawal when deposits are paused

    @Test
    public void testPauseDeposits_withdrawGas() throws Throwable {
        assertFalse(bridge.depositsArePaused());
        bridge.pauseDeposits();
        assertThat(bridge.getNativeBridge().withdrawalState.nonce, is(BigInteger.ZERO));

        BigInteger nonce = BigInteger.ONE;
        Hash160 to = new Hash160("0x6472bf811b33b87f7872e31439cdbd16871d8cad");
        BigInteger amount = new BigInteger("200000000");

        String d1 = createDepositHash(nonce, to, amount);
        String root = concatAndKeccak256(Hash256.ZERO.toString(), d1);
        List<Account> validators = Arrays.asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter withdrawal = array(array(integer(nonce), hash160(to), integer(amount)));
        bridge.withdrawNative(root, signMsg(validators, root), withdrawal);

        assertThat(bridge.getNativeBridge().withdrawalState.nonce, is(BigInteger.ONE));
        bridge.unpauseDeposits();
    }

    @Test
    public void testPauseDeposits_withdrawToken() throws Throwable {
        assertFalse(bridge.depositsArePaused());
        bridge.pauseDeposits();
        assertThat(bridge.getTokenBridge(neoN3NeoTokenHash).withdrawalState.nonce, is(BigInteger.ZERO));

        BigInteger nonce = BigInteger.ONE;
        Hash160 to = new Hash160("0x6472bf811b33b87f7872e31439cdbd16871d8cad");
        BigInteger amount = new BigInteger("200000000");

        State withdrawalState_before = bridge.getTokenBridge(neoN3NeoTokenHash).withdrawalState;
        String root = computeNewTokenRootNoPrefix(withdrawalState_before.root.toString(), neoN3NeoTokenHash,
                        neoXNeoTokenHash, BigInteger.ONE, to, amount);

        List<Account> validators = Arrays.asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter withdrawal = array(array(integer(nonce), hash160(to), integer(amount)));
        bridge.withdrawToken(neoN3NeoTokenHash, root, signMsg(validators, root), withdrawal);

        assertThat(bridge.getTokenBridge(neoN3NeoTokenHash).withdrawalState.nonce, is(nonce));
        bridge.unpauseDeposits();
    }

    // endregion

}
