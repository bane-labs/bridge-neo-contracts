package network.bane.bridge;

import io.neow3j.contract.NefFile;
import io.neow3j.protocol.ObjectMapperFactory;
import io.neow3j.protocol.core.response.ContractManifest;
import io.neow3j.protocol.core.response.ContractStorageEntry;
import io.neow3j.protocol.core.response.NeoApplicationLog;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.protocol.core.response.Notification;
import io.neow3j.protocol.core.stackitem.ArrayStackItem;
import io.neow3j.protocol.core.stackitem.IntegerStackItem;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.test.ContractTest;
import io.neow3j.test.ContractTestExtension;
import io.neow3j.test.DeployConfig;
import io.neow3j.test.DeployConfiguration;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.types.NeoVMStateType;
import io.neow3j.utils.Await;
import io.neow3j.utils.Numeric;
import io.neow3j.wallet.Account;
import network.bane.management.BridgeManagementContract;
import network.bane.testhelper.TestContract;
import network.bane.util.structs.GasBridge;
import network.bane.util.structs.State;
import network.bane.util.TestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.types.ContractParameter.any;
import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.types.ContractParameter.string;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static java.util.Arrays.asList;
import static network.bane.util.TestHelper.getClaimEvents;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_GAS_DEPOSIT_FEE;
import static network.bane.util.helper.DefaultTestValues.MANAGEMENT_CONTRACT_HASH;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_MAX_GAS_DEPOSIT;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_MAX_WITHDRAWALS;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_MIN_GAS_DEPOSIT;
import static network.bane.util.helper.PrintHelper.printTransactionFee;
import static network.bane.util.TestHelper.concatAndSha256;
import static network.bane.util.TestHelper.createDepositHash;
import static network.bane.util.TestHelper.getClaimableEvents;
import static network.bane.util.TestHelper.getDepositEvents;
import static network.bane.util.TestHelper.getWithdrawEvents;
import static network.bane.util.TestHelper.hasFiredEvent;
import static network.bane.util.TestHelper.owner;
import static network.bane.util.TestHelper.recipient0;
import static network.bane.util.TestHelper.recipient1;
import static network.bane.util.TestHelper.recipient2;
import static network.bane.util.TestHelper.recipient3;
import static network.bane.util.TestHelper.recipient4;
import static network.bane.util.TestHelper.relayer;
import static network.bane.util.TestHelper.securityGuard;
import static network.bane.util.TestHelper.setDepositFee;
import static network.bane.util.TestHelper.setMaxGasDeposit;
import static network.bane.util.TestHelper.setMinDeposit;
import static network.bane.util.TestHelper.signMsg;
import static network.bane.util.TestHelper.validator1;
import static network.bane.util.TestHelper.validator2;
import static network.bane.util.TestHelper.validator3;
import static network.bane.util.TestHelper.validator4;
import static network.bane.util.TestHelper.validator5;
import static network.bane.util.helper.TestHelper.alice;
import static network.bane.util.helper.TestHelper.bob;
import static network.bane.util.helper.TestHelper.bridge;
import static network.bane.util.helper.TestHelper.charlie;
import static network.bane.util.helper.TestHelper.createBridgeDeployConfig;
import static network.bane.util.helper.TestHelper.createBridgeManagementDeployConfig;
import static network.bane.util.helper.TestHelper.denise;
import static network.bane.util.helper.TestHelper.gasToken;
import static network.bane.util.helper.TestHelper.incrementAndGetDepositNonce;
import static network.bane.util.helper.TestHelper.incrementAndGetWithdrawalNonce;
import static network.bane.util.helper.TestHelper.neoToken;
import static network.bane.util.helper.TestHelper.neow3j;
import static network.bane.util.helper.TestHelper.setup;
import static network.bane.util.helper.TestHelper.testContract;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ContractTest(
        blockTime = 1,
        contracts = {BridgeManagementContract.class, BridgeContract.class, TestContract.class},
        batchFile = "setup.batch"
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BridgeTest {

    @RegisterExtension
    public static final ContractTestExtension ext = new ContractTestExtension();

    @BeforeAll
    public static void setUp() throws Throwable {
        setup(ext);
    }

    @DeployConfig(BridgeManagementContract.class)
    public static DeployConfiguration deployConfigManagement() {
        return createBridgeManagementDeployConfig();
    }

    @DeployConfig(BridgeContract.class)
    public static DeployConfiguration deployConfigBridge() {
        return createBridgeDeployConfig();
    }

    // region helper gas

    private Hash256 depositGasUsingDepositMethod(Account from, Hash160 to, BigInteger amount) throws Throwable {
        return depositGasUsingDepositMethod(from, to, amount, DEFAULT_GAS_DEPOSIT_FEE);
    }

    private Hash256 depositGasUsingDepositMethod(Account from, Hash160 to, BigInteger amount, BigInteger maxFee) throws Throwable {
        Hash256 txHash = bridge.depositGas(from, to, amount, maxFee);
        printTransactionFee(neow3j, "deposit", txHash);
        return txHash;
    }

    private Hash256 depositGasWithDirectTransfer(Account from, Hash160 to, BigInteger amount,
            BigInteger minBridgeAmount) throws Throwable {
        NeoSendRawTransaction response = gasToken.transfer(from, bridge.getScriptHash(), amount, array(hash160(to),
                        integer(minBridgeAmount)))
                .sign()
                .send();
        Hash256 txHash = response.getSendRawTransaction().getHash();
        Await.waitUntilTransactionIsExecuted(txHash, neow3j);
        printTransactionFee(neow3j, "deposit direct", txHash);
        return txHash;
    }

    // endregion
    // endregion
    // region deployment

    @Test
    @Order(0)
    public void testDeployment_deploymentDataSetCorrectly() throws IOException {
        assertThat(bridge.findStorage("0x0a"), hasSize(4));
        ContractStorageEntry managementEntry = bridge.findStorage("0x0a").get(0);
        ContractStorageEntry pauseEntry = bridge.findStorage("0x0a").get(1);
        ContractStorageEntry gasBridgeEntry = bridge.findStorage("0x0a").get(2);
        ContractStorageEntry unclaimedRewardsEntry = bridge.findStorage("0x0a").get(3);

        assertThat(managementEntry.getKeyHex(), is("0x0a01"));
        assertArrayEquals(managementEntry.getValue(), MANAGEMENT_CONTRACT_HASH.toLittleEndianArray());
        assertThat(pauseEntry.getKeyHex(), is("0x0a02"));
        assertArrayEquals(pauseEntry.getValue(), new byte[]{});
        assertThat(gasBridgeEntry.getKeyHex(), is("0x0a03"));
        String expectedStorageValue = "0x4004" + // array size 4
                "2100" + // integer - pause
                "4002" + // array size 2 - deposit state
                "2100" + // integer 0
                "28200000000000000000000000000000000000000000000000000000000000000000" + // bytestring size 32
                "4002" + // array size 2 - withdrawal state
                "2100" +
                "28200000000000000000000000000000000000000000000000000000000000000000" + // bytestring size 32
                "4004" + // array size 4 - config
                "210480969800" + // integer 10000000
                "210400e1f505" + // integer 100000000
                "21060010a5d4e800" + // integer 1000000000000
                "210164"; // integer 100
        assertThat(gasBridgeEntry.getValueHex(), is(expectedStorageValue));

        assertThat(bridge.management(), is(MANAGEMENT_CONTRACT_HASH));

        GasBridge expectedGasBridge = new GasBridge(false, State.newState(), State.newState(),
                new GasBridge.GasConfig(
                        DEFAULT_GAS_DEPOSIT_FEE,
                        DEFAULT_MIN_GAS_DEPOSIT,
                        DEFAULT_MAX_GAS_DEPOSIT,
                        DEFAULT_MAX_WITHDRAWALS)
        );
        assertTrue(bridge.getGasBridge().equals(expectedGasBridge));
        assertThat(bridge.gasDepositFee(), is(DEFAULT_GAS_DEPOSIT_FEE));
        assertThat(bridge.minGasDeposit(), is(DEFAULT_MIN_GAS_DEPOSIT));
        assertThat(bridge.maxGasDeposit(), is(DEFAULT_MAX_GAS_DEPOSIT));
        assertThat(bridge.gasDepositNonce(), is(BigInteger.ZERO));
        assertThat(bridge.gasDepositRoot(), is(Numeric.toHexString(Hash256.ZERO.toArray())));
        assertThat(bridge.gasWithdrawalNonce(), is(BigInteger.ZERO));
        assertThat(bridge.gasWithdrawRoot(), is(Numeric.toHexString(Hash256.ZERO.toArray())));

        assertThat(unclaimedRewardsEntry.getValueHex(), is("0x"));
    }

    // endregion
    // region rejected deposits

    @Test
    @Order(0)
    public void testDeposit_abortIfNotRegistered() {
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class, () ->
                neoToken.transfer(alice, bridge.getScriptHash(), BigInteger.ONE).sign().send());
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Unregistered token."));
    }

    @Test
    @Order(0)
    public void testDeposit_assertFailIfInvalidRecipientData() {
        ContractParameter dataParam = integer(42_000);
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class, () ->
                gasToken.transfer(alice, bridge.getScriptHash(), BigInteger.ONE, dataParam).sign().send());
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Invalid payment data."));
    }

    @Test
    @Order(0)
    public void testDeposit_abortIfInvalidDataInOnNEP17PaymentMethod() throws Throwable {
        TransactionConfigurationException thrown =
                assertThrows(TransactionConfigurationException.class, () ->
                        gasToken.transfer(
                                alice,
                                bridge.getScriptHash(),
                                DEFAULT_MIN_GAS_DEPOSIT,
                                string(recipient0.toString())
                        ).sign()
                );
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Invalid payment data."));

        NeoSendRawTransaction response = gasToken.transfer(alice, bridge.getScriptHash(), DEFAULT_MIN_GAS_DEPOSIT)
                .sign()
                .send();
        assertFalse(response.hasError());
        Hash256 txHash = response.getSendRawTransaction().getHash();
        Await.waitUntilTransactionIsExecuted(txHash, neow3j);
        assertThat(neow3j.getApplicationLog(txHash).send().getApplicationLog().getFirstExecution().getState(),
                is(NeoVMStateType.HALT));

        // Use same length as Hash160 but not zero.
        byte[] newArr = new byte[20];
        Arrays.fill(newArr, (byte) 1);
        assertThat(newArr.length, is(20));
        thrown = assertThrows(TransactionConfigurationException.class, () ->
                gasToken.transfer(
                        alice,
                        bridge.getScriptHash(),
                        DEFAULT_MIN_GAS_DEPOSIT.add(bridge.gasDepositFee()),
                        array(array(newArr), integer(DEFAULT_MIN_GAS_DEPOSIT))
                ).sign()
        );
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Invalid payment data."));

        Arrays.fill(newArr, (byte) 0);
        assertThat(newArr[5], is((byte) 0));
        thrown = assertThrows(TransactionConfigurationException.class, () ->
                gasToken.transfer(
                        alice,
                        bridge.getScriptHash(),
                        DEFAULT_MIN_GAS_DEPOSIT.add(bridge.gasDepositFee()),
                        array(array(newArr), integer(DEFAULT_MIN_GAS_DEPOSIT))
                ).sign()
        );
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Invalid payment data."));

        thrown = assertThrows(TransactionConfigurationException.class, () ->
                gasToken.transfer(
                        alice,
                        bridge.getScriptHash(),
                        DEFAULT_MIN_GAS_DEPOSIT.add(bridge.gasDepositFee()),
                        array(hash160(Hash160.ZERO), integer(DEFAULT_MIN_GAS_DEPOSIT))
                ).sign()
        );
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Invalid payment data."));
    }

    // endregion
    // region deposits

    @Test
    @Order(11)
    public void testRootComputation_1() throws Throwable {
        // This test only works if it is the first test in the order of deposits, due to the use of raw data for the
        // nonce, to and amount.
        Account from = alice;
        incrementAndGetDepositNonce(); // Necessary if other tests are run besides this one.
        BigInteger nextNonce = BigInteger.ONE;
        Hash160 to = new Hash160("0x70997970C51812dc3A010C7d01b50e0d17dc79C8");
        BigInteger amount = new BigInteger("110000000");

        String depositRootBefore = bridge.gasDepositRoot();
        Hash256 txHash = depositGasUsingDepositMethod(from, to, amount);

        assertThat(bridge.gasDepositFee(), is(new BigInteger("10000000")));
        String d1 = createDepositHash(nextNonce, to, amount.subtract(bridge.gasDepositFee()));
        // Raw deposit hash and root. The same inputs and the same deposit and root hash are used in a Neo X test.
        assertThat(d1, is("0x20aad97e2860b1934184ffb2b04ea45d49af5145fa43cb212027f9e8e728baea"));
        String newRoot = concatAndSha256(depositRootBefore, d1);
        assertThat(newRoot, is("0xdda77cac690580c1e5220d377cd807b4d2ed89751e4078525ee54297182d3d88"));
        assertThat(bridge.gasDepositRoot(), is(newRoot));

        List<TestHelper.DepositEvent> depositEvents = getDepositEvents(txHash, neow3j, bridge.getScriptHash());
        assertThat(depositEvents, hasSize(1));
        TestHelper.DepositEvent depositEvent = depositEvents.get(0);
        assertThat(depositEvent.nonce, is(nextNonce));
        assertThat(depositEvent.from, is(from.getScriptHash()));
        assertThat(depositEvent.to, is(to));
        assertThat(depositEvent.amount, is(amount.subtract(bridge.gasDepositFee())));
        assertThat(depositEvent.depositHashHex, is(d1));
        assertThat(depositEvent.rootHashHex, is(newRoot));
    }

    @Test
    @Order(12)
    public void testRootComputation_2() throws Throwable {
        Account from = bob;
        BigInteger nextNonce = new BigInteger("2");
        incrementAndGetDepositNonce();
        Hash160 to = new Hash160("0x89fc6b042b146f373cec4ca4a1b112697763360f");
        BigInteger sentAmount = DEFAULT_MIN_GAS_DEPOSIT.add(bridge.gasDepositFee());
        assertThat(sentAmount, is(new BigInteger("110000000")));

        String depositRootBefore = bridge.gasDepositRoot();
        Hash256 txHash = depositGasWithDirectTransfer(from, to, sentAmount, DEFAULT_MIN_GAS_DEPOSIT);

        // If GAS is directly sent to the bridge providing a Hash160 value as data, the transfer initiates a bridge
        // operation. In this case, the deposit fee is deducted from the sent amount and the remaining amount is
        // used for the deposit to Neo X.
        BigInteger resultingBridgeAmount = sentAmount.subtract(bridge.gasDepositFee());
        String d2 = createDepositHash(nextNonce, to, resultingBridgeAmount);
        assertThat(d2, is("0x983b3a1ee6b74a5ba07109966f86189d2adf108a25fd9b456030f684bffa8f84"));
        String d12 = concatAndSha256(depositRootBefore, d2);
        assertThat(d12, is("0xa33a32b7b710705118b37d5fa7684a26e63840e7b5b88326177e7ec4ee7be253"));

        assertThat(bridge.gasDepositRoot(), is(d12));

        List<TestHelper.DepositEvent> depositEvents = getDepositEvents(txHash, neow3j, bridge.getScriptHash());
        assertThat(depositEvents, hasSize(1));
        TestHelper.DepositEvent depositEvent = depositEvents.get(0);
        assertThat(depositEvent.nonce, is(nextNonce));
        assertThat(depositEvent.from, is(from.getScriptHash()));
        assertThat(depositEvent.to, is(to));
        assertThat(depositEvent.amount, is(resultingBridgeAmount));
        assertThat(depositEvent.depositHashHex, is(d2));
        assertThat(depositEvent.rootHashHex, is(d12));
    }

    @Test
    @Order(13)
    public void testRootComputation_3() throws Throwable {
        Account from = charlie;
        Hash160 to = recipient3;
        BigInteger amount = DEFAULT_MIN_GAS_DEPOSIT.multiply(new BigInteger("3"));
        BigInteger nextNonce = incrementAndGetDepositNonce();

        String depositRootBefore = bridge.gasDepositRoot();
        Hash256 txHash = depositGasUsingDepositMethod(from, to, amount);

        String d3 = createDepositHash(nextNonce, to, amount.subtract(bridge.gasDepositFee()));
        String d12d3 = concatAndSha256(depositRootBefore, d3);

        assertThat(bridge.gasDepositRoot(), is(d12d3));

        List<TestHelper.DepositEvent> depositEvents = getDepositEvents(txHash, neow3j, bridge.getScriptHash());
        assertThat(depositEvents, hasSize(1));
        TestHelper.DepositEvent depositEvent = depositEvents.get(0);
        assertThat(depositEvent.nonce, is(nextNonce));
        assertThat(depositEvent.from, is(from.getScriptHash()));
        assertThat(depositEvent.to, is(to));
        assertThat(depositEvent.amount, is(amount.subtract(bridge.gasDepositFee())));
        assertThat(depositEvent.depositHashHex, is(d3));
        assertThat(depositEvent.rootHashHex, is(d12d3));
    }

    @Test
    @Order(14)
    public void testRootComputation_4() throws Throwable {
        Account from = denise;
        Hash160 to = recipient4;
        BigInteger amount = DEFAULT_MIN_GAS_DEPOSIT.multiply(new BigInteger("4"));
        BigInteger nextNonce = incrementAndGetDepositNonce();

        String depositRootBefore = bridge.gasDepositRoot();

        Hash256 txHash = depositGasUsingDepositMethod(from, to, amount);

        String depositHashOffChain = createDepositHash(nextNonce, to, amount.subtract(bridge.gasDepositFee()));
        String d1234 = concatAndSha256(depositRootBefore, depositHashOffChain);

        assertThat(bridge.gasDepositRoot(), is(d1234));

        List<TestHelper.DepositEvent> depositEvents = getDepositEvents(txHash, neow3j, bridge.getScriptHash());
        assertThat(depositEvents, hasSize(1));
        TestHelper.DepositEvent depositEvent = depositEvents.get(0);
        assertThat(depositEvent.nonce, is(nextNonce));
        assertThat(depositEvent.from, is(from.getScriptHash()));
        assertThat(depositEvent.to, is(to));
        assertThat(depositEvent.amount, is(amount.subtract(bridge.gasDepositFee())));
        assertThat(depositEvent.depositHashHex, is(depositHashOffChain));
        assertThat(depositEvent.rootHashHex, is(d1234));
    }

    @Test
    @Order(15)
    public void testTryingToDepositWithBridgeContractAsFrom() throws Throwable {
        BigInteger amount = DEFAULT_MIN_GAS_DEPOSIT.multiply(new BigInteger("3"));
        assertThat(gasToken.getBalanceOf(bridge.getScriptHash()), greaterThan(amount));
        TransactionConfigurationException thrown =
                assertThrows(TransactionConfigurationException.class,
                        () -> bridge.depositGas(alice, bridge.getScriptHash(), recipient0, amount));
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Invalid sender."));
    }

    @Test
    @Order(16)
    public void testUsingTooHighMinDepositAmount() throws Throwable {
        Account from = bob;
        Hash160 to = recipient2;
        BigInteger amount = DEFAULT_MIN_GAS_DEPOSIT.add(bridge.gasDepositFee()).subtract(BigInteger.ONE);

        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> depositGasWithDirectTransfer(from, to, amount, DEFAULT_MIN_GAS_DEPOSIT));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Amount below defined minimum."));
    }

    // endregion
    // region withdrawal and claim

    @Test
    @Order(20)
    public void testWithdrawal_1() throws Throwable {
        // This test only works if it is the first test in the order of withdrawals, due to the use of raw data for the
        // nonce, to and amount.
        BigInteger nonce = BigInteger.ONE;
        incrementAndGetWithdrawalNonce(); // Necessary if other tests are run besides this one.
        Hash160 to = new Hash160("0x6472bf811b33b87f7872e31439cdbd16871d8cad");
        BigInteger amount = new BigInteger("200000000");

        String d1 = createDepositHash(nonce, to, amount);
        assertThat(d1, is("0x9d532058b0e607e4767a9c6b6915d1c97019adfcb50cafe1001904cbbaf6a3bb"));
        String root = concatAndSha256(Hash256.ZERO.toString(), d1);
        assertThat(root, is("0x177604db278d7680e254218a72dcc06043600f9c9cb6eb56055cd17fb81fa0b9"));
        List<Account> validators = Arrays.asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter withdrawal = array(array(integer(nonce), integer(amount), hash160(to)));
        Hash256 txHash = bridge.withdrawGas(root, signMsg(validators, root), withdrawal);
        printTransactionFee(neow3j, "tx with 1 withdrawals", txHash);
        List<TestHelper.WithdrawEvent> withdrawEvents = getWithdrawEvents(txHash, neow3j, bridge.getScriptHash());
        assertThat(withdrawEvents, hasSize(1));
        TestHelper.WithdrawEvent withdrawEvent = withdrawEvents.get(0);
        assertThat(withdrawEvent.nonce, is(nonce));
        assertThat(withdrawEvent.to, is(to));
        assertThat(withdrawEvent.amount, is(amount));
    }

    @Test
    @Order(21)
    public void testWithdrawal_2() throws Throwable {
        BigInteger nonce1 = new BigInteger("2");
        Hash160 to1 = new Hash160("0x9f20eee56cc5abe5955ee5e15835dc45344c368d");
        BigInteger amount1 = new BigInteger("100000000");
        incrementAndGetWithdrawalNonce();
        BigInteger nonce2 = new BigInteger("3");
        Hash160 to2 = new Hash160("0xa71fbffad1f175bfddb1e81a63962212d2e72e8f");
        BigInteger amount2 = new BigInteger("100000000");
        incrementAndGetWithdrawalNonce();

        String withdrawRootBefore = bridge.gasWithdrawRoot();

        String d1 = createDepositHash(nonce1, to1, amount1);
        assertThat(d1, is("0x32a41a5e3d27d9308a10d85281a0cee7e0f9bbfe5ac417fa21cf0ec8464be5bb"));
        String root1 = concatAndSha256(withdrawRootBefore, d1);
        assertThat(root1, is("0x38d6ba25f87e2a128ab82f815b5f1f84191da961cf4b58029eee2d33e9640a3a"));
        String d2 = createDepositHash(nonce2, to2, amount2);
        assertThat(d2, is("0x5355bdf08f3429f0ca6c15bea7d6bc147a7fb5072c782ff72e350e25ee683ad1"));
        String newRoot = concatAndSha256(root1, d2);
        assertThat(newRoot, is("0x005bac323a5f98e0ba3d41d71573853e37cc28649cd95ea703ab0f9e462573bd"));

        List<Account> validators = Arrays.asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter withdrawal =
                array(array(integer(nonce1), integer(amount1), hash160(to1)), array(integer(nonce2), integer(amount2),
                        hash160(to2)));
        Hash256 txHash = bridge.withdrawGas(newRoot, signMsg(validators, newRoot), withdrawal);
        printTransactionFee(neow3j, "tx with 2 withdrawals", txHash);
        List<TestHelper.WithdrawEvent> withdrawEvents = getWithdrawEvents(txHash, neow3j, bridge.getScriptHash());
        assertThat(withdrawEvents, hasSize(2));
        TestHelper.WithdrawEvent withdrawEvent1 = withdrawEvents.get(0);
        assertThat(withdrawEvent1.nonce, is(nonce1));
        assertThat(withdrawEvent1.to, is(to1));
        assertThat(withdrawEvent1.amount, is(amount1));
        TestHelper.WithdrawEvent withdrawEvent2 = withdrawEvents.get(1);
        assertThat(withdrawEvent2.nonce, is(nonce2));
        assertThat(withdrawEvent2.to, is(to2));
        assertThat(withdrawEvent2.amount, is(amount2));
    }

    @Test
    @Order(22)
    public void testWithdrawalToContract() throws Throwable {
        Hash160 to = testContract;
        BigInteger amount = DEFAULT_MIN_GAS_DEPOSIT;
        BigInteger nonce = incrementAndGetWithdrawalNonce();

        String withdrawRootBefore = bridge.gasWithdrawRoot();
        String d1 = createDepositHash(nonce, to, amount);
        String root = concatAndSha256(withdrawRootBefore, d1);
        List<Account> validators = Arrays.asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter withdrawal = array(array(integer(nonce), integer(amount), hash160(to)));

        Hash256 txHash = bridge.withdrawGas(root, signMsg(validators, root), withdrawal);
        printTransactionFee(neow3j, "tx with 1 withdrawals to claim", txHash);

        List<TestHelper.ClaimableEvent> claimableEvents = getClaimableEvents(txHash, neow3j, bridge.getScriptHash());
        assertThat(claimableEvents, hasSize(1));
        TestHelper.ClaimableEvent claimableEvent = claimableEvents.get(0);
        assertThat(claimableEvent.nonce, is(nonce));
        assertThat(claimableEvent.to, is(to));
        assertThat(claimableEvent.amount, is(amount));
    }

    @Test
    @Order(23)
    public void testMultipleWithdrawals() throws Throwable {
        Hash160 to1 = recipient0;
        Hash160 to2 = recipient1;
        Hash160 to3 = recipient2;
        Hash160 to4 = recipient3;
        // The following two assertions will make sure that:
        // - the first withdrawal is successful, leaving a contract balance of less than 12 but more than 5 gas.
        // - the second withdrawal is not successful due to insufficient funds.
        // - the third withdrawal is successful, leaving a contract balance of less than 5 gas.
        // - the fourth withdrawal is not successful due to insufficient funds.
        // This scenario should never happen since the contract should hold all the gas that was deposited and can be
        // withdrawn from Neo X. Nevertheless, the proper functionality of adding a claimable if a transfer fails is
        // tested here.
        assertThat(gasToken.getBalanceOf(bridge.getScriptHash()), lessThan(new BigInteger("1400000000")));
        assertThat(gasToken.getBalanceOf(bridge.getScriptHash()), greaterThanOrEqualTo(new BigInteger("600000000")));
        BigInteger amount1 = DEFAULT_MIN_GAS_DEPOSIT.multiply(new BigInteger("2"));
        BigInteger amount2 = DEFAULT_MIN_GAS_DEPOSIT.multiply(new BigInteger("12"));
        BigInteger amount3 = DEFAULT_MIN_GAS_DEPOSIT.multiply(new BigInteger("4"));
        BigInteger amount4 = DEFAULT_MIN_GAS_DEPOSIT.multiply(new BigInteger("9"));
        BigInteger nonce1 = incrementAndGetWithdrawalNonce();
        BigInteger nonce2 = incrementAndGetWithdrawalNonce();
        BigInteger nonce3 = incrementAndGetWithdrawalNonce();
        BigInteger nonce4 = incrementAndGetWithdrawalNonce();

        String rootBefore = bridge.gasWithdrawRoot();
        String d1 = createDepositHash(nonce1, to1, amount1);
        String root = concatAndSha256(rootBefore, d1);
        String d2 = createDepositHash(nonce2, to2, amount2);
        root = concatAndSha256(root, d2);
        String d3 = createDepositHash(nonce3, to3, amount3);
        root = concatAndSha256(root, d3);
        String d4 = createDepositHash(nonce4, to4, amount4);
        root = concatAndSha256(root, d4);

        List<Account> validators = Arrays.asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter withdrawals = array(
                array(integer(nonce1), integer(amount1), hash160(to1)),
                array(integer(nonce2), integer(amount2), hash160(to2)),
                array(integer(nonce3), integer(amount3), hash160(to3)),
                array(integer(nonce4), integer(amount4), hash160(to4))
        );
        Hash256 txHash = bridge.withdrawGas(root, signMsg(validators, root), withdrawals);

        assertThat(bridge.gasWithdrawRoot(), is(root));
        NeoApplicationLog appLog = neow3j.getApplicationLog(txHash).send().getApplicationLog();
        assertThat(appLog.getFirstExecution().getNotifications(), hasSize(7));
        Notification firstNotification = appLog.getFirstExecution().getNotification(0);
        assertThat(firstNotification.getEventName(), is("GasWithdrawalRootUpdate"));
        assertThat(firstNotification.getContract(), is(bridge.getScriptHash()));
        List<StackItem> withdrawalRootUpdateEvent = firstNotification.getState().getList();
        assertThat(withdrawalRootUpdateEvent, hasSize(2));
        assertThat(withdrawalRootUpdateEvent.get(0).getInteger(), is(nonce4));
        assertThat(withdrawalRootUpdateEvent.get(1).getHexString(), is(Numeric.cleanHexPrefix(root)));
        assertThat(appLog.getFirstExecution().getNotification(1).getEventName(), is("Transfer"));
        assertThat(appLog.getFirstExecution().getNotification(2).getEventName(), is("GasWithdrawal"));
        assertThat(appLog.getFirstExecution().getNotification(3).getEventName(), is("GasClaimable"));
        assertThat(appLog.getFirstExecution().getNotification(4).getEventName(), is("Transfer"));
        assertThat(appLog.getFirstExecution().getNotification(5).getEventName(), is("GasWithdrawal"));
        assertThat(appLog.getFirstExecution().getNotification(6).getEventName(), is("GasClaimable"));
    }

    @Test
    @Order(23)
    public void testClaim() throws Throwable {
        Hash160 to = testContract;
        BigInteger amount = bridge.minGasDeposit().multiply(BigInteger.valueOf(2));
        BigInteger nextNonce = incrementAndGetWithdrawalNonce();

        String withdrawRootBefore = bridge.gasWithdrawRoot();
        String d1 = createDepositHash(nextNonce, to, amount);
        String root = concatAndSha256(withdrawRootBefore, d1);
        List<Account> validators = Arrays.asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter withdrawal = array(array(integer(nextNonce), integer(amount), hash160(to)));
        bridge.withdrawGas(root, signMsg(validators, root), withdrawal); // fund the contract if this test is executed
        // alone
        depositGasUsingDepositMethod(alice, to, amount);

        Hash256 txHash = bridge.claimGas(alice, nextNonce);
        List<TestHelper.ClaimEvent> claimEvents = getClaimEvents(txHash, neow3j, bridge.getScriptHash());
        assertThat(claimEvents, hasSize(1));
        TestHelper.ClaimEvent claimEvent = claimEvents.get(0);
        assertThat(claimEvent.nonce, is(nextNonce));
        assertThat(claimEvent.to, is(to));
        assertThat(claimEvent.amount, is(amount));
    }

    // endregion
    // region setters

    @Test
    @Order(0)
    public void testSetDepositFee() throws Throwable {
        BigInteger newFee = new BigInteger("20000000");
        assertThat(bridge.gasDepositFee(), is(not(newFee)));
        Hash256 txHash = setDepositFee(bridge, neow3j, newFee);
        BigInteger actualDepositFee = bridge.gasDepositFee();
        assertThat(actualDepositFee, is(newFee));
        assertTrue(hasFiredEvent(neow3j, txHash, bridge.getScriptHash(), "GasDepositFeeChange",
                new ArrayStackItem(asList(new IntegerStackItem(newFee)))));
        setDepositFee(bridge, neow3j, DEFAULT_GAS_DEPOSIT_FEE);
    }

    @Test
    @Order(0)
    public void testSetDepositFee_FeeisZero() throws Throwable {
        BigInteger newFee = new BigInteger("0");
        assertThat(bridge.gasDepositFee(), is(not(newFee)));
        setDepositFee(bridge, neow3j, newFee);
        BigInteger actualDepositFee = bridge.gasDepositFee();
        assertThat(actualDepositFee, is(newFee));
        setDepositFee(bridge, neow3j, DEFAULT_GAS_DEPOSIT_FEE);
    }

    @Test
    @Order(0)
    public void testSetDepositFee_abortIfFeeLowerThanZero() {
        BigInteger newFee = new BigInteger("-1");
        TransactionConfigurationException thrown =
                assertThrows(TransactionConfigurationException.class, () ->
                        setDepositFee(bridge, neow3j, newFee)
                );
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: New deposit fee must be nonnegative."));
    }

    @Test
    @Order(0)
    public void testSetDepositFee_abortGovernorNotSigner() {
        BigInteger newFee = new BigInteger("200000000");
        TransactionConfigurationException thrown =
                assertThrows(TransactionConfigurationException.class, () ->
                        bridge.invokeFunction("setGasDepositFee", integer(newFee))
                                .signers(AccountSigner.calledByEntry(alice))
                                .sign()
                                .send()
                );
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Only the governor can call this method."));
    }

    @Test
    @Order(0)
    public void testSetMinDeposit() throws Throwable {
        BigInteger newMinDeposit = new BigInteger("200000000");
        assertThat(bridge.minGasDeposit(), is(not(newMinDeposit)));
        Hash256 txHash = setMinDeposit(bridge, neow3j, newMinDeposit);
        BigInteger actualMinDeposit = bridge.minGasDeposit();
        assertThat(actualMinDeposit, is(newMinDeposit));
        assertTrue(hasFiredEvent(neow3j, txHash, bridge.getScriptHash(), "MinGasDepositChange",
                new ArrayStackItem(asList(new IntegerStackItem(newMinDeposit)))));
        setMinDeposit(bridge, neow3j, DEFAULT_MIN_GAS_DEPOSIT);
    }

    @Test
    @Order(0)
    public void testSetMinDeposit_isEqualToDepositFee() throws Throwable {
        BigInteger newMinDeposit = bridge.gasDepositFee();
        assertThat(bridge.minGasDeposit(), is(not(newMinDeposit)));

        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> setMinDeposit(bridge, neow3j, newMinDeposit));
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Minimum deposit must be greater than the deposit fee."));

        BigInteger newValidMinDeposit = newMinDeposit.add(BigInteger.ONE);
        setMinDeposit(bridge, neow3j, newValidMinDeposit);

        BigInteger actualMinDeposit = bridge.minGasDeposit();
        assertThat(actualMinDeposit, is(newValidMinDeposit));
        setMinDeposit(bridge, neow3j, DEFAULT_MIN_GAS_DEPOSIT);
    }

    @Test
    @Order(0)
    public void testSetMinDeposit_greaterThanMaxDeposit() throws Throwable {
        BigInteger newMinDeposit = bridge.maxGasDeposit().add(BigInteger.ONE);
        assertThat(bridge.minGasDeposit(), is(not(newMinDeposit)));
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> setMinDeposit(bridge, neow3j, newMinDeposit));
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Minimum must be less than the maximum amount."));
    }

    @Test
    @Order(0)
    public void testSetMinGasDeposit_abortGovernorNotSigner() throws Throwable {
        BigInteger newMinDeposit = new BigInteger("200000000");
        assertThat(bridge.minGasDeposit(), is(not(newMinDeposit)));
        TransactionConfigurationException thrown =
                assertThrows(TransactionConfigurationException.class, () ->
                        bridge.invokeFunction("setMinGasDeposit", integer(newMinDeposit))
                                .signers(AccountSigner.calledByEntry(alice))
                                .sign());
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Only the governor can call this method."));
    }

    @Test
    @Order(0)
    public void testSetMaxGasDeposit() throws Throwable {
        BigInteger newMaxDeposit = new BigInteger("20000000000");
        assertThat(bridge.maxGasDeposit(), is(not(newMaxDeposit)));
        Hash256 txHash = setMaxGasDeposit(bridge, neow3j, newMaxDeposit);
        BigInteger actualMaxDeposit = bridge.maxGasDeposit();
        assertThat(actualMaxDeposit, is(newMaxDeposit));
        assertTrue(hasFiredEvent(neow3j, txHash, bridge.getScriptHash(), "MaxGasDepositChange",
                new ArrayStackItem(asList(new IntegerStackItem(newMaxDeposit)))));
        setMaxGasDeposit(bridge, neow3j, DEFAULT_MAX_GAS_DEPOSIT);
    }

    @Test
    @Order(0)
    public void testSetMaxGasDeposit_lessThanMinGasDeposit() throws Throwable {
        BigInteger newMaxDeposit = bridge.minGasDeposit().subtract(BigInteger.ONE);
        assertThat(bridge.maxGasDeposit(), is(not(newMaxDeposit)));
        TransactionConfigurationException thrown =
                assertThrows(TransactionConfigurationException.class, () -> setMaxGasDeposit(bridge, neow3j,
                        newMaxDeposit));
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Maximum must be greater than the minimum amount."));
    }

    @Test
    @Order(0)
    public void testSetMaxGasDeposit_abortGovernorNotSigner() throws Throwable {
        BigInteger newMaxDeposit = new BigInteger("20000000000");
        assertThat(bridge.maxGasDeposit(), is(not(newMaxDeposit)));
        TransactionConfigurationException thrown =
                assertThrows(TransactionConfigurationException.class, () ->
                        bridge.invokeFunction("setMaxGasDeposit", integer(newMaxDeposit))
                                .signers(AccountSigner.calledByEntry(alice))
                                .sign());
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Only the governor can call this method."));
    }

    // endregion
    // region contract update

    @Test
    @Order(100)
    public void testUpdateContract() throws Throwable {
        File contractNefFile = Paths.get("src", "test", "resources", "DummyBridge.nef").toFile();
        NefFile nefFile = NefFile.readFromFile(contractNefFile);

        File manifestFile = Paths.get("src", "test", "resources", "DummyBridge.manifest.json").toFile();
        ContractManifest manifest;
        try (FileInputStream s = new FileInputStream(manifestFile)) {
            manifest = ObjectMapperFactory.getObjectMapper().readValue(s, ContractManifest.class);
        }
        byte[] manifestBytes = ObjectMapperFactory.getObjectMapper().writeValueAsBytes(manifest);
        bridge.pauseBridge();
        NeoSendRawTransaction response =
                bridge.invokeFunction("update", byteArray(nefFile.toArray()), byteArray(manifestBytes), any(null))
                        .signers(AccountSigner.calledByEntry(owner))
                        .sign()
                        .send();
        waitUntilTransactionIsExecuted(response.getSendRawTransaction().getHash(), ext.getNeow3j());

        assertThat(bridge.getManifest().getAbi().getMethods(), hasSize(1));
        assertThat(bridge.callFunctionReturningString("sayHello", string("World")), is("Hello World!"));
    }

    @Test
    @Order(0)
    public void testUpdateContract_notOwner() throws Throwable {
        bridge.pauseBridge();
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.invokeFunction("update", byteArray(""), string(""), any(null))
                        .signers(AccountSigner.calledByEntry(alice))
                        .sign());

        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Only the owner can update this contract."));
        bridge.unpause();
    }

    @Test
    @Order(0)
    public void testUpdate_unpaused() throws IOException {
        assertFalse(bridge.isPaused());
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.invokeFunction("update", byteArray(""), string(""), any(null))
                        .signers(AccountSigner.calledByEntry(owner))
                        .sign());
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Contract is not paused."));
    }

    // endregion
    // region contract pausing

    @Test
    @Order(0)
    public void testPauseBridge_onlySecurityGuard() {
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.invokeFunction("pauseBridge").signers(calledByEntry(relayer)).sign());
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Only the security guard can call this method"));
    }

    @Test
    @Order(0)
    public void testUnpause_onlyGovernor() throws Throwable {
        bridge.pauseBridge();
        assertTrue(bridge.isPaused());
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.invokeFunction("unpauseBridge").signers(calledByEntry(relayer)).sign());
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Only the governor can call this" +
                " method."));
        bridge.unpause();
    }

    @Test
    @Order(0)
    public void testPauseBridge() throws Throwable {
        bridge.pauseBridge();
        assertTrue(bridge.isPaused());

        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> gasToken.transfer(relayer, bridge.getScriptHash(), BigInteger.ONE, hash160(recipient0)).sign());
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Contract is paused."));

        thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.depositGas(relayer, recipient0, BigInteger.TEN, DEFAULT_MIN_GAS_DEPOSIT));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Contract is paused."));

        thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.claimGas(relayer, BigInteger.TEN));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Contract is paused."));

        thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.pauseBridge(securityGuard));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Contract is paused."));
        bridge.unpause();
    }

    @Test
    @Order(0)
    public void testUnpause_alreadyUnpaused() throws IOException {
        assertFalse(bridge.isPaused());
        TransactionConfigurationException thrown =
                assertThrows(TransactionConfigurationException.class, () -> bridge.unpause());
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Contract is not paused."));
    }

    @Test
    @Order(0)
    public void testPauseBridge_withdrawal() throws Throwable {
        bridge.pauseBridge();
        HashMap<ContractParameter, ContractParameter> map = new HashMap<>();
        // Map content doesn't matter for this test, just required to have at least one entry.
        map.put(integer(0), integer(0));

        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.withdrawGas("", map, array("")));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Contract is paused."));
        bridge.unpause();
    }

    // endregion

}
