package network.bane.util.helper;

import io.neow3j.contract.ContractManagement;
import io.neow3j.contract.GasToken;
import io.neow3j.contract.NeoToken;
import io.neow3j.contract.PolicyContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.test.ContractTestExtension;
import io.neow3j.test.DeployConfiguration;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.witnessrule.CalledByContractCondition;
import io.neow3j.transaction.witnessrule.WitnessAction;
import io.neow3j.transaction.witnessrule.WitnessRule;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.wallet.Account;
import network.bane.bridge.BridgeContract;
import network.bane.management.BridgeManagementContract;
import network.bane.testhelper.TestContract;
import network.bane.util.Bridge;
import network.bane.util.Management;

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

public class TestHelper {

    public static final BigInteger gasDepositFee = new BigInteger("10000000");
    public static final BigInteger minGasDeposit = new BigInteger("100000000");
    public static final BigInteger maxGasDeposit = new BigInteger("1000000000000");
    public static final BigInteger maxWithdrawals = new BigInteger("100");

    public static Bridge bridge;
    public static Management management;

    public static Hash160 testContract;

    public static Neow3j neow3j;
    public static GasToken gasToken;
    public static NeoToken neoToken;
    public static PolicyContract policyContract;

    public static BigInteger withdrawalNonce = BigInteger.ZERO;
    public static BigInteger depositNonce = BigInteger.ZERO;

    public static Account alice;
    public static Account bob;
    public static Account charlie;
    public static Account denise;
    public static Account eve;
    public static Account florian;
    public static Account gabriel;
    public static Account henry;
    public static Account isabella;

    public static Account committee;

    public static void setup(ContractTestExtension ext) throws Throwable {
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
        alice = ext.getAccount(network.bane.util.TestHelper.ALICE);
        committee = Account.createMultiSigAccount(asList(alice.getECKeyPair().getPublicKey()), 1);
        bob = ext.getAccount(network.bane.util.TestHelper.BOB);
        charlie = ext.getAccount(network.bane.util.TestHelper.CHARLIE);
        denise = ext.getAccount(network.bane.util.TestHelper.DENISE);
        eve = ext.getAccount(network.bane.util.TestHelper.EVE);
        florian = ext.getAccount(network.bane.util.TestHelper.FLORIAN);
        gabriel = ext.getAccount(network.bane.util.TestHelper.GABRIEL);
        henry = ext.getAccount(network.bane.util.TestHelper.HENRY);
        isabella = ext.getAccount(network.bane.util.TestHelper.ISABELLA);

        updateNetworkSettings(neow3j, committee, alice);
    }

    public static DeployConfiguration createBridgeManagementDeployConfig() {
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

    public static DeployConfiguration createBridgeDeployConfig() {
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

    public static BigInteger incrementAndGetWithdrawalNonce() {
        withdrawalNonce = withdrawalNonce.add(BigInteger.ONE);
        return withdrawalNonce;
    }

    public static BigInteger incrementAndGetDepositNonce() {
        depositNonce = depositNonce.add(BigInteger.ONE);
        return depositNonce;
    }

}
