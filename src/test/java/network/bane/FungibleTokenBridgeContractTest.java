package network.bane;

import io.neow3j.contract.GasToken;
import io.neow3j.contract.NeoToken;
import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoApplicationLog;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.protocol.core.response.Notification;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.test.ContractTest;
import io.neow3j.test.ContractTestExtension;
import io.neow3j.transaction.Transaction;
import io.neow3j.transaction.TransactionBuilder;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.types.NeoVMStateType;
import io.neow3j.wallet.Account;
import network.bane.util.TestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static io.neow3j.utils.Numeric.reverseHexString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ContractTest(blockTime = 1, contracts = FungibleTokenBridgeContract.class, batchFile = "setup.batch")
public class FungibleTokenBridgeContractTest {

    private static SmartContract contract;
    private static Neow3j neow3j;
    private static GasToken gasToken;

    private static Account alice;
    private static Account bob;
    private static Account charlie;
    private static Account denise;
    private static Account eve;
    private static Account florian;

    private static String evmRecipient;
    private static Hash160 evmRecipientHash;

    @RegisterExtension
    public static final ContractTestExtension ext = new ContractTestExtension();

    @BeforeAll
    public static void setUp() throws Exception {
        contract = ext.getDeployedContract(FungibleTokenBridgeContract.class);
        neow3j = ext.getNeow3j();
        gasToken = new GasToken(neow3j);

        alice = ext.getAccount(TestHelper.ALICE);
        bob = ext.getAccount(TestHelper.BOB);
        charlie = ext.getAccount(TestHelper.CHARLIE);
        denise = ext.getAccount(TestHelper.DENISE);
        eve = ext.getAccount(TestHelper.EVE);
        florian = ext.getAccount(TestHelper.FLORIAN);

        evmRecipient = "0x1eC3A51B9137e5588A402fA15c422B72C0d7b533";
        evmRecipientHash = new Hash160(evmRecipient);

        Hash160 scriptHash = Account.create().getScriptHash();
        System.out.println(scriptHash);
    }

    @Test
    public void testDepositGas() throws Throwable {
        BigInteger amount = gasToken.toFractions(new BigDecimal("42"));
        BigInteger gasLimit = BigInteger.valueOf(42_000);
        ContractParameter data = ContractParameter.array(
                ContractParameter.hash160(evmRecipientHash),
                ContractParameter.integer(42_000),
                null);

        assertThat(gasToken.getBalanceOf(alice), greaterThan(amount));

        TransactionBuilder b = gasToken.transfer(
                alice,
                contract.getScriptHash(),
                amount,
                data);
        Transaction tx = b.sign();
        NeoSendRawTransaction response = tx.send();

        assertFalse(response.hasError());

        Hash256 txHash = response.getSendRawTransaction().getHash();
        waitUntilTransactionIsExecuted(txHash, neow3j);

        NeoApplicationLog.Execution exec = neow3j.getApplicationLog(txHash).send().getApplicationLog().getExecution(0);
        assertThat(exec.getStack(), hasSize(1));
        assertTrue(exec.getFirstStackItem().getBoolean());

        assertThat(exec.getNotifications(), hasSize(2));
        Notification event1 = exec.getFirstNotification();
        assertThat(event1.getContract(), is(GasToken.SCRIPT_HASH));
        assertThat(event1.getEventName(), is("Transfer"));

        List<StackItem> event1State = event1.getState().getList();
        assertThat(event1State, hasSize(3));
        assertThat(new Hash160(reverseHexString(event1State.get(0).getHexString())), is(alice.getScriptHash()));
        assertThat(new Hash160(reverseHexString(event1State.get(1).getHexString())), is(contract.getScriptHash()));
        assertThat(event1State.get(2).getInteger(), is(amount));

        Notification event2 = exec.getNotification(1);
        assertThat(event2.getContract(), is(contract.getScriptHash()));
        assertThat(event2.getEventName(), is("OnDeposit"));

        List<StackItem> event2State = event2.getState().getList();
        assertThat(event2State, hasSize(5));
        assertThat(new Hash160(reverseHexString(event2State.get(0).getHexString())), is(GasToken.SCRIPT_HASH));
        assertThat(new Hash160(reverseHexString(event2State.get(1).getHexString())), is(alice.getScriptHash()));
        assertThat(new Hash160(reverseHexString(event2State.get(2).getHexString())), is(evmRecipientHash));
        assertThat(event2State.get(3).getInteger(), is(amount));
        assertThat(event2State.get(4).getInteger(), is(gasLimit));
    }

    @Test
    public void testDepositNeo_shouldFail() throws Throwable {
        neow3j.allowTransmissionOnFault();

        NeoToken neoToken = new NeoToken(neow3j);
        BigInteger amount = BigInteger.TEN;
        assertThat(neoToken.getBalanceOf(alice), greaterThan(amount));
        TransactionBuilder b = neoToken.transfer(
                alice,
                contract.getScriptHash(),
                amount,
                ContractParameter.hash160(evmRecipientHash));
        Transaction tx = b.sign();
        NeoSendRawTransaction response = tx.send();
        Hash256 txHash = response.getSendRawTransaction().getHash();
        waitUntilTransactionIsExecuted(txHash, neow3j);

        NeoApplicationLog log = neow3j.getApplicationLog(txHash).send().getApplicationLog();
        List<NeoApplicationLog.Execution> execs = log.getExecutions();
        assertThat(execs, hasSize(1));
        assertThat(execs.get(0).getState(), is(NeoVMStateType.FAULT));
        assertThat(execs.get(0).getException(), containsString("ABORT is executed"));

        neow3j.preventTransmissionOnFault();
    }

    @Test
    public void testDeposit_invalidDataHash160() throws Throwable {
        neow3j.allowTransmissionOnFault();

        BigInteger amount = gasToken.toFractions(new BigDecimal("12.2"));
        assertThat(gasToken.getBalanceOf(alice), greaterThan(amount));
        TransactionBuilder b = gasToken.transfer(
                alice,
                contract.getScriptHash(),
                amount,
                ContractParameter.string(evmRecipient));
        Transaction tx = b.sign();
        NeoSendRawTransaction response = tx.send();
        Hash256 txHash = response.getSendRawTransaction().getHash();
        waitUntilTransactionIsExecuted(txHash, neow3j);

        NeoApplicationLog log = neow3j.getApplicationLog(txHash).send().getApplicationLog();
        List<NeoApplicationLog.Execution> execs = log.getExecutions();
        assertThat(execs, hasSize(1));
        assertThat(execs.get(0).getState(), is(NeoVMStateType.FAULT));
        assertThat(execs.get(0).getException(), containsString("ABORT is executed"));

        neow3j.preventTransmissionOnFault();
    }

}
