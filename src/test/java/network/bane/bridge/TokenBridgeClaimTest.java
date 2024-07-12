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
import network.bane.testhelper.TestContract;
import network.bane.management.BridgeManagementContract;
import network.bane.testhelper.Nep17TokenContract;
import network.bane.util.Nep17Token;
import network.bane.util.TestHelper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static io.neow3j.types.ContractParameter.*;
import static io.neow3j.types.ContractParameter.integer;
import static network.bane.util.TestHelper.*;
import static network.bane.util.helper.PrintHelper.printTransactionFee;
import static network.bane.util.helper.TestHelper.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ContractTest(
        blockTime = 1,
        contracts = {BridgeManagementContract.class, BridgeContract.class, Nep17TokenContract.class, TestContract.class},
        batchFile = "setup.batch"
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TokenBridgeClaimTest {

    @RegisterExtension
    public static final ContractTestExtension ext = new ContractTestExtension();

    @BeforeAll
    public static void setUp() throws Throwable {
        setup(ext);
        setupBridge(ext);
        nep17Token = new Nep17Token(ext.getDeployedContract(Nep17TokenContract.class).getScriptHash(), neow3j);
        registerTokenBridge(nep17Token.getScriptHash());
        nep17Token.transfer(owner, bridge.getScriptHash(), new BigInteger("100000000"));
        // TODO: Deploy simple NEP-17
        // TODO: Register simple NEP-17 in token bridge
        // TODO: Allocate funds of the simple NEP-17 to the bridge contract
        // TODO: Use withdrawToken() with to as a contract
        // (TODO: Use withdrawToken() with to as an EOA (not a contract) and the bridge contract having insufficient
        //  balance for the provided amount leading to a failed transfer)
    }

    /***
     * Use withdrawToken with to as a contract
     * Use claimToken for and check event TokenClaim
     * @throws Throwable
     */
    @Test
    @Order(1)
    public static void withdrawalToContract() throws Throwable {
        Hash160 to = testContract;
        BigInteger amount = new BigInteger("10");
        BigInteger nonce = incrementAndGetWithdrawalNonce();

        String withdrawRootBefore = bridge.tokenWithdrawalRoot(nep17Token.getScriptHash());
        String d1 = createTokenOpHash(nep17Token.getScriptHash(), nep17TokenXTokenHash, nonce, to, amount);
        String root = concatAndSha256(withdrawRootBefore, d1);
        List<Account> validators = Arrays.asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter withdrawal = array(array(integer(nonce), hash160(to), integer(amount)));

        Hash256 txHash = bridge.withdrawToken(nep17Token.getScriptHash(), root, signMsg(validators, root), withdrawal);
        printTransactionFee(neow3j, "tx with 1 withdrawals to claim", txHash);

        List<network.bane.util.TestHelper.ClaimableEvent> claimableEvents = getTokenClaimableEvents(txHash, neow3j, bridge.getScriptHash());
        assertThat(claimableEvents, hasSize(1));
        network.bane.util.TestHelper.ClaimableEvent claimableEvent = claimableEvents.get(0);
        assertThat(claimableEvent.neoN3Token, Matchers.is(nep17Token.getScriptHash()));
        assertThat(claimableEvent.nonce, Matchers.is(nonce));
        assertThat(claimableEvent.to, Matchers.is(to));
        assertThat(claimableEvent.amount, Matchers.is(amount));

        txHash = bridge.claimToken(alice, nep17Token.getScriptHash(), nonce);
        List<TestHelper.ClaimEvent> claimEvents = getTokenClaimEvents(txHash, neow3j, bridge.getScriptHash());
        assertThat(claimEvents, hasSize(1));
        TestHelper.ClaimEvent claimEvent = claimEvents.get(0);
        assertThat(claimEvent.neoN3Token, Matchers.is(nep17Token.getScriptHash()));
        assertThat(claimEvent.nonce, is(nonce));
        assertThat(claimEvent.to, is(to));
        assertThat(claimEvent.amount, is(amount));
    }

    /***
     * Use withdrawToken with to as an EOA (not a contract) and the bridge contract having insufficient balance for the provided amount leading to a failed transfer)
     * Use claimToken for and check event TokenClaim (in order to do this, make the NEP-17 allocate more funds to the bridge to make a claim transfer successful)
     * @throws Throwable
     */
    @Test
    @Order(2)
    public static void withdrawalToEOA() throws Throwable {
        Hash160 to = new Hash160("0x6472bf811b33b87f7872e31439cdbd16871d8cad");
        BigInteger amount = new BigInteger("1000000000");
        BigInteger nonce = incrementAndGetWithdrawalNonce();

        String withdrawRootBefore = bridge.tokenWithdrawalRoot(nep17Token.getScriptHash());
        String d1 = createTokenOpHash(nep17Token.getScriptHash(), nep17TokenXTokenHash, nonce, to, amount);
        String root = concatAndSha256(withdrawRootBefore, d1);
        List<Account> validators = Arrays.asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter withdrawal = array(array(integer(nonce), hash160(to), integer(amount)));

        Hash256 txHash = bridge.withdrawToken(nep17Token.getScriptHash(), root, signMsg(validators, root), withdrawal);
        printTransactionFee(neow3j, "tx with 1 withdrawals to claim", txHash);

        List<network.bane.util.TestHelper.ClaimableEvent> claimableEvents = getTokenClaimableEvents(txHash, neow3j, bridge.getScriptHash());
        assertThat(claimableEvents, hasSize(1));
        network.bane.util.TestHelper.ClaimableEvent claimableEvent = claimableEvents.get(0);
        assertThat(claimableEvent.neoN3Token, Matchers.is(nep17Token.getScriptHash()));
        assertThat(claimableEvent.nonce, Matchers.is(nonce));
        assertThat(claimableEvent.to, Matchers.is(to));
        assertThat(claimableEvent.amount, Matchers.is(amount));

        TransactionConfigurationException thrown =
                assertThrows(TransactionConfigurationException.class, () ->
                        bridge.claimToken(alice, nep17Token.getScriptHash(), nonce)
                );
        assertThat(thrown.getMessage(),
                containsString("Claim transfer failed."));
    }

    /***
     * Fail to claim if bridge is paused
     * @throws Throwable
     */
    @Test
    @Order(3)
    public void testPauseBridge_claim() throws Throwable {
        bridge.pauseBridge();
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.claimToken(alice, nep17Token.getScriptHash(), BigInteger.ONE));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Contract is paused."));
        bridge.unpause();
    }

    @Test
    @Order(4)
    public void testPauseTokenBridge_claim() throws Throwable {
        bridge.pauseTokenBridge(nep17Token.getScriptHash());
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.claimToken(alice, nep17Token.getScriptHash(), BigInteger.ONE));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Token bridge is paused."));
        bridge.unpauseTokenBridge(nep17Token.getScriptHash());
    }


    /***
     * Fail to withdraw if token bridge is not registered
     */
    @Test
    @Order(5)
    public void testClaim_abortIfNotRegistered() throws Throwable {
        bridge.pauseTokenBridge(nep17Token.getScriptHash());
        unregisterTokenBridge(nep17Token.getScriptHash());
        HashMap<ContractParameter, ContractParameter> map = new HashMap<>();
        // Map content doesn't matter for this test, just required to have at least one entry.
        map.put(integer(0), integer(0));

        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.claimToken(alice, nep17Token.getScriptHash(), BigInteger.TEN));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Token not registered."));
        registerTokenBridge(nep17Token.getScriptHash());
    }

    /***
     * Fail to claim if there's nothing to claim for the provided nonce
     * @throws Throwable
     */
    @Test
    @Order(6)
    public void testClaim_abortIfInvalidNonce() throws Throwable {
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.claimToken(alice, nep17Token.getScriptHash(), BigInteger.TEN));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No claim for this nonce."));
    }

    @DeployConfig(BridgeManagementContract.class)
    public static DeployConfiguration deployConfigManagement() {
        return createBridgeManagementDeployConfig();
    }

    @DeployConfig(BridgeContract.class)
    public static DeployConfiguration deployConfigBridge() {
        return createBridgeDeployConfig();
    }

    @DeployConfig(Nep17TokenContract.class)
    public static DeployConfiguration deployConfigTest() {
        return createTestDeployConfig();
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
