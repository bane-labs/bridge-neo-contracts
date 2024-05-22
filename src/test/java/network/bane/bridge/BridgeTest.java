package network.bane.bridge;

import io.neow3j.contract.ContractManagement;
import io.neow3j.contract.GasToken;
import io.neow3j.contract.NefFile;
import io.neow3j.contract.NeoToken;
import io.neow3j.contract.PolicyContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.ObjectMapperFactory;
import io.neow3j.protocol.core.response.ContractManifest;
import io.neow3j.protocol.core.response.ContractStorageEntry;
import io.neow3j.protocol.core.response.NeoApplicationLog;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.protocol.core.stackitem.ArrayStackItem;
import io.neow3j.protocol.core.stackitem.IntegerStackItem;
import io.neow3j.test.ContractTest;
import io.neow3j.test.ContractTestExtension;
import io.neow3j.test.DeployConfig;
import io.neow3j.test.DeployConfiguration;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.transaction.witnessrule.CalledByContractCondition;
import io.neow3j.transaction.witnessrule.WitnessAction;
import io.neow3j.transaction.witnessrule.WitnessRule;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.types.NeoVMStateType;
import io.neow3j.utils.Await;
import io.neow3j.utils.Numeric;
import io.neow3j.wallet.Account;
import network.bane.management.BridgeManagementContract;
import network.bane.testhelper.TestContract;
import network.bane.util.Bridge;
import network.bane.util.structs.GasBridge;
import network.bane.util.Management;
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
import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.types.ContractParameter.string;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static java.util.Arrays.asList;
import static network.bane.util.helper.NetworkSettingsHelper.updateNetworkSettings;
import static network.bane.util.helper.PrintHelper.printTransactionFee;
import static network.bane.util.TestHelper.concatAndSha256;
import static network.bane.util.TestHelper.createDepositHash;
import static network.bane.util.TestHelper.getClaimEvent;
import static network.bane.util.TestHelper.getClaimableEvent;
import static network.bane.util.TestHelper.getDepositEvent;
import static network.bane.util.TestHelper.getWithdrawEvent;
import static network.bane.util.TestHelper.governorPubKey;
import static network.bane.util.TestHelper.hasFiredEvent;
import static network.bane.util.TestHelper.owner;
import static network.bane.util.TestHelper.ownerPubKey;
import static network.bane.util.TestHelper.prepareManagementDeployParameter;
import static network.bane.util.TestHelper.recipient0;
import static network.bane.util.TestHelper.recipient1;
import static network.bane.util.TestHelper.recipient2;
import static network.bane.util.TestHelper.recipient3;
import static network.bane.util.TestHelper.recipient4;
import static network.bane.util.TestHelper.relayer;
import static network.bane.util.TestHelper.relayerPubKey;
import static network.bane.util.TestHelper.securityGuard;
import static network.bane.util.TestHelper.securityGuardPubKey;
import static network.bane.util.TestHelper.setDepositFee;
import static network.bane.util.TestHelper.setMaxGasDeposit;
import static network.bane.util.TestHelper.setMinDeposit;
import static network.bane.util.TestHelper.signMsg;
import static network.bane.util.TestHelper.validator1;
import static network.bane.util.TestHelper.validator1PubKey;
import static network.bane.util.TestHelper.validator2;
import static network.bane.util.TestHelper.validator2PubKey;
import static network.bane.util.TestHelper.validator3;
import static network.bane.util.TestHelper.validator3PubKey;
import static network.bane.util.TestHelper.validator4;
import static network.bane.util.TestHelper.validator4PubKey;
import static network.bane.util.TestHelper.validator5;
import static network.bane.util.TestHelper.validator5PubKey;
import static network.bane.util.TestHelper.validator6PubKey;
import static network.bane.util.TestHelper.validator7PubKey;
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
    private static final BigInteger gasDepositFee = new BigInteger("10000000");
    private static final BigInteger minGasDeposit = new BigInteger("100000000");
    private static final BigInteger maxGasDeposit = new BigInteger("1000000000000");
    private static final BigInteger maxWithdrawals = new BigInteger("100");

    private static final Hash160 managementContractHash = new Hash160("8863a5617efaa07f13f92308165b197633cd53b6");

    private static Bridge bridge;
    private static Management management;

    private static Hash160 testContract;

    private static Neow3j neow3j;
    private static GasToken gasToken;
    private static NeoToken neoToken;
    public static PolicyContract policyContract;

    private static BigInteger withdrawalNonce = BigInteger.ZERO;
    private static BigInteger depositNonce = BigInteger.ZERO;

    public static Account alice;
    private static Account bob;
    private static Account charlie;
    private static Account denise;
    private static Account eve;
    private static Account florian;
    private static Account gabriel;
    private static Account henry;
    private static Account isabella;

    public static Account committee;

    @RegisterExtension
    public static final ContractTestExtension ext = new ContractTestExtension();

    // region setup

    @BeforeAll
    public static void setUp() throws Throwable {
        neow3j = ext.getNeow3j();

        gasToken = new GasToken(neow3j);
        neoToken = new NeoToken(neow3j);
        policyContract = new PolicyContract(neow3j);
        management = new Management(ext.getDeployedContract(BridgeManagementContract.class).getScriptHash(), neow3j);
        assert management.getScriptHash().equals(managementContractHash) : "BridgeManagement Contract or its deployer" +
                " has changed. Change the contract hash in this test to " + management.getScriptHash() + ".";
        bridge = new Bridge(ext.getDeployedContract(BridgeContract.class).getScriptHash(), neow3j);
        testContract = ext.getDeployedContract(TestContract.class).getScriptHash();
        alice = ext.getAccount(TestHelper.ALICE);
        committee = Account.createMultiSigAccount(asList(alice.getECKeyPair().getPublicKey()), 1);
        bob = ext.getAccount(TestHelper.BOB);
        charlie = ext.getAccount(TestHelper.CHARLIE);
        denise = ext.getAccount(TestHelper.DENISE);
        eve = ext.getAccount(TestHelper.EVE);
        florian = ext.getAccount(TestHelper.FLORIAN);
        gabriel = ext.getAccount(TestHelper.GABRIEL);
        henry = ext.getAccount(TestHelper.HENRY);
        isabella = ext.getAccount(TestHelper.ISABELLA);

        updateNetworkSettings(neow3j);
    }

    // endregion
    // region deploy config

    @DeployConfig(BridgeManagementContract.class)
    public static DeployConfiguration deployConfigManagement() {
        DeployConfiguration config = new DeployConfiguration();
        config.setDeployParam(
                prepareManagementDeployParameter(
                        ownerPubKey,
                        relayerPubKey,
                        asList(
                                validator1PubKey,
                                validator2PubKey,
                                validator3PubKey,
                                validator4PubKey,
                                validator5PubKey,
                                validator6PubKey,
                                validator7PubKey
                        ),
                        5,
                        governorPubKey,
                        securityGuardPubKey
                )
        );
        AccountSigner deploySigner = AccountSigner.none(owner);
        WitnessRule deployWitnessRule = new WitnessRule(WitnessAction.ALLOW,
                new CalledByContractCondition(ContractManagement.SCRIPT_HASH));
        deploySigner.setRules(deployWitnessRule);
        config.setSigner(deploySigner);
        return config;
    }

    @DeployConfig(BridgeContract.class)
    public static DeployConfiguration deployConfigBridge() {
        DeployConfiguration config = new DeployConfiguration();
        config.setDeployParam(
                prepareBridgeDeployParameter(
                        managementContractHash,
                        gasDepositFee,
                        minGasDeposit,
                        maxGasDeposit,
                        maxWithdrawals
                )
        );
        AccountSigner deploySigner = AccountSigner.none(owner);
        WitnessRule deployWitnessRule = new WitnessRule(WitnessAction.ALLOW,
                new CalledByContractCondition(ContractManagement.SCRIPT_HASH));
        deploySigner.setRules(deployWitnessRule);
        config.setSigner(deploySigner);
        return config;
    }

    private static ContractParameter prepareBridgeDeployParameter(Hash160 managementContractHash,
            BigInteger depositFee, BigInteger minDeposit, BigInteger maxDeposit, BigInteger maxWithdrawals) {
        return array(
                hash160(managementContractHash),
                array(
                        integer(depositFee),
                        integer(minDeposit),
                        integer(maxDeposit),
                        integer(maxWithdrawals)
                )
        );
    }

    // endregion
    // region helper

    private Hash256 depositGasUsingDepositMethod(Account from, Hash160 to, BigInteger amount) throws Throwable {
        Hash256 txHash = bridge.depositGas(from, to, amount);
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
        assertArrayEquals(managementEntry.getValue(), managementContractHash.toLittleEndianArray());
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

        assertThat(bridge.management(), is(managementContractHash));

        GasBridge expectedGasBridge = new GasBridge(false, State.newState(), State.newState(),
                new GasBridge.GasConfig(gasDepositFee, minGasDeposit, maxGasDeposit, maxWithdrawals));
        assertTrue(bridge.getGasBridge().equals(expectedGasBridge));
        assertThat(bridge.gasDepositFee(), is(gasDepositFee));
        assertThat(bridge.minGasDeposit(), is(minGasDeposit));
        assertThat(bridge.maxGasDeposit(), is(maxGasDeposit));
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
                                minGasDeposit,
                                string(recipient0.toString())
                        ).sign()
                );
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Invalid payment data."));

        NeoSendRawTransaction response = gasToken.transfer(alice, bridge.getScriptHash(), minGasDeposit)
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
                        minGasDeposit.add(bridge.gasDepositFee()),
                        array(array(newArr), integer(minGasDeposit))
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
                        minGasDeposit.add(bridge.gasDepositFee()),
                        array(array(newArr), integer(minGasDeposit))
                ).sign()
        );
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Invalid payment data."));

        thrown = assertThrows(TransactionConfigurationException.class, () ->
                gasToken.transfer(
                        alice,
                        bridge.getScriptHash(),
                        minGasDeposit.add(bridge.gasDepositFee()),
                        array(hash160(Hash160.ZERO), integer(minGasDeposit))
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
        BigInteger amount = new BigInteger("100000000");

        String depositRootBefore = bridge.gasDepositRoot();
        Hash256 txHash = depositGasUsingDepositMethod(from, to, amount);

        String d1 = createDepositHash(nextNonce, to, amount);
        // Raw deposit hash and root. The same inputs and the same deposit and root hash are used in a Neo X test.
        assertThat(d1, is("0x20aad97e2860b1934184ffb2b04ea45d49af5145fa43cb212027f9e8e728baea"));
        String newRoot = concatAndSha256(depositRootBefore, d1);
        assertThat(newRoot, is("0xdda77cac690580c1e5220d377cd807b4d2ed89751e4078525ee54297182d3d88"));
        assertThat(bridge.gasDepositRoot(), is(newRoot));

        TestHelper.DepositEvent depositEvent = getDepositEvent(txHash, neow3j);
        assertThat(depositEvent.nonce, is(nextNonce));
        assertThat(depositEvent.from, is(from.getScriptHash()));
        assertThat(depositEvent.to, is(to));
        assertThat(depositEvent.amount, is(amount));
        assertThat(depositEvent.depositHashHex, is(d1));
        assertThat(depositEvent.rootHashHex, is(newRoot));
    }

    @Test
    @Order(12)
    public void testRootComputation_2() throws Throwable {
        Account from = bob;
        Hash160 to = recipient2;
        BigInteger amount = minGasDeposit.add(bridge.gasDepositFee());
        BigInteger nextNonce = incrementAndGetDepositNonce();

        String depositRootBefore = bridge.gasDepositRoot();
        Hash256 txHash = depositGasWithDirectTransfer(from, to, amount, minGasDeposit);

        // If GAS is directly sent to the bridge providing a Hash160 value as data, the transfer initiates a bridge
        // operation. In this case, the deposit fee is deducted from the sent amount and the remaining amount is
        // used for the deposit to Neo X.
        BigInteger bridgedAmount = amount.subtract(bridge.gasDepositFee());
        String d2 = createDepositHash(nextNonce, to, bridgedAmount);
        String d12 = concatAndSha256(depositRootBefore, d2);

        assertThat(bridge.gasDepositRoot(), is(d12));

        TestHelper.DepositEvent depositEvent = getDepositEvent(txHash, neow3j);
        assertThat(depositEvent.nonce, is(nextNonce));
        assertThat(depositEvent.from, is(from.getScriptHash()));
        assertThat(depositEvent.to, is(to));
        assertThat(depositEvent.amount, is(bridgedAmount));
        assertThat(depositEvent.depositHashHex, is(d2));
        assertThat(depositEvent.rootHashHex, is(d12));
    }

    @Test
    @Order(13)
    public void testRootComputation_3() throws Throwable {
        Account from = charlie;
        Hash160 to = recipient3;
        BigInteger amount = minGasDeposit.multiply(new BigInteger("3"));
        BigInteger nextNonce = incrementAndGetDepositNonce();

        String depositRootBefore = bridge.gasDepositRoot();
        Hash256 txHash = depositGasUsingDepositMethod(from, to, amount);

        String d3 = createDepositHash(nextNonce, to, amount);
        String d12d3 = concatAndSha256(depositRootBefore, d3);

        assertThat(bridge.gasDepositRoot(), is(d12d3));

        TestHelper.DepositEvent depositEvent = getDepositEvent(txHash, neow3j);
        assertThat(depositEvent.nonce, is(nextNonce));
        assertThat(depositEvent.from, is(from.getScriptHash()));
        assertThat(depositEvent.to, is(to));
        assertThat(depositEvent.amount, is(amount));
        assertThat(depositEvent.depositHashHex, is(d3));
        assertThat(depositEvent.rootHashHex, is(d12d3));
    }

    @Test
    @Order(14)
    public void testRootComputation_4() throws Throwable {
        Account from = denise;
        Hash160 to = recipient4;
        BigInteger amount = minGasDeposit.multiply(new BigInteger("4"));
        BigInteger nextNonce = incrementAndGetDepositNonce();

        String depositRootBefore = bridge.gasDepositRoot();

        Hash256 txHash = depositGasUsingDepositMethod(from, to, amount);

        String depositHashOffChain = createDepositHash(nextNonce, to, amount);
        String d1234 = concatAndSha256(depositRootBefore, depositHashOffChain);

        assertThat(bridge.gasDepositRoot(), is(d1234));

        TestHelper.DepositEvent depositEvent = getDepositEvent(txHash, neow3j);
        assertThat(depositEvent.nonce, is(nextNonce));
        assertThat(depositEvent.from, is(from.getScriptHash()));
        assertThat(depositEvent.to, is(to));
        assertThat(depositEvent.amount, is(amount));
        assertThat(depositEvent.depositHashHex, is(depositHashOffChain));
        assertThat(depositEvent.rootHashHex, is(d1234));
    }

    @Test
    @Order(15)
    public void testTryingToDepositWithBridgeContractAsFrom() throws Throwable {
        BigInteger amount = minGasDeposit.multiply(new BigInteger("3"));
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
        BigInteger amount = minGasDeposit.add(bridge.gasDepositFee()).subtract(BigInteger.ONE);
        BigInteger nextNonce = incrementAndGetDepositNonce();

        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> depositGasWithDirectTransfer(from, to, amount, minGasDeposit));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Amount below defined minimum."));
    }

    // endregion
    // region withdrawal and claim

    @Test
    @Order(20)
    public void testWithdrawal_1() throws Throwable {
        Hash160 to = alice.getScriptHash();
        BigInteger amount = minGasDeposit;
        BigInteger nonce = incrementAndGetWithdrawalNonce();

        String d1 = createDepositHash(nonce, to, amount);
        String root = concatAndSha256(Hash256.ZERO.toString(), d1);
        List<Account> validators = Arrays.asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter withdrawal = array(array(integer(nonce), integer(amount), hash160(to)));
        Hash256 txHash = bridge.withdrawGas(root, signMsg(validators, root), withdrawal);
        printTransactionFee(neow3j, "tx with 1 withdrawals", txHash);
        TestHelper.WithdrawEvent withdrawEvent = getWithdrawEvent(txHash, neow3j);
        assertThat(withdrawEvent.nonce, is(nonce));
        assertThat(withdrawEvent.to, is(to));
        assertThat(withdrawEvent.amount, is(amount));
    }

    @Test
    @Order(21)
    public void testWithdrawal_2() throws Throwable {
        Hash160 to1 = alice.getScriptHash();
        BigInteger amount1 = minGasDeposit;
        BigInteger nonce1 = incrementAndGetWithdrawalNonce();
        Hash160 to2 = charlie.getScriptHash();
        BigInteger amount2 = minGasDeposit;
        BigInteger nonce2 = incrementAndGetWithdrawalNonce();

        String withdrawRootBefore = bridge.gasWithdrawRoot();

        String d1 = createDepositHash(nonce1, to1, amount1);
        String root1 = concatAndSha256(withdrawRootBefore, d1);
        String d2 = createDepositHash(nonce2, to2, amount2);
        String newRoot = concatAndSha256(root1, d2);

        List<Account> validators = Arrays.asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter withdrawal =
                array(array(integer(nonce1), integer(amount1), hash160(to1)), array(integer(nonce2), integer(amount2),
                        hash160(to2)));
        Hash256 txHash = bridge.withdrawGas(newRoot, signMsg(validators, newRoot), withdrawal);
        printTransactionFee(neow3j, "tx with 2 withdrawals", txHash);
        TestHelper.WithdrawEvent withdrawEvent = getWithdrawEvent(txHash, neow3j);
        assertThat(withdrawEvent.nonce, is(nonce1));
        assertThat(withdrawEvent.to, is(to1));
        assertThat(withdrawEvent.amount, is(amount1));
    }

    @Test
    @Order(22)
    public void testWithdrawalToContract() throws Throwable {
        Hash160 to = testContract;
        BigInteger amount = minGasDeposit;
        BigInteger nonce = incrementAndGetWithdrawalNonce();

        String withdrawRootBefore = bridge.gasWithdrawRoot();
        String d1 = createDepositHash(nonce, to, amount);
        String root = concatAndSha256(withdrawRootBefore, d1);
        List<Account> validators = Arrays.asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter withdrawal = array(array(integer(nonce), integer(amount), hash160(to)));

        Hash256 txHash = bridge.withdrawGas(root, signMsg(validators, root), withdrawal);
        printTransactionFee(neow3j, "tx with 1 withdrawals to claim", txHash);

        TestHelper.ClaimableEvent claimableEvent = getClaimableEvent(txHash, neow3j);
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
        assertThat(gasToken.getBalanceOf(bridge.getScriptHash()), greaterThanOrEqualTo(new BigInteger("700000000")));
        BigInteger amount1 = minGasDeposit.multiply(new BigInteger("2"));
        BigInteger amount2 = minGasDeposit.multiply(new BigInteger("12"));
        BigInteger amount3 = minGasDeposit.multiply(new BigInteger("5"));
        BigInteger amount4 = minGasDeposit.multiply(new BigInteger("9"));
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
        assertThat(appLog.getFirstExecution().getNotifications(), hasSize(6));
        assertThat(appLog.getFirstExecution().getNotification(0).getEventName(), is("Transfer"));
        assertThat(appLog.getFirstExecution().getNotification(1).getEventName(), is("GasWithdrawal"));
        assertThat(appLog.getFirstExecution().getNotification(2).getEventName(), is("GasClaimable"));
        assertThat(appLog.getFirstExecution().getNotification(3).getEventName(), is("Transfer"));
        assertThat(appLog.getFirstExecution().getNotification(4).getEventName(), is("GasWithdrawal"));
        assertThat(appLog.getFirstExecution().getNotification(5).getEventName(), is("GasClaimable"));
    }

    @Test
    @Order(23)
    public void testClaim() throws Throwable {
        Hash160 to = testContract;
        BigInteger amount = minGasDeposit;
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
        TestHelper.ClaimEvent claimEvent = getClaimEvent(txHash, neow3j);
        assertThat(claimEvent.nonce, is(nextNonce));
        assertThat(claimEvent.to, is(to));
        assertThat(claimEvent.amount, is(amount));
    }

    // endregion
    // region setters

    @Test
    @Order(0)
    public void testSetDepositFee() throws Throwable {
        BigInteger newFee = new BigInteger("200000000");
        assertThat(bridge.gasDepositFee(), is(not(newFee)));
        Hash256 txHash = setDepositFee(bridge, neow3j, newFee);
        BigInteger actualDepositFee = bridge.gasDepositFee();
        assertThat(actualDepositFee, is(newFee));
        assertTrue(hasFiredEvent(neow3j, txHash, bridge.getScriptHash(), "GasDepositFeeChange",
                new ArrayStackItem(asList(new IntegerStackItem(newFee)))));
        setDepositFee(bridge, neow3j, gasDepositFee);
    }

    @Test
    @Order(0)
    public void testSetDepositFee_FeeisZero() throws Throwable {
        BigInteger newFee = new BigInteger("0");
        assertThat(bridge.gasDepositFee(), is(not(newFee)));
        setDepositFee(bridge, neow3j, newFee);
        BigInteger actualDepositFee = bridge.gasDepositFee();
        assertThat(actualDepositFee, is(newFee));
        setDepositFee(bridge, neow3j, gasDepositFee);
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
        setMinDeposit(bridge, neow3j, minGasDeposit);
    }

    @Test
    @Order(0)
    public void testSetMinDeposit_isZero() throws Throwable {
        BigInteger newMinDeposit = new BigInteger("0");
        assertThat(bridge.minGasDeposit(), is(not(newMinDeposit)));
        setMinDeposit(bridge, neow3j, newMinDeposit);
        BigInteger actualMinDeposit = bridge.minGasDeposit();
        assertThat(actualMinDeposit, is(newMinDeposit));
        setMinDeposit(bridge, neow3j, minGasDeposit);
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
        setMaxGasDeposit(bridge, neow3j, maxGasDeposit);
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
    // region private helpers

    private static BigInteger incrementAndGetWithdrawalNonce() {
        withdrawalNonce = withdrawalNonce.add(BigInteger.ONE);
        return withdrawalNonce;
    }

    private static BigInteger incrementAndGetDepositNonce() {
        depositNonce = depositNonce.add(BigInteger.ONE);
        return depositNonce;
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
        bridge.pause();
        NeoSendRawTransaction response =
                bridge.invokeFunction("update", byteArray(nefFile.toArray()), byteArray(manifestBytes))
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
        bridge.pause();
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.invokeFunction("update", byteArray(""), string(""))
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
                () -> bridge.invokeFunction("update", byteArray(""), string(""))
                        .signers(AccountSigner.calledByEntry(owner))
                        .sign());
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Contract is not paused."));
    }

    // endregion
    // region contract pausing

    @Test
    @Order(0)
    public void testPause_onlySecurityGuard() {
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.invokeFunction("pause").signers(calledByEntry(relayer)).sign());
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Only the security guard can call this method"));
    }

    @Test
    @Order(0)
    public void testUnpause_onlyGovernor() throws Throwable {
        bridge.pause();
        assertTrue(bridge.isPaused());
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.invokeFunction("unpause").signers(calledByEntry(relayer)).sign());
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Only the governor can call this" +
                " method."));
        bridge.unpause();
    }

    @Test
    @Order(0)
    public void testPause() throws Throwable {
        bridge.pause();
        assertTrue(bridge.isPaused());

        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> gasToken.transfer(relayer, bridge.getScriptHash(), BigInteger.ONE, hash160(recipient0)).sign());
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Contract is paused."));

        thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.depositGas(relayer, recipient0, BigInteger.TEN));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Contract is paused."));

        thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.claimGas(relayer, BigInteger.TEN));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Contract is paused."));

        thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.pause(securityGuard));
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
    public void testPause_withdrawal() throws Throwable {
        bridge.pause();
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
