package network.bane;

import io.neow3j.contract.GasToken;
import io.neow3j.contract.NeoToken;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.test.ContractTest;
import io.neow3j.test.ContractTestExtension;
import io.neow3j.test.DeployConfig;
import io.neow3j.test.DeployConfiguration;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;
import network.bane.util.Bridge;
import network.bane.util.Management;
import network.bane.util.TestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.types.ContractParameter.map;
import static io.neow3j.types.ContractParameter.string;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static io.neow3j.utils.Numeric.prependHexPrefix;
import static io.neow3j.utils.Numeric.reverseHexString;
import static io.neow3j.utils.Numeric.toHexString;
import static java.util.Arrays.asList;
import static network.bane.util.TestHelper.concatAndSha256;
import static network.bane.util.TestHelper.createDepositHash;
import static network.bane.util.TestHelper.getClaimEvent;
import static network.bane.util.TestHelper.getClaimableEvent;
import static network.bane.util.TestHelper.getDepositEvent;
import static network.bane.util.TestHelper.getProofFromStorage;
import static network.bane.util.TestHelper.getWithdrawEvent;
import static network.bane.util.TestHelper.governorPubKey;
import static network.bane.util.TestHelper.ownerPubKey;
import static network.bane.util.TestHelper.prepareManagementDeployParameter;
import static network.bane.util.TestHelper.printDepositStorage;
import static network.bane.util.TestHelper.recipient0;
import static network.bane.util.TestHelper.recipient1;
import static network.bane.util.TestHelper.recipient2;
import static network.bane.util.TestHelper.recipient3;
import static network.bane.util.TestHelper.recipient4;
import static network.bane.util.TestHelper.relayer;
import static network.bane.util.TestHelper.relayerPubKey;
import static network.bane.util.TestHelper.setDepositFee;
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ContractTest(
        blockTime = 1,
        contracts = {BridgeManagementContract.class, BridgeContract.class, TestContract.class},
        batchFile = "setup.batch"
)

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BridgeTest {

    private static final int howManyDepositProofsToPrint = 0;

    private static final BigInteger depositFee = new BigInteger("10000000");
    private static final BigInteger minDeposit = new BigInteger("100000000");
    private static final BigInteger maxDeposit = new BigInteger("1000000000000");
    private static final BigInteger maxWithdrawalPerRootUpdate = new BigInteger("10");

    private static final Hash160 managementContractHash = new Hash160("5d5875cab4333e13b645926c3a5a206153aa58a0");

    private static Bridge bridge;
    private static Management management;

    private static Hash160 testContract;

    private static Neow3j neow3j;
    private static GasToken gasToken;
    private static NeoToken neoToken;

    private static Account alice;
    private static Account bob;
    private static Account charlie;
    private static Account denise;
    private static Account eve;
    private static Account florian;
    private static Account gabriel;
    private static Account henry;
    private static Account isabella;

    private static List<Integer> printDeposits = new ArrayList<>();

    @RegisterExtension
    public static final ContractTestExtension ext = new ContractTestExtension();

    // region setup

    @BeforeAll
    public static void setUp() throws Exception {
        neow3j = ext.getNeow3j();

        gasToken = new GasToken(neow3j);
        neoToken = new NeoToken(neow3j);
        management = new Management(ext.getDeployedContract(BridgeManagementContract.class).getScriptHash(), neow3j);
        assert management.getScriptHash().equals(managementContractHash) : "BridgeManagement Contract or its deployer" +
                " has changed. Change the contract hash in this test to " + management.getScriptHash() + ".";
        bridge = new Bridge(ext.getDeployedContract(BridgeContract.class).getScriptHash(), neow3j);
        testContract = ext.getDeployedContract(TestContract.class).getScriptHash();
        alice = ext.getAccount(TestHelper.ALICE);
        bob = ext.getAccount(TestHelper.BOB);
        charlie = ext.getAccount(TestHelper.CHARLIE);
        denise = ext.getAccount(TestHelper.DENISE);
        eve = ext.getAccount(TestHelper.EVE);
        florian = ext.getAccount(TestHelper.FLORIAN);
        gabriel = ext.getAccount(TestHelper.GABRIEL);
        henry = ext.getAccount(TestHelper.HENRY);
        isabella = ext.getAccount(TestHelper.ISABELLA);

        for (int i = 1; i <= howManyDepositProofsToPrint; i++) {
            printDeposits.add(i);
        }
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
                        governorPubKey
                )
        );
        return config;
    }

    @DeployConfig(BridgeContract.class)
    public static DeployConfiguration deployConfigBridge() {
        DeployConfiguration config = new DeployConfiguration();
        config.setDeployParam(
                prepareBridgeDeployParameter(
                        managementContractHash,
                        depositFee,
                        minDeposit,
                        maxDeposit,
                        maxWithdrawalPerRootUpdate
                )
        );
        return config;
    }

    private static ContractParameter prepareBridgeDeployParameter(Hash160 managementContractHash,
            BigInteger depositFee, BigInteger minDeposit, BigInteger maxDeposit,
            BigInteger maxWithdrawalPerRootUpdate) {
        return array(
                hash160(managementContractHash),
                integer(depositFee),
                integer(minDeposit),
                integer(maxDeposit),
                integer(maxWithdrawalPerRootUpdate)
        );
    }

    // endregion
    // region helper

    private Hash256 bridgeGasWithFee(Account from, Hash160 to, BigInteger amount) throws Throwable {
        return bridgeGasWithFee(from, to, amount, false);
    }

    private Hash256 bridgeGasWithFee(Account from, Hash160 to, BigInteger amount, boolean print) throws Throwable {
        List<String> proof = new ArrayList<>();
        if (print) {
            proof = getProofFromStorage(bridge);
        }
        BigInteger depositFee = bridge.depositFee();
        NeoSendRawTransaction response =
                gasToken.transfer(from, bridge.getScriptHash(), amount.add(depositFee), hash160(to))
                        .sign()
                        .send();
        Hash256 txHash = response.getSendRawTransaction().getHash();
        waitUntilTransactionIsExecuted(txHash, neow3j);
        if (print) {
            printDepositStorage(bridge, neow3j, txHash, proof);
        }
        return txHash;
    }

    private Hash256 bridgeGasUsingDepositMethod(Account from, Hash160 to, BigInteger amount, boolean print) throws Throwable {
        List<String> proof = new ArrayList<>();
        if (print) {
            proof = getProofFromStorage(bridge);
        }
        NeoSendRawTransaction response = bridge.deposit(from, to, amount)
                .sign()
                .send();
        Hash256 txHash = response.getSendRawTransaction().getHash();
        waitUntilTransactionIsExecuted(txHash, neow3j);
        if (print) {
            printDepositStorage(bridge, neow3j, txHash, proof);
        }
        return txHash;
    }

    // endregion
    // region deployment

    @Test
    @Order(0)
    public void testDeployment_deploymentDataSetCorrectly() throws IOException {
        assertThat(bridge.findStorage("0x0a"), hasSize(8));
        // Bridge Management Contract Hash
        assertThat(bridge.getStorage("0x0a01"), is(toHexString(management.getScriptHash().toLittleEndianArray())));
        // Deposit Fee
        assertThat(bridge.getStorage("0x0a02"), is(prependHexPrefix(reverseHexString("0x00989680"))));
        // Min Deposit
        assertThat(bridge.getStorage("0x0a03"), is(prependHexPrefix(reverseHexString("0x05f5e100"))));
        // Max Deposit
        assertThat(bridge.getStorage("0x0a04"), is(prependHexPrefix(reverseHexString("0x00e8d4a51000"))));
        // Max Proofs Per Withdrawal
//        assertThat(bridge.getStorage("0x0a05"), is(prependHexPrefix(reverseHexString("0x0a"))));

        assertThat(bridge.management(), is(managementContractHash));
        assertThat(bridge.depositFee(), is(depositFee));
        assertThat(bridge.minDeposit(), is(minDeposit));
        assertThat(bridge.maxDeposit(), is(maxDeposit));

        // Deposit Root
        assertThat(bridge.getStorage("0x0a10"), is(
                "0x0000000000000000000000000000000000000000000000000000000000000000"));
        // Deposit Nonce
        assertThat(bridge.getStorage("0x0a11"), is("0x"));
        // Withdrawal Nonce
        assertThat(bridge.getStorage("0x0a21"), is("0x"));

        assertThat(bridge.depositsProcessed(), is(BigInteger.ZERO));
        assertThat(bridge.withdrawalsProcessed(), is(BigInteger.ZERO));
    }

    // endregion
    // region rejected deposits

    @Test
    @Order(0)
    public void testDeposit_abortIfNotGasToken() {
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class, () ->
                neoToken.transfer(alice, bridge.getScriptHash(), BigInteger.ONE).sign().send());
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Only GAS is accepted."));
    }

    @Test
    @Order(0)
    public void testDeposit_assertFailIfInvalidRecipientData() {
        ContractParameter dataParam = integer(42_000);
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class, () ->
                gasToken.transfer(alice, bridge.getScriptHash(), BigInteger.ONE, dataParam).sign().send());
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Invalid recipient data."));
    }

    @Test
    @Order(0)
    public void testDeposit_assertFailIfInvalidDataHash160() {
        TransactionConfigurationException thrown =
                assertThrows(TransactionConfigurationException.class, () ->
                        gasToken.transfer(
                                alice,
                                bridge.getScriptHash(),
                                minDeposit,
                                string(recipient0.toString())
                        ).sign()
                );
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Invalid recipient data."));

        thrown = assertThrows(TransactionConfigurationException.class, () ->
                gasToken.transfer(
                        alice,
                        bridge.getScriptHash(),
                        minDeposit
                ).sign()
        );
        assertThat(thrown.getMessage(), containsString("Invalid type for SIZE: Any"));

        ArrayList<ContractParameter> toArrayWithSize20 = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            toArrayWithSize20.add(hash160(Account.create()));
        }
        assertThat(toArrayWithSize20.size(), is(20));

        thrown = assertThrows(TransactionConfigurationException.class, () ->
                gasToken.transfer(
                        alice,
                        bridge.getScriptHash(),
                        minDeposit,
                        array(toArrayWithSize20)
                ).sign()
        );
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Invalid recipient data."));
    }

    // endregion
    // region deposits

    // Test OnNEP17Payment and deposit functions
    @Test
    @Order(11)
    public void testRootComputation_1() throws Throwable {
        Account from = alice;
        Hash160 to = recipient1;
        BigInteger amount = minDeposit;
        BigInteger nextNonce = new BigInteger("1");

        Hash256 txHash = bridgeGasWithFee(from, to, amount, printDeposits.contains(1));

        String d1 = createDepositHash(nextNonce, to, amount);

        // root after first deposit is the deposit hash itself
        String newRoot = concatAndSha256(Hash256.ZERO.toString(), d1);
        assertThat(bridge.getStorage("0x0a10"), is(newRoot));
        assertThat(bridge.depositRoot(), is(newRoot));

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
        BigInteger amount = minDeposit;
        BigInteger nextNonce = new BigInteger("2");

        String depositRootBefore = bridge.depositRoot();
        Hash256 txHash = bridgeGasUsingDepositMethod(from, to, amount, printDeposits.contains(2));

        String d2 = createDepositHash(nextNonce, to, amount);
        String d12 = concatAndSha256(depositRootBefore, d2);

        assertThat(bridge.getStorage("0x0a10"), is(d12));
        assertThat(bridge.depositRoot(), is(d12));

        TestHelper.DepositEvent depositEvent = getDepositEvent(txHash, neow3j);
        assertThat(depositEvent.nonce, is(nextNonce));
        assertThat(depositEvent.from, is(from.getScriptHash()));
        assertThat(depositEvent.to, is(to));
        assertThat(depositEvent.amount, is(amount));
        assertThat(depositEvent.depositHashHex, is(d2));
        assertThat(depositEvent.rootHashHex, is(d12));
    }

    @Test
    @Order(13)
    public void testRootComputation_3() throws Throwable {
        Account from = charlie;
        Hash160 to = recipient3;
        BigInteger amount = minDeposit.multiply(new BigInteger("3"));
        BigInteger nextNonce = new BigInteger("3");

        String depositRootBefore = bridge.depositRoot();
        Hash256 txHash = bridgeGasUsingDepositMethod(from, to, amount, printDeposits.contains(3));

        String d3 = createDepositHash(nextNonce, to, amount);
        String d12d3 = concatAndSha256(depositRootBefore, d3);

        assertThat(bridge.getStorage("0x0a10"), is(d12d3));
        assertThat(bridge.depositRoot(), is(d12d3));

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
        BigInteger amount = minDeposit.multiply(new BigInteger("4"));
        BigInteger nextNonce = new BigInteger("4");

        String depositRootBefore = bridge.depositRoot();

        Hash256 txHash = bridgeGasWithFee(from, to, amount, printDeposits.contains(4));

        String depositHashOffChain = createDepositHash(nextNonce, to, amount);
        String d1234 = concatAndSha256(depositRootBefore, depositHashOffChain);

        assertThat(bridge.getStorage("0x0a10"), is(d1234));
        assertThat(bridge.depositRoot(), is(d1234));

        TestHelper.DepositEvent depositEvent = getDepositEvent(txHash, neow3j);
        assertThat(depositEvent.nonce, is(nextNonce));
        assertThat(depositEvent.from, is(from.getScriptHash()));
        assertThat(depositEvent.to, is(to));
        assertThat(depositEvent.amount, is(amount));
        assertThat(depositEvent.depositHashHex, is(depositHashOffChain));
        assertThat(depositEvent.rootHashHex, is(d1234));
    }

    // endregion
    // region withdrawal and claim

    // Test withdraw function with first withdrawal
    @Test
    @Order(20)
    public void testWithdrawal_1() throws Throwable {
        Hash160 to = alice.getScriptHash();
        BigInteger amount1 = new BigInteger("1000000000");
        bridgeGasWithFee(bob, to, amount1, false);
        BigInteger amount = minDeposit;
        BigInteger nonce = new BigInteger("1");

        String d1 = createDepositHash(nonce, to, amount);
        String root = concatAndSha256(Hash256.ZERO.toString(), d1);
        List<Account> validators = Arrays.asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter withdrawal = array(array(integer(nonce), integer(amount), hash160(to)));
        Hash256 txHash = withdrawGas(root, signMsg(validators, root), withdrawal);
        TestHelper.WithdrawEvent withdrawEvent = getWithdrawEvent(txHash, neow3j);
        assertThat(withdrawEvent.nonce, is(nonce));
        assertThat(withdrawEvent.to, is(to));
        assertThat(withdrawEvent.amount, is(amount));
    }

    // Test withdraw function with second withdrawal
    @Test
    @Order(21)
    public void testWithdrawal_2() throws Throwable {
        Hash160 to1 = alice.getScriptHash();
        BigInteger amount1 = minDeposit;
        BigInteger nonce1 = new BigInteger("2");
        Hash160 to2 = charlie.getScriptHash();
        BigInteger amount2 = minDeposit;
        BigInteger nonce2 = new BigInteger("3");

        String withdrawRootBefore = bridge.withdrawRoot();

        String d1 = createDepositHash(nonce1, to1, amount1);
        String root1 = concatAndSha256(withdrawRootBefore, d1);
        String d2 = createDepositHash(nonce2, to2, amount2);
        String newRoot = concatAndSha256(root1, d2);

        List<Account> validators = Arrays.asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter withdrawal =
                array(array(integer(nonce1), integer(amount1), hash160(to1)), array(integer(nonce2), integer(amount2)
                        , hash160(to2)));
        Hash256 txHash = withdrawGas(newRoot, signMsg(validators, newRoot), withdrawal);
        TestHelper.WithdrawEvent withdrawEvent = getWithdrawEvent(txHash, neow3j);
        assertThat(withdrawEvent.nonce, is(nonce1));
        assertThat(withdrawEvent.to, is(to1));
        assertThat(withdrawEvent.amount, is(amount1));
    }

    // Test withdraw function with contract as recipient
    @Test
    @Order(22)
    public void testWithdrawalToContract() throws Throwable {
        Hash160 to = testContract;
        BigInteger amount = minDeposit;
        BigInteger nonce = new BigInteger("4");

        String withdrawRootBefore = bridge.withdrawRoot();
        String d1 = createDepositHash(nonce, to, amount);
        String root = concatAndSha256(withdrawRootBefore, d1);
        List<Account> validators = Arrays.asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter withdrawal = array(array(integer(nonce), integer(amount), hash160(to)));

        Hash256 txHash = withdrawGas(root, signMsg(validators, root), withdrawal);

        TestHelper.ClaimableEvent claimableEvent = getClaimableEvent(txHash, neow3j);
        assertThat(claimableEvent.nonce, is(nonce));
        assertThat(claimableEvent.to, is(to));
        assertThat(claimableEvent.amount, is(amount));
    }

    @Test
    @Order(23)
    public void testClaim() throws Throwable {
        Hash160 to = testContract;
        BigInteger amount = minDeposit;
        BigInteger nonce = new BigInteger("4");

        Hash256 txHash = claimGas(nonce.intValue());
        TestHelper.ClaimEvent claimEvent = getClaimEvent(txHash, neow3j);
        assertThat(claimEvent.nonce, is(nonce));
        assertThat(claimEvent.to, is(to));
        assertThat(claimEvent.amount, is(amount));
    }

    // endregion
    // region deposit fee

    @Test
    @Order(0)
    public void testSetDepositFee() throws Throwable {
        BigInteger newFee = new BigInteger("200000000");
        assertThat(bridge.depositFee(), is(not(newFee)));

        setDepositFee(bridge, neow3j, newFee);
        BigInteger actualDepositFee = bridge.depositFee();
        assertThat(actualDepositFee, is(newFee));
        setDepositFee(bridge, neow3j, depositFee);
    }

    @Test
    @Order(0)
    public void testSetDepositFee_FeeisZero() throws Throwable {
        BigInteger newFee = new BigInteger("0");
        assertThat(bridge.depositFee(), is(not(newFee)));

        setDepositFee(bridge, neow3j, newFee);
        BigInteger actualDepositFee = bridge.depositFee();
        assertThat(actualDepositFee, is(newFee));
        setDepositFee(bridge, neow3j, depositFee);
    }

    @Test
    @Order(0)
    public void testSetDepositFee_assertFailIfFeeLowerThanZero() {
        BigInteger newFee = new BigInteger("-1");
        TransactionConfigurationException thrown =
                assertThrows(TransactionConfigurationException.class, () ->
                        setDepositFee(bridge, neow3j, newFee)
                );
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Deposit fee must be nonnegative."));
    }

    @Test
    @Order(0)
    public void testSetDepositFee_assertFailIfCallerisNotOwner() {
        BigInteger newFee = new BigInteger("200000000");
        TransactionConfigurationException thrown =
                assertThrows(TransactionConfigurationException.class, () ->
                        bridge.invokeFunction("setDepositFee", integer(newFee))
                                .signers(AccountSigner.calledByEntry(alice))
                                .sign()
                                .send()
                );
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Only owner can set deposit fee."));
    }

    private Hash256 withdrawGas(String withdrawalRoot, Map<ContractParameter, ContractParameter> signatures,
            ContractParameter withdrawals) throws Throwable {
        NeoSendRawTransaction response =
                bridge.invokeFunction("withdraw", byteArray(withdrawalRoot), map(signatures), withdrawals)
                        .signers(AccountSigner.calledByEntry(relayer))
                        .sign()
                        .send();
        Hash256 txHash = response.getSendRawTransaction().getHash();
        waitUntilTransactionIsExecuted(txHash, neow3j);
        return txHash;
    }

    private Hash256 claimGas(int nonce) throws Throwable {
        NeoSendRawTransaction response = bridge.invokeFunction("claim", integer(nonce))
                .signers(AccountSigner.calledByEntry(alice))
                .sign()
                .send();
        Hash256 txHash = response.getSendRawTransaction().getHash();
        waitUntilTransactionIsExecuted(txHash, neow3j);
        return txHash;
    }

    // endregion

}
