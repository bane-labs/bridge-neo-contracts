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
import network.bane.bridge.BridgeContract;
import network.bane.management.BridgeManagementContract;
import network.bane.message.MessageBridgeContract;
import network.bane.messageexecution.ExecutionManagerContract;
import network.bane.testhelper.MessageTestStoreContract;
import network.bane.testhelper.TestContract;
import network.bane.util.Bridge;
import network.bane.util.ExecutionManager;
import network.bane.util.Management;
import network.bane.util.MessageBridge;
import network.bane.util.MessageTestStorer;
import network.bane.util.structs.MessageBridgeDto;
import network.bane.util.structs.TokenBridge;

import java.math.BigDecimal;
import java.math.BigInteger;

import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static java.util.Arrays.asList;
import static network.bane.util.TestHelper.governorScriptHash;
import static network.bane.util.TestHelper.owner;
import static network.bane.util.TestHelper.ownerScriptHash;
import static network.bane.util.TestHelper.prepareManagementDeployParameter;
import static network.bane.util.TestHelper.relayerScriptHash;
import static network.bane.util.TestHelper.securityGuardScriptHash;
import static network.bane.util.TestHelper.validator1PubKey;
import static network.bane.util.TestHelper.validator2PubKey;
import static network.bane.util.TestHelper.validator3PubKey;
import static network.bane.util.TestHelper.validator4PubKey;
import static network.bane.util.TestHelper.validator5PubKey;
import static network.bane.util.TestHelper.validator6PubKey;
import static network.bane.util.TestHelper.validator7PubKey;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_LINKED_CHAIN_ID;
import static network.bane.util.helper.DefaultTestValues.EXECUTION_MANAGER_CONTRACT_HASH;
import static network.bane.util.helper.DefaultTestValues.MANAGEMENT_CONTRACT_HASH;
import static network.bane.util.helper.DefaultTestValues.MESSAGE_BRIDGE_CONTRACT_HASH;
import static network.bane.util.helper.NetworkSettingsHelper.updateNetworkSettings;

public class TestHelper {

    public static final BigInteger nativeDepositFee = new BigInteger("10000000");
    public static final BigInteger minNativeDeposit = new BigInteger("100000000");
    public static final BigInteger maxNativeDeposit = new BigInteger("1000000000000");
    public static final int maxNativeWithdrawals = 100;
    public static final BigInteger maxTotalNativeDepositAmount = new BigInteger("10000000000000");

    public static Bridge bridge;
    public static Management management;
    public static MessageBridge messageBridge;
    public static ExecutionManager executionManager;
    public static MessageTestStorer messageTestStorer;

    public static Hash160 testContract;
    public static final Hash160 neoXNeoTokenHash = new Hash160("0x5615dEB798BB3E4dFa0139dFa1b3D433Cc23b72f");
    public static final Hash160 neoN3NeoTokenHash = NeoToken.SCRIPT_HASH;

    public static Neow3j neow3j;
    public static GasToken gasToken;
    public static NeoToken neoToken;
    public static PolicyContract policyContract;

    public static BigInteger withdrawalNonce = BigInteger.ZERO;
    public static BigInteger depositNonce = BigInteger.ZERO;

    public static BigInteger n3MessageNonce = BigInteger.ZERO;
    public static BigInteger evmMessageNonce = BigInteger.ZERO;

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

    public static void setupManagement(ContractTestExtension ext) {
        management = new Management(ext.getDeployedContract(BridgeManagementContract.class).getScriptHash(), neow3j);
    }

    public static void setupBridge(ContractTestExtension ext) {
        setupManagement(ext);
        bridge = new Bridge(ext.getDeployedContract(BridgeContract.class).getScriptHash(), neow3j);
        setupTestContract(ext);
    }

    public static void setupTestContract(ContractTestExtension ext) {
        testContract = ext.getDeployedContract(TestContract.class).getScriptHash();
    }

    public static void setupMessageBridge(ContractTestExtension ext) {
        setupManagement(ext);
        setupTestContract(ext);
        messageBridge = new MessageBridge(ext.getDeployedContract(MessageBridgeContract.class).getScriptHash(), neow3j);
    }

    public static void setupExecutionManager(ContractTestExtension ext) {
        executionManager = new ExecutionManager(ext.getDeployedContract(ExecutionManagerContract.class).getScriptHash(),
                neow3j);
        messageTestStorer = new MessageTestStorer(
                ext.getDeployedContract(MessageTestStoreContract.class).getScriptHash(), neow3j);
    }

    public static DeployConfiguration createBridgeManagementDeployConfig() {
        DeployConfiguration config = new DeployConfiguration();
        config.setDeployParam(
                prepareManagementDeployParameter(
                        ownerScriptHash,
                        relayerScriptHash,
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
                        governorScriptHash,
                        securityGuardScriptHash
                )
        );
        AccountSigner deploySigner = AccountSigner.none(owner);
        WitnessRule deployWitnessRule = new WitnessRule(WitnessAction.ALLOW,
                new CalledByContractCondition(ContractManagement.SCRIPT_HASH));
        deploySigner.setRules(deployWitnessRule);
        config.setSigner(deploySigner);
        config.setSubstitution("BridgeManagementName", "NeoXBridgeManagement");
        return config;
    }

    public static DeployConfiguration createBridgeDeployConfig() {
        DeployConfiguration config = new DeployConfiguration();
        config.setDeployParam(
                prepareBridgeDeployParameter(
                        DEFAULT_LINKED_CHAIN_ID,
                        MANAGEMENT_CONTRACT_HASH
                )
        );
        AccountSigner deploySigner = AccountSigner.none(owner);
        WitnessRule deployWitnessRule = new WitnessRule(WitnessAction.ALLOW,
                new CalledByContractCondition(ContractManagement.SCRIPT_HASH));
        deploySigner.setRules(deployWitnessRule);
        config.setSigner(deploySigner);
        config.setSubstitution("BridgeName", "NeoXBridge");
        return config;
    }

    public static DeployConfiguration createMessageBridgeDeployConfig() {
        DeployConfiguration config = new DeployConfiguration();
        config.setDeployParam(
                prepareMessageBridgeDeployParameter(
                        DEFAULT_LINKED_CHAIN_ID,
                        MANAGEMENT_CONTRACT_HASH,
                        EXECUTION_MANAGER_CONTRACT_HASH
                )
        );
        config.setSigner(AccountSigner.none(owner));
        return config;
    }

    private static ContractParameter prepareMessageBridgeDeployParameter(BigInteger linkedChain,
            Hash160 managementContractHash, Hash160 executionManager) {
        return array(
                integer(linkedChain),
                hash160(managementContractHash),
                hash160(executionManager)
        );
    }

    public static DeployConfiguration createExecutionManagerDeployConfig() {
        DeployConfiguration config = new DeployConfiguration();
        config.setDeployParam(
                prepareExecutionManagerDeployParameter(
                        MANAGEMENT_CONTRACT_HASH,
                        MESSAGE_BRIDGE_CONTRACT_HASH
                )
        );
        AccountSigner deploySigner = AccountSigner.none(owner);
        config.setSigner(deploySigner);
        return config;
    }

    public static ContractParameter prepareBridgeDeployParameter(BigInteger linkedChainId,
            Hash160 managementContractHash) {
        return array(
                integer(linkedChainId),
                hash160(managementContractHash)
        );
    }

    private static ContractParameter prepareExecutionManagerDeployParameter(Hash160 managementContractHash,
            Hash160 bridgeContractHash) {
        return array(
                hash160(managementContractHash),
                hash160(bridgeContractHash)
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

    public static BigInteger incrementAndGetN3MessageNonce() {
        n3MessageNonce = n3MessageNonce.add(BigInteger.ONE);
        return n3MessageNonce;
    }

    public static void decrementN3MessageNonce() {
        n3MessageNonce = n3MessageNonce.subtract(BigInteger.ONE);
    }

    public static void registerNeoTokenBridge() throws Throwable {
        BigInteger fee = gasToken.toFractions(new BigDecimal("0.1"));
        BigInteger minAmount = BigInteger.ONE;
        BigInteger maxAmount = new BigInteger("1000");
        int maxWithdrawals = 100;
        int decimalScalingFactor = 0;
        TokenBridge.TokenConfig config = new TokenBridge.TokenConfig(neoXNeoTokenHash, fee, minAmount, maxAmount,
                maxWithdrawals, decimalScalingFactor);

        Hash256 txHash = bridge.registerToken(neoN3NeoTokenHash, config);
        Await.waitUntilTransactionIsExecuted(txHash, neow3j);
        Hash256 unpauseTxHash = bridge.unpauseTokenBridge(neoN3NeoTokenHash);
        Await.waitUntilTransactionIsExecuted(unpauseTxHash, neow3j);
    }

}
