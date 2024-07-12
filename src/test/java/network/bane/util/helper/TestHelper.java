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
import io.neow3j.types.Hash256;
import io.neow3j.utils.Await;
import io.neow3j.wallet.Account;
import network.bane.testhelper.TestContract;
import network.bane.bridge.BridgeContract;
import network.bane.management.BridgeManagementContract;
import network.bane.util.Nep17Token;
import network.bane.util.Bridge;
import network.bane.util.Management;
import network.bane.util.structs.ExecutionType;
import network.bane.util.structs.TokenBridge;

import java.math.BigDecimal;
import java.math.BigInteger;

import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static java.util.Arrays.asList;
import static network.bane.util.TestHelper.*;
import static network.bane.util.helper.DefaultTestValues.MANAGEMENT_CONTRACT_HASH;
import static network.bane.util.helper.NetworkSettingsHelper.updateNetworkSettings;

public class TestHelper {

    public static final BigInteger gasDepositFee = new BigInteger("10000000");
    public static final BigInteger minGasDeposit = new BigInteger("100000000");
    public static final BigInteger maxGasDeposit = new BigInteger("1000000000000");
    public static final BigInteger maxWithdrawals = new BigInteger("100");

    public static Bridge bridge;
    public static Management management;

    public static Nep17Token nep17Token;
    public static Hash160 testContract;
    public static final Hash160 nep17TokenXTokenHash = new Hash160("0x8095581030409afc716d5f35ce5172e13d7ba316");
    public static final Hash160 neoXNeoTokenHash = new Hash160("0x5615dEB798BB3E4dFa0139dFa1b3D433Cc23b72f");
    public static final Hash160 neoN3NeoTokenHash = NeoToken.SCRIPT_HASH;

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

    public static void setupBridge(ContractTestExtension ext) {
        management = new Management(ext.getDeployedContract(BridgeManagementContract.class).getScriptHash(), neow3j);
        assert management.getScriptHash().equals(MANAGEMENT_CONTRACT_HASH) :
                "BridgeManagement Contract or its deployer has changed. Change the contract hash in this test to " +
                        management.getScriptHash() + ".";
        bridge = new Bridge(ext.getDeployedContract(BridgeContract.class).getScriptHash(), neow3j);
        testContract = ext.getDeployedContract(TestContract.class).getScriptHash();
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

    public static DeployConfiguration createTestDeployConfig() {
        DeployConfiguration config = new DeployConfiguration();
        config.setDeployParam( hash160(ownerScriptHash));
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

    public static void registerNeoTokenBridge() throws Throwable {
        BigInteger fee = gasToken.toFractions(new BigDecimal("0.1"));
        BigInteger minAmount = BigInteger.ONE;
        BigInteger maxAmount = new BigInteger("1000");
        int maxWithdrawals = 100;
        ExecutionType executionType = ExecutionType.NEO;
        TokenBridge.TokenConfig config = new TokenBridge.TokenConfig(neoXNeoTokenHash, fee, minAmount, maxAmount,
                maxWithdrawals, executionType);

        Hash256 txHash = bridge.registerToken(neoN3NeoTokenHash, config);
        Await.waitUntilTransactionIsExecuted(txHash, neow3j);
    }

    public static void registerTokenBridge(Hash160 token) throws Throwable {
        BigInteger fee = gasToken.toFractions(new BigDecimal("0.1"));
        BigInteger minAmount = BigInteger.ONE;
        BigInteger maxAmount = new BigInteger("1000");
        int maxWithdrawals = 100;
        ExecutionType executionType = ExecutionType.NEP17;
        TokenBridge.TokenConfig config = new TokenBridge.TokenConfig(nep17TokenXTokenHash, fee, minAmount, maxAmount,
                maxWithdrawals, executionType);

        Hash256 txHash = bridge.registerToken(token, config);
        Await.waitUntilTransactionIsExecuted(txHash, neow3j);
    }

    public static void unregisterTokenBridge(Hash160 token) throws Throwable {
        Hash256 txHash = bridge.unregisterToken(token);
        Await.waitUntilTransactionIsExecuted(txHash, neow3j);
    }

}
