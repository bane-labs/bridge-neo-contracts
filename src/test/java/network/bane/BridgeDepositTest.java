package network.bane;

import io.neow3j.contract.GasToken;
import io.neow3j.contract.NeoToken;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.protocol.exceptions.RpcResponseErrorException;
import io.neow3j.test.ContractTest;
import io.neow3j.test.ContractTestExtension;
import io.neow3j.test.DeployConfig;
import io.neow3j.test.DeployConfiguration;
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
import java.util.List;

import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.types.ContractParameter.string;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static io.neow3j.utils.Numeric.prependHexPrefix;
import static io.neow3j.utils.Numeric.reverseHexString;
import static io.neow3j.utils.Numeric.toHexString;
import static java.util.Arrays.asList;
import static network.bane.util.TestHelper.buildSubTree;
import static network.bane.util.TestHelper.concatAndSha256;
import static network.bane.util.TestHelper.concatLeftRight;
import static network.bane.util.TestHelper.createDepositHash;
import static network.bane.util.TestHelper.getDepositEvent;
import static network.bane.util.TestHelper.getProofFromStorage;
import static network.bane.util.TestHelper.ownerPubKey;
import static network.bane.util.TestHelper.prepareManagementDeployParameter;
import static network.bane.util.TestHelper.printDepositStorage;
import static network.bane.util.TestHelper.recipient0;
import static network.bane.util.TestHelper.recipient1;
import static network.bane.util.TestHelper.recipient2;
import static network.bane.util.TestHelper.recipient3;
import static network.bane.util.TestHelper.recipient4;
import static network.bane.util.TestHelper.recipient5;
import static network.bane.util.TestHelper.recipient6;
import static network.bane.util.TestHelper.recipient7;
import static network.bane.util.TestHelper.recipient8;
import static network.bane.util.TestHelper.recipient9;
import static network.bane.util.TestHelper.relayerPubKey;
import static network.bane.util.TestHelper.sha256Hex;
import static network.bane.util.TestHelper.validator1PubKey;
import static network.bane.util.TestHelper.validator2PubKey;
import static network.bane.util.TestHelper.validator3PubKey;
import static network.bane.util.TestHelper.validator4PubKey;
import static network.bane.util.TestHelper.validator5PubKey;
import static network.bane.util.TestHelper.validator6PubKey;
import static network.bane.util.TestHelper.validator7PubKey;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ContractTest(
        blockTime = 1,
        contracts = {BridgeManagementContract.class, BridgeContract.class, TestContract.class},
        batchFile = "setup.batch"
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BridgeDepositTest {

    private static final int howManyDepositProofsToPrint = 0;

    private static final BigInteger depositPrice = new BigInteger("10000000");
    private static final BigInteger minDeposit = new BigInteger("100000000");
    private static final BigInteger maxDeposit = new BigInteger("1000000000000");
    private static final BigInteger maxProofsPerWithdrawal = new BigInteger("10");

    private static final Hash160 managementContractHash = new Hash160("5d5875cab4333e13b645926c3a5a206153aa58a0");

    private static Bridge bridge;
    private static Management management;
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
                        5
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
                        depositPrice,
                        minDeposit,
                        maxDeposit,
                        maxProofsPerWithdrawal
                )
        );
        return config;
    }

    private static ContractParameter prepareBridgeDeployParameter(Hash160 managementContractHash,
            BigInteger depositPrice, BigInteger minDeposit, BigInteger maxDeposit, BigInteger maxProofsPerWithdrawal) {
        return array(
                hash160(managementContractHash),
                integer(depositPrice),
                integer(minDeposit),
                integer(maxDeposit),
                integer(maxProofsPerWithdrawal)
        );
    }

    // endregion
    // region helper

    private Hash256 bridgeGas(Account from, Hash160 to, BigInteger amount) throws Throwable {
        return bridgeGas(from, to, amount, false);
    }

    private Hash256 bridgeGas(Account from, Hash160 to, BigInteger amount, boolean print) throws Throwable {
        List<String> proof = new ArrayList<>();
        if (print) {
            proof = getProofFromStorage(bridge);
        }
        NeoSendRawTransaction response = gasToken.transfer(from, bridge.getScriptHash(), amount, hash160(to))
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
        // Deposit Price
        assertThat(bridge.getStorage("0x0a02"), is(prependHexPrefix(reverseHexString("0x00989680"))));
        // Min Deposit
        assertThat(bridge.getStorage("0x0a03"), is(prependHexPrefix(reverseHexString("0x05f5e100"))));
        // Max Deposit
        assertThat(bridge.getStorage("0x0a04"), is(prependHexPrefix(reverseHexString("0x00e8d4a51000"))));
        // Max Proofs Per Withdrawal
        assertThat(bridge.getStorage("0x0a05"), is(prependHexPrefix(reverseHexString("0x0a"))));

        assertThat(bridge.management(), is(managementContractHash));
        assertThat(bridge.depositPrice(), is(depositPrice));
        assertThat(bridge.minDeposit(), is(minDeposit));
        assertThat(bridge.maxDeposit(), is(maxDeposit));
        assertThat(bridge.maxProofsPerWithdrawal(), is(maxProofsPerWithdrawal));

        assertThrows(RpcResponseErrorException.class, () -> bridge.getStorage("0x0a10"));
        // Deposit Root
        assertThat(bridge.getStorage("0x0a11"), is("0x"));
        // Deposit Nonce
        assertThat(bridge.getStorage("0x0a12"), is("0x"));
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
                containsString("ASSERTMSG is executed with false result. Reason: Invalid recipient data."));
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
                containsString("ASSERTMSG is executed with false result. Reason: Invalid recipient data."));

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
                containsString("ASSERTMSG is executed with false result. Reason: Invalid recipient data."));
    }

    // endregion
    // region accepted deposits

    /**
     * @formatter:off
     * Pre Tree:
     * -
     * Post Tree:
     * d1
     * Post in storage (index -> value):
     * 0 -> d1
     * Post Root: d1
     * @formatter:on
     */
    @Test
    @Order(11)
    public void testRootComputation_1() throws Throwable {
        Account from = alice;
        Hash160 to = recipient1;
        BigInteger amount = minDeposit;
        BigInteger nextNonce = new BigInteger("1");

        Hash256 txHash = bridgeGas(from, to, amount, printDeposits.contains(1));

        String d1 = createDepositHash(nextNonce, to, amount);

        assertThat(bridge.findStorage("0x0b"), hasSize(1));
        assertThat(bridge.getStorage("0x0b"), is(d1));

        // root after first deposit is the deposit hash itself
        assertThat(bridge.getStorage("0x0a10"), is(d1));
        assertThat(bridge.depositRoot(), is(d1));

        TestHelper.DepositEvent depositEvent = getDepositEvent(txHash, neow3j);
        assertThat(depositEvent.nonce, is(nextNonce));
        assertThat(depositEvent.from, is(from.getScriptHash()));
        assertThat(depositEvent.to, is(to));
        assertThat(depositEvent.amount, is(amount));
        assertThat(depositEvent.depositHashHex, is(d1));
        assertThat(depositEvent.rootHashHex, is(d1));
    }

    /**
     * @formatter:off
     * Pre Tree:
     * d1
     * Post Tree:
     *   d12
     *  /  \
     * d1  d2
     * Post in storage (index -> value):
     * 0 -> -
     * 1 -> d12
     * Post Root: d12
     * @formatter:on
     */
    @Test
    @Order(12)
    public void testRootComputation_2() throws Throwable {
        Account from = bob;
        Hash160 to = recipient2;
        BigInteger amount = minDeposit;
        BigInteger nextNonce = new BigInteger("2");

        String depositRootBefore = bridge.depositRoot();

        Hash256 txHash = bridgeGas(from, to, amount, printDeposits.contains(2));

        String d2 = createDepositHash(nextNonce, to, amount);
        String d12 = concatAndSha256(depositRootBefore, d2);

        assertThat(bridge.findStorage("0x0b"), hasSize(1));
        assertThrows(RpcResponseErrorException.class, () -> bridge.getStorage("0x0b"));
        assertThat(bridge.getStorage("0x0b01"), is(d12));

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

    /**
     * @formatter:off
     * Pre Tree:
     *   d12
     *  /  \
     * d1  d2
     * Post Tree:
     *  d12
     * /  \
     * d1  d2  d3
     * Post in storage (index -> value):
     * 0 -> d3
     * 1 -> d12
     * Post Root: d12d3
     * @formatter:on
     */
    @Test
    @Order(13)
    public void testRootComputation_3() throws Throwable {
        Account from = charlie;
        Hash160 to = recipient3;
        BigInteger amount = minDeposit.multiply(new BigInteger("3"));
        BigInteger nextNonce = new BigInteger("3");

        assertThat(bridge.findStorage("0x0b"), hasSize(1));
        String d12 = bridge.getStorage("0x0b01");

        Hash256 txHash = bridgeGas(from, to, amount, printDeposits.contains(3));

        String d3 = createDepositHash(nextNonce, to, amount);
        String d12d3 = sha256Hex(concatLeftRight(d12, d3));

        assertThat(bridge.findStorage("0x0b"), hasSize(2));
        assertThat(bridge.getStorage("0x0b"), is(d3));
        assertThat(bridge.getStorage("0x0b01"), is(d12));
        assertThrows(RpcResponseErrorException.class, () -> bridge.getStorage("0x0b02"));

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

    /**
     * @formatter:off
     * Pre Tree:
     *   d12
     *  /  \
     * d1  d2  d3
     * Post Tree:
     *     d1234
     *    /     \
     *  d12     d34
     * /  \    /  \
     * d1  d2  d3  d4
     * Post in storage (index -> value):
     * 0 -> -
     * 1 -> -
     * 2 -> d1234
     * Post Root: d1234
     * @formatter:on
     */
    @Test
    @Order(14)
    public void testRootComputation_4() throws Throwable {
        Account from = denise;
        Hash160 to = recipient4;
        BigInteger amount = minDeposit.multiply(new BigInteger("4"));
        BigInteger nextNonce = new BigInteger("4");

        assertThat(bridge.findStorage("0x0b"), hasSize(2));
        String d3 = bridge.getStorage("0x0b");
        String d12 = bridge.getStorage("0x0b01");

        Hash256 txHash = bridgeGas(from, to, amount, printDeposits.contains(4));

        String depositHashOffChain = createDepositHash(nextNonce, to, amount);
        String d34 = sha256Hex(concatLeftRight(d3, depositHashOffChain));
        String d1234 = sha256Hex(concatLeftRight(d12, d34));

        assertThat(bridge.findStorage("0x0b"), hasSize(1));
        assertThat(bridge.getStorage("0x0b02"), is(d1234));

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

    /**
     * @formatter:off
     * Pre Tree:
     *      d1234
     *     /     \
     *   d12     d34
     *  /  \    /  \
     * d1  d2  d3  d4
     * Post Tree:
     *      d1234
     *     /     \
     *   d12     d34
     *  /  \    /  \
     * d1  d2  d3  d4  d5
     * Post in storage (index -> value):
     * 0 -> d5
     * 1 -> -
     * 2 -> d1234
     * Post Root: d1234d5
     * @formatter:on
     */
    @Test
    @Order(15)
    public void testRootComputation_5() throws Throwable {
        Account from = eve;
        Hash160 to = recipient5;
        BigInteger amount = minDeposit.multiply(new BigInteger("5"));
        BigInteger nextNonce = new BigInteger("5");

        assertThat(bridge.findStorage("0x0b"), hasSize(1));
        String d1234 = bridge.getStorage("0x0b02");

        Hash256 txHash = bridgeGas(from, to, amount, printDeposits.contains(5));

        String d5 = createDepositHash(nextNonce, to, amount);
        String d1234d5 = sha256Hex(concatLeftRight(d1234, d5));

        assertThat(bridge.findStorage("0x0b"), hasSize(2));
        assertThat(bridge.getStorage("0x0b"), is(d5));
        assertThat(bridge.getStorage("0x0b02"), is(d1234));

        assertThat(bridge.getStorage("0x0a10"), is(d1234d5));
        assertThat(bridge.depositRoot(), is(d1234d5));

        TestHelper.DepositEvent depositEvent = getDepositEvent(txHash, neow3j);
        assertThat(depositEvent.nonce, is(nextNonce));
        assertThat(depositEvent.from, is(from.getScriptHash()));
        assertThat(depositEvent.to, is(to));
        assertThat(depositEvent.amount, is(amount));
        assertThat(depositEvent.depositHashHex, is(d5));
        assertThat(depositEvent.rootHashHex, is(d1234d5));
    }

    /**
     * @formatter:off
     * Pre Tree:
     *      d1234
     *     /     \
     *   d12     d34
     *  /  \    /  \
     * d1  d2  d3  d4  d5
     * Post Tree:
     *      d1234
     *     /     \
     *   d12     d34     d56
     *  /  \    /  \    /  \
     * d1  d2  d3  d4  d5  d6
     * Post in storage (index -> value):
     * 0 -> -
     * 1 -> d56
     * 2 -> d1234
     * Post Root: d1234d56
     * @formatter:on
     */
    @Test
    @Order(16)
    public void testRootComputation_6() throws Throwable {
        Account from = eve;
        Hash160 to = recipient6;
        BigInteger amount = minDeposit.multiply(new BigInteger("6"));
        BigInteger nextNonce = new BigInteger("6");

        assertThat(bridge.findStorage("0x0b"), hasSize(2));
        String d5 = bridge.getStorage("0x0b");
        String d1234 = bridge.getStorage("0x0b02");

        Hash256 txHash = bridgeGas(from, to, amount, printDeposits.contains(6));

        String d6 = createDepositHash(nextNonce, to, amount);
        String d56 = sha256Hex(concatLeftRight(d5, d6));
        String d1234d56 = sha256Hex(concatLeftRight(d1234, d56));

        assertThat(bridge.findStorage("0x0b"), hasSize(2));
        assertThat(bridge.getStorage("0x0b01"), is(d56));
        assertThat(bridge.getStorage("0x0b02"), is(d1234));

        assertThat(bridge.getStorage("0x0a10"), is(d1234d56));
        assertThat(bridge.depositRoot(), is(d1234d56));

        TestHelper.DepositEvent depositEvent = getDepositEvent(txHash, neow3j);
        assertThat(depositEvent.nonce, is(nextNonce));
        assertThat(depositEvent.from, is(from.getScriptHash()));
        assertThat(depositEvent.to, is(to));
        assertThat(depositEvent.amount, is(amount));
        assertThat(depositEvent.depositHashHex, is(d6));
        assertThat(depositEvent.rootHashHex, is(d1234d56));
    }

    /**
     * @formatter:off
     * Pre Tree:
     *      d1234
     *     /     \
     *   d12     d34     d56
     *  /  \    /  \    /  \
     * d1  d2  d3  d4  d5  d6
     * Post Tree:
     *      d1234
     *     /     \
     *   d12     d34     d56
     *  /  \    /  \    /  \
     * d1  d2  d3  d4  d5  d6  d7
     * Post in storage (index -> value):
     * 0 -> d7
     * 1 -> d56
     * 2 -> d1234
     * Post Root: d1234(d56d7)
     * @formatter:on
     */
    @Test
    @Order(17)
    public void testRootComputation_7() throws Throwable {
        Account from = florian;
        Hash160 to = recipient7;
        BigInteger amount = minDeposit.multiply(new BigInteger("7"));
        BigInteger nextNonce = new BigInteger("7");

        assertThat(bridge.findStorage("0x0b"), hasSize(2));
        String d1234 = bridge.getStorage("0x0b02");
        String d56 = bridge.getStorage("0x0b01");

        Hash256 txHash = bridgeGas(from, to, amount, printDeposits.contains(7));

        String d7 = createDepositHash(nextNonce, to, amount);
        String d56d7 = sha256Hex(concatLeftRight(d56, d7));
        String d1234d56d7 = sha256Hex(concatLeftRight(d1234, d56d7));

        assertThat(bridge.findStorage("0x0b"), hasSize(3));
        assertThat(bridge.getStorage("0x0b"), is(d7));
        assertThat(bridge.getStorage("0x0b01"), is(d56));
        assertThat(bridge.getStorage("0x0b02"), is(d1234));

        assertThat(bridge.getStorage("0x0a10"), is(d1234d56d7));
        assertThat(bridge.depositRoot(), is(d1234d56d7));

        TestHelper.DepositEvent depositEvent = getDepositEvent(txHash, neow3j);
        assertThat(depositEvent.nonce, is(nextNonce));
        assertThat(depositEvent.from, is(from.getScriptHash()));
        assertThat(depositEvent.to, is(to));
        assertThat(depositEvent.amount, is(amount));
        assertThat(depositEvent.depositHashHex, is(d7));
        assertThat(depositEvent.rootHashHex, is(d1234d56d7));
    }

    /**
     * @formatter:off
     * Pre Tree:
     *      d1234
     *     /     \
     *   d12     d34     d56
     *  /  \    /  \    /  \
     * d1  d2  d3  d4  d5  d6  d7
     * Post Tree:
     *          d12345678
     *         /          \
     *     d1234          d5678
     *    /     \        /     \
     *  d12     d34     d56    d78
     * /  \    /  \    /  \    /  \
     * d1  d2  d3  d4  d5  d6  d7  d8
     * Post in storage (index -> value):
     * 0 -> -
     * 1 -> -
     * 2 -> -
     * 3 -> d12345678
     * Post Root: d12345678
     * @formatter:on
     */
    @Test
    @Order(18)
    public void testRootComputation_8() throws Throwable {
        Account from = gabriel;
        Hash160 to = recipient8;
        BigInteger amount = minDeposit;
        BigInteger nextNonce = new BigInteger("8");

        assertThat(bridge.findStorage("0x0b"), hasSize(3));
        String d7 = bridge.getStorage("0x0b");
        String d56 = bridge.getStorage("0x0b01");
        String d1234 = bridge.getStorage("0x0b02");

        Hash256 txHash = bridgeGas(from, to, amount, printDeposits.contains(8));

        String d8 = createDepositHash(nextNonce, to, amount);
        String d78 = sha256Hex(concatLeftRight(d7, d8));
        String d5678 = sha256Hex(concatLeftRight(d56, d78));
        String d12345678 = sha256Hex(concatLeftRight(d1234, d5678));

        assertThat(bridge.findStorage("0x0b"), hasSize(1));
        assertThat(bridge.getStorage("0x0b03"), is(d12345678));

        assertThat(bridge.getStorage("0x0a10"), is(d12345678));
        assertThat(bridge.depositRoot(), is(d12345678));

        TestHelper.DepositEvent depositEvent = getDepositEvent(txHash, neow3j);
        assertThat(depositEvent.nonce, is(nextNonce));
        assertThat(depositEvent.from, is(from.getScriptHash()));
        assertThat(depositEvent.to, is(to));
        assertThat(depositEvent.amount, is(amount));
        assertThat(depositEvent.depositHashHex, is(d8));
        assertThat(depositEvent.rootHashHex, is(d12345678));
    }

    /**
     * @formatter:off
     * Pre Tree:
     *           d12345678
     *         /          \
     *      d1234          d5678
     *     /     \        /     \
     *   d12     d34     d56    d78
     *  /  \    /  \    /  \    /  \
     * d1  d2  d3  d4  d5  d6  d7  d8
     * Post Tree:
     *            d12345678
     *          /          \
     *      d1234          d5678
     *     /     \        /     \
     *   d12     d34     d56    d78
     *  /  \    /  \    /  \    /  \
     * d1  d2  d3  d4  d5  d6  d7  d8  d9
     * Post in storage (index -> value):
     * 0 -> d9
     * 1 -> -
     * 2 -> -
     * 3 -> d12345678
     * Post Root: d12345678d9
     * @formatter:on
     */
    @Test
    @Order(19)
    public void testRootComputation_9() throws Throwable {
        Account from = henry;
        Hash160 to = recipient9;
        BigInteger amount = minDeposit;
        BigInteger nextNonce = new BigInteger("9");

        assertThat(bridge.findStorage("0x0b"), hasSize(1));
        String d12345678 = bridge.getStorage("0x0b03");

        Hash256 txHash = bridgeGas(from, to, amount, printDeposits.contains(9));

        String d9 = createDepositHash(nextNonce, to, amount);
        String d12345678d9 = sha256Hex(concatLeftRight(d12345678, d9));

        assertThat(bridge.findStorage("0x0b"), hasSize(2));
        assertThat(bridge.getStorage("0x0b"), is(d9));
        assertThat(bridge.getStorage("0x0b03"), is(d12345678));

        assertThat(bridge.getStorage("0x0a10"), is(d12345678d9));
        assertThat(bridge.depositRoot(), is(d12345678d9));

        TestHelper.DepositEvent depositEvent = getDepositEvent(txHash, neow3j);
        assertThat(depositEvent.nonce, is(nextNonce));
        assertThat(depositEvent.from, is(from.getScriptHash()));
        assertThat(depositEvent.to, is(to));
        assertThat(depositEvent.amount, is(amount));
        assertThat(depositEvent.depositHashHex, is(d9));
        assertThat(depositEvent.rootHashHex, is(d12345678d9));
    }

    @Test
    @Order(20)
    public void testRootComputation_depositAnother42Times() throws Throwable {
        Account from = isabella;
        Hash160 to = recipient0;
        BigInteger amount = minDeposit;
        BigInteger nextNonce = new BigInteger("10");

        // previous contract storage state
        String d9 = bridge.getStorage("0x0b");
        String d18 = bridge.getStorage("0x0b03");

        // compute expected root after another 42 deposits
        List<String> leaves = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            leaves.add(null); // Unknown leaf values -> subtrees are stored on-chain
        }
        leaves.add(d9);
        for (int i = 10; i < 52; i++) {
            leaves.add(createDepositHash(nextNonce, to, amount));
            nextNonce = nextNonce.add(BigInteger.ONE);
        }
        assertThat(leaves, hasSize(51));

        // Compute complete subtree of leaves #1 to #32, with subtrees of leaves #1 to #9 being in storage on-chain
        String subTree916 = buildSubTree(8, leaves, 8);
        String subTree116 = concatAndSha256(d18, subTree916);
        String subTree1632 = buildSubTree(16, leaves, 16);
        String subTree132 = concatAndSha256(subTree116, subTree1632);

        // Compute complete subtrees after deposit #32
        String subTree3348 = buildSubTree(16, leaves, 32);
        String subTree4950 = buildSubTree(2, leaves, 48);
        String leaf51 = leaves.get(50);

        String expectedRoot = concatAndSha256(
                subTree132,
                concatAndSha256(
                        subTree3348,
                        concatAndSha256(subTree4950, leaf51)
                )
        );

        // execute another 42 deposits
        Hash256 txHash = null;
        for (int i = 10; i < 52; i++) {
            txHash = bridgeGas(from, to, amount, printDeposits.contains(i));
        }

        assertThat(bridge.findStorage("0x0b"), hasSize(4));
        // Total of 51 leafs => complete subtrees: 32+16+2+1=51
        String d51 = bridge.getStorage("0x0b"); // 1
        String d4950 = bridge.getStorage("0x0b01"); // 2
        String d3348 = bridge.getStorage("0x0b04"); // 16
        String d132 = bridge.getStorage("0x0b05"); // 32

        assertThat(d51, is(leaf51));
        assertThat(d4950, is(subTree4950));
        assertThat(d3348, is(subTree3348));
        assertThat(d132, is(subTree132));

        assertThat(bridge.getStorage("0x0a10"), is(expectedRoot));
        assertThat(bridge.depositRoot(), is(expectedRoot));

        TestHelper.DepositEvent depositEvent = getDepositEvent(txHash, neow3j);
        assertThat(depositEvent.nonce, is(new BigInteger("51")));
        assertThat(depositEvent.from, is(from.getScriptHash()));
        assertThat(depositEvent.to, is(to));
        assertThat(depositEvent.amount, is(amount));
        assertThat(depositEvent.depositHashHex, is(leaf51));
        assertThat(depositEvent.rootHashHex, is(expectedRoot));
    }

    // endregion

}
