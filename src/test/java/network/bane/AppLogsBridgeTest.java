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
import io.neow3j.utils.Numeric;
import io.neow3j.wallet.Account;
import network.bane.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.*;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static io.neow3j.utils.Numeric.*;
import static java.util.Arrays.asList;
import static network.bane.util.TestHelper.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ContractTest(
        blockTime = 1,
        contracts = {BridgeManagementContract.class, AppLogsBridgeContract.class, TestContract.class},
        batchFile = "setup.batch"
)

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AppLogsBridgeTest {

    private static final BigInteger depositPrice = new BigInteger("10000000");
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
        bridge = new Bridge(ext.getDeployedContract(AppLogsBridgeContract.class).getScriptHash(), neow3j);
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

    @DeployConfig(AppLogsBridgeContract.class)
    public static DeployConfiguration deployConfigBridge() {
        DeployConfiguration config = new DeployConfiguration();
        config.setDeployParam(
                prepareBridgeDeployParameter(
                        managementContractHash,
                        depositPrice,
                        minDeposit,
                        maxDeposit,
                        maxWithdrawalPerRootUpdate
                )
        );
        return config;
    }

    private static ContractParameter prepareBridgeDeployParameter(Hash160 managementContractHash,
                                                                  BigInteger depositPrice, BigInteger minDeposit, BigInteger maxDeposit, BigInteger maxWithdrawalPerRootUpdate) {
        return array(
                hash160(managementContractHash),
                integer(depositPrice),
                integer(minDeposit),
                integer(maxDeposit),
                integer(maxWithdrawalPerRootUpdate)
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
//        assertThat(bridge.getStorage("0x0a05"), is(prependHexPrefix(reverseHexString("0x0a"))));

        assertThat(bridge.management(), is(managementContractHash));
        assertThat(bridge.depositPrice(), is(depositPrice));
        assertThat(bridge.minDeposit(), is(minDeposit));
        assertThat(bridge.maxDeposit(), is(maxDeposit));

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
//
//    // endregion

    /**
     * test deposit function
     * @throws Throwable
     */
    @Test
    @Order(11)
    public void testDeposit_1() throws Throwable {
        Account from = alice;
        Hash160 to = recipient1;
        BigInteger amount = minDeposit;
        BigInteger nextNonce = new BigInteger("1");

        Hash256 txHash = bridgeGas(from, to, amount, printDeposits.contains(1));

        TestHelper.DepositEvent depositEvent = getDepositEvent(txHash, neow3j);
        assertThat(depositEvent.nonce, is(nextNonce));
        assertThat(depositEvent.from, is(from.getScriptHash()));
        assertThat(depositEvent.to, is(to));
        assertThat(depositEvent.amount, is(amount));
    }

    @Test
    @Order(12)
    public void testDeposit_2() throws Throwable {
        Account from = alice;
        Hash160 to = recipient1;
        BigInteger amount = minDeposit;
        BigInteger nextNonce1 = new BigInteger("2");
        BigInteger nextNonce2 = new BigInteger("3");

        Hash256 txHash1 = bridgeGas(from, to, amount, printDeposits.contains(1));

        TestHelper.DepositEvent depositEvent = getDepositEvent(txHash1, neow3j);
        assertThat(depositEvent.nonce, is(nextNonce1));
        assertThat(depositEvent.from, is(from.getScriptHash()));
        assertThat(depositEvent.to, is(to));
        assertThat(depositEvent.amount, is(amount));

        Hash256 txHash2 = bridgeGas(from, to, amount, printDeposits.contains(1));

        depositEvent = getDepositEvent(txHash2, neow3j);
        assertThat(depositEvent.nonce, is(nextNonce2));
        assertThat(depositEvent.from, is(from.getScriptHash()));
        assertThat(depositEvent.to, is(to));
        assertThat(depositEvent.amount, is(amount));
    }

    @Test
    @Order(13)
    public void testWithdrawal_1() throws Throwable {
        Hash160 to = alice.getScriptHash();
        BigInteger amount1 = new BigInteger("1000000000");
        bridgeGas(bob, to, amount1, false);
        BigInteger amount = minDeposit;
        BigInteger nonce = new BigInteger("1");

        String d1 = createDepositHash(nonce, to, amount);
        String[] leafNodes = new String[32];
        Arrays.fill(leafNodes, Numeric.toHexString(Hash256.ZERO.toArray()));
        leafNodes[0] = d1;
        String root = buildSubTree(32, Arrays.asList(leafNodes), 0);
        MerkleTree tree = new MerkleTree(Arrays.asList(leafNodes));
        assertThat(root, is(tree.getRoot()));

        WithdrawalWithProof withdrawalWithProof = tree.getProof(d1);
        List<Account> validators = Arrays.asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter withdrawal = array(array(hash160(to), integer(amount), integer(nonce), integer(withdrawalWithProof.path), array(withdrawalWithProof.proof)));

        Hash256 txHash = withdrawalGas(tree.getRoot(), signMsg(validators, root), withdrawal);
        TestHelper.WithdrawEvent withdrawEvent = getWithdrawEvent(txHash, neow3j);
        assertThat(withdrawEvent.nonce, is(nonce));
        assertThat(withdrawEvent.to, is(to));
        assertThat(withdrawEvent.amount, is(amount));

        TestHelper.TransferEvent transferEvent = getTransferEvent(txHash, neow3j, 0);
        assertThat(transferEvent.from, is(bridge.getScriptHash()));
        assertThat(transferEvent.to, is(to));
        assertThat(transferEvent.amount, is(amount));
    }

    @Test
    @Order(14)
    public void testWithdrawal_2() throws Throwable {
        Hash160 to = alice.getScriptHash();
        BigInteger amount = new BigInteger("1000000000");
        bridgeGas(bob, to, amount, false);
        BigInteger amount1 = minDeposit;
        BigInteger nonce1 = new BigInteger("2");
        BigInteger amount2 = minDeposit;
        BigInteger nonce2 = new BigInteger("3");

        String d1 = createDepositHash(nonce1, to, amount1);
        String d2 = createDepositHash(nonce2, to, amount2);
        String[] leafNodes = new String[32];
        Arrays.fill(leafNodes, Numeric.toHexString(Hash256.ZERO.toArray()));
        leafNodes[0] = d1;
        leafNodes[15] = d2;
        MerkleTree tree = new MerkleTree(Arrays.asList(leafNodes));

        WithdrawalWithProof withdrawalWithProof1 = tree.getProof(d1);
        WithdrawalWithProof withdrawalWithProof2 = tree.getProof(d2);
        List<Account> validators = Arrays.asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter withdrawal = array(array(hash160(to), integer(amount1), integer(nonce1), integer(withdrawalWithProof1.path), array(withdrawalWithProof1.proof)),
                array(hash160(to), integer(amount2), integer(nonce2), integer(withdrawalWithProof2.path), array(withdrawalWithProof2.proof)));
        Hash256 txHash = withdrawalGas(tree.getRoot(), signMsg(validators, tree.getRoot()), withdrawal);
        TestHelper.WithdrawEvent withdrawEvent = getWithdrawEvent(txHash, neow3j);
        assertThat(withdrawEvent.nonce, is(nonce1));
        assertThat(withdrawEvent.to, is(to));
        assertThat(withdrawEvent.amount, is(amount1));

        TestHelper.TransferEvent transferEvent = getTransferEvent(txHash, neow3j, 0);
        assertThat(transferEvent.from, is(bridge.getScriptHash()));
        assertThat(transferEvent.to, is(to));
        assertThat(transferEvent.amount, is(amount1));

        transferEvent = getTransferEvent(txHash, neow3j, 2);
        assertThat(transferEvent.from, is(bridge.getScriptHash()));
        assertThat(transferEvent.to, is(to));
        assertThat(transferEvent.amount, is(amount2));
    }

    @Test
    @Order(15)
    public void testWithdrawal_3() throws Throwable {
        Hash160 to = testContract;
        BigInteger amount = minDeposit;
        BigInteger nonce = new BigInteger("4");

        String d1 = createDepositHash(nonce, to, amount);
        String[] leafNodes = new String[32];
        Arrays.fill(leafNodes, Numeric.toHexString(Hash256.ZERO.toArray()));
        leafNodes[0] = d1;
        MerkleTree tree = new MerkleTree(Arrays.asList(leafNodes));

        WithdrawalWithProof withdrawalWithProof = tree.getProof(d1);
        List<Account> validators = Arrays.asList(validator1, validator2, validator3, validator4, validator5);
        ContractParameter withdrawal = array(array(hash160(to), integer(amount), integer(nonce), integer(withdrawalWithProof.path), array(withdrawalWithProof.proof)));
        Hash256 txHash = withdrawalGas(tree.getRoot(), signMsg(validators, tree.getRoot()), withdrawal);
        TestHelper.ClaimableEvent claimableEvent = getClaimableEvent(txHash, neow3j);
        assertThat(claimableEvent.nonce, is(nonce));
        assertThat(claimableEvent.to, is(to));
        assertThat(claimableEvent.amount, is(amount));
    }

    @Test
    @Order(16)
    public void testWithdrawal_4() throws Throwable {
        Hash160 to = testContract;
        BigInteger amount = minDeposit;
        BigInteger nonce = new BigInteger("4");

        Hash256 txHash = claimGas(nonce.intValue());
        TestHelper.ClaimEvent claimEvent = getClaimEvent(txHash, neow3j);
        assertThat(claimEvent.nonce, is(nonce));
        assertThat(claimEvent.to, is(to));
        assertThat(claimEvent.amount, is(amount));

        TestHelper.TransferEvent transferEvent = getTransferEvent(txHash, neow3j, 0);
        assertThat(transferEvent.from, is(bridge.getScriptHash()));
        assertThat(transferEvent.to, is(to));
        assertThat(transferEvent.amount, is(amount));
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

    private Hash256 withdrawalGas(String withdrawalRoot, Map<ContractParameter, ContractParameter> signatures,
                                  ContractParameter withdrawals) throws Throwable {
        NeoSendRawTransaction response = bridge.invokeFunction("withdraw", byteArray(withdrawalRoot), map(signatures), withdrawals)
                .signers(AccountSigner.calledByEntry(relayer))
                .sign()
                .send();
        Hash256 txHash = response.getSendRawTransaction().getHash();
        waitUntilTransactionIsExecuted(txHash, neow3j);
        return txHash;
    }
}
