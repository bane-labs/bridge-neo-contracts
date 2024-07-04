package network.bane.bridge;

import io.neow3j.contract.ContractManagement;
import io.neow3j.contract.GasToken;
import io.neow3j.contract.NeoToken;
import io.neow3j.contract.PolicyContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.test.ContractTest;
import io.neow3j.test.ContractTestExtension;
import io.neow3j.test.DeployConfig;
import io.neow3j.test.DeployConfiguration;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.witnessrule.CalledByContractCondition;
import io.neow3j.transaction.witnessrule.WitnessAction;
import io.neow3j.transaction.witnessrule.WitnessRule;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.wallet.Account;
import network.bane.management.BridgeManagementContract;
import network.bane.testhelper.TestContract;
import network.bane.util.Bridge;
import network.bane.util.Management;
import network.bane.util.TestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigInteger;

import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static java.util.Arrays.asList;
import static network.bane.util.TestHelper.governorPubKey;
import static network.bane.util.TestHelper.owner;
import static network.bane.util.TestHelper.ownerPubKey;
import static network.bane.util.TestHelper.prepareManagementDeployParameter;
import static network.bane.util.TestHelper.relayerPubKey;
import static network.bane.util.TestHelper.securityGuardPubKey;
import static network.bane.util.TestHelper.validator1PubKey;
import static network.bane.util.TestHelper.validator2PubKey;
import static network.bane.util.TestHelper.validator3PubKey;
import static network.bane.util.TestHelper.validator4PubKey;
import static network.bane.util.TestHelper.validator5PubKey;
import static network.bane.util.TestHelper.validator6PubKey;
import static network.bane.util.TestHelper.validator7PubKey;
import static network.bane.util.helper.DefaultTestValues.MANAGEMENT_CONTRACT_HASH;
import static network.bane.util.helper.NetworkSettingsHelper.updateNetworkSettings;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ContractTest(
        blockTime = 1,
        contracts = {BridgeManagementContract.class, BridgeContract.class, TestContract.class},
        batchFile = "setup.batch"
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TokenBridgeRegistrationTest {
    private static final BigInteger gasDepositFee = new BigInteger("10000000");
    private static final BigInteger minGasDeposit = new BigInteger("100000000");
    private static final BigInteger maxGasDeposit = new BigInteger("1000000000000");
    private static final BigInteger maxWithdrawals = new BigInteger("100");

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
        assert management.getScriptHash().equals(MANAGEMENT_CONTRACT_HASH) :
                "BridgeManagement Contract or its deployer has changed. Change the contract hash in this test to " +
                        management.getScriptHash() + ".";
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

        updateNetworkSettings(neow3j, committee, alice);
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
                        MANAGEMENT_CONTRACT_HASH,
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
    // region helper general

    private static BigInteger incrementAndGetWithdrawalNonce() {
        withdrawalNonce = withdrawalNonce.add(BigInteger.ONE);
        return withdrawalNonce;
    }

    private static BigInteger incrementAndGetDepositNonce() {
        depositNonce = depositNonce.add(BigInteger.ONE);
        return depositNonce;
    }

    // endregion
    // region test successful token bridge registration

    @Order(0)
    @Test
    public void testTokenRegistration() {
        // TODO:  Register a token bridge and check the thrown event matches the provided config
        assertFalse(true);
    }

    // TODO: Test getTokenBridge(Hash160)

    // TODO: Test getRegisteredTokens()

    // TODO: Test getRegisteredTokensIterator()

    // endregion
    // region test invalid token bridge registrations

    // TODO: Fail if governor does not sign registration transaction

    // TODO: Fail if the token config parameter is an invalid configuration

    // TODO: Fail if for the token an already registered token bridge exists

    // endregion

}
