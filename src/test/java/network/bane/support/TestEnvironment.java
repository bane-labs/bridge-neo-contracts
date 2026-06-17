package network.bane.support;

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
import network.bane.client.MessageBridgeTestClient;
import network.bane.management.BridgeManagementContract;
import network.bane.message.MessageBridgeContract;
import network.bane.messageexecution.ExecutionManagerContract;
import network.bane.testhelper.MessageTestStoreContract;
import network.bane.testhelper.TestContract;
import network.bane.testhelper.TestMessageSenderContract;
import network.bane.client.BridgeTestClient;
import network.bane.client.ManagementTestClient;
import network.bane.dto.bridge.TokenBridge;
import network.bane.support.contract.ExecutionManager;
import network.bane.support.contract.MessageTestStorer;
import network.bane.support.contract.TestMessageSender;

import java.math.BigDecimal;
import java.math.BigInteger;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static java.util.Arrays.asList;
import static network.bane.support.TestConstants.governor;
import static network.bane.support.TestConstants.governorScriptHash;
import static network.bane.support.TestConstants.owner;
import static network.bane.support.TestConstants.ownerScriptHash;
import static network.bane.support.TestConstants.prepareManagementDeployParameter;
import static network.bane.support.TestConstants.relayerScriptHash;
import static network.bane.support.TestConstants.securityGuardScriptHash;
import static network.bane.support.TestConstants.validator1PubKey;
import static network.bane.support.TestConstants.validator2PubKey;
import static network.bane.support.TestConstants.validator3PubKey;
import static network.bane.support.TestConstants.validator4PubKey;
import static network.bane.support.TestConstants.validator5PubKey;
import static network.bane.support.TestConstants.validator6PubKey;
import static network.bane.support.TestConstants.validator7PubKey;
import static network.bane.support.TestConstants.DEFAULT_LINKED_CHAIN_ID;
import static network.bane.support.TestConstants.EXECUTION_MANAGER_CONTRACT_HASH;
import static network.bane.support.TestConstants.MANAGEMENT_CONTRACT_HASH;
import static network.bane.support.TestConstants.MESSAGE_BRIDGE_CONTRACT_HASH;
import static network.bane.support.NetworkSettingsHelper.updateNetworkSettings;

public class TestEnvironment {

    public static final BigInteger nativeDepositFee = new BigInteger("10000000");
    public static final BigInteger minNativeDeposit = new BigInteger("100000000");
    public static final BigInteger maxNativeDeposit = new BigInteger("1000000000000");
    public static final int maxNativeWithdrawals = 100;
    public static final BigInteger maxTotalNativeDepositAmount = new BigInteger("10000000000000");

    public static BridgeTestClient bridge;
    public static ManagementTestClient management;
    public static MessageBridgeTestClient messageBridge;
    public static ExecutionManager executionManager;
    public static MessageTestStorer messageTestStorer;
    public static TestMessageSender testMessageSender;

    public static Hash160 testContract;
    public static final Hash160 neoXNeoTokenHash = new Hash160("0x5615dEB798BB3E4dFa0139dFa1b3D433Cc23b72f");
    public static final Hash160 neoN3NeoTokenHash = NeoToken.SCRIPT_HASH;

    public static Neow3j neow3j;
    public static GasToken gasToken;
    public static NeoToken neoToken;
    public static PolicyContract policyContract;

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
        alice = ext.getAccount(TestConstants.ALICE);
        committee = Account.createMultiSigAccount(asList(alice.getECKeyPair().getPublicKey()), 1);
        bob = ext.getAccount(TestConstants.BOB);
        charlie = ext.getAccount(TestConstants.CHARLIE);
        denise = ext.getAccount(TestConstants.DENISE);
        eve = ext.getAccount(TestConstants.EVE);
        florian = ext.getAccount(TestConstants.FLORIAN);
        gabriel = ext.getAccount(TestConstants.GABRIEL);
        henry = ext.getAccount(TestConstants.HENRY);
        isabella = ext.getAccount(TestConstants.ISABELLA);

        updateNetworkSettings(neow3j, committee, alice);
    }

    public static void setupManagement(ContractTestExtension ext) {
        management = new ManagementTestClient(ext.getDeployedContract(BridgeManagementContract.class).getScriptHash(), neow3j);
    }

    public static void setupBridge(ContractTestExtension ext) {
        setupManagement(ext);
        bridge = new BridgeTestClient(ext.getDeployedContract(BridgeContract.class).getScriptHash(), neow3j);
        setupTestContract(ext);
    }

    public static void setupTestContract(ContractTestExtension ext) {
        testContract = ext.getDeployedContract(TestContract.class).getScriptHash();
    }

    public static void setupTestMessageSender(ContractTestExtension ext) throws Throwable {
        if (messageBridge == null) {
            throw new IllegalStateException("MessageBridge must be set up before TestMessageSender.");
        }
        testMessageSender =
                new TestMessageSender(ext.getDeployedContract(TestMessageSenderContract.class).getScriptHash(), neow3j);
        testMessageSender.setMessageBridge(messageBridge.getScriptHash());
    }

    public static void setupMessageBridge(ContractTestExtension ext) {
        setupManagement(ext);
        Hash160 messageBridgeHash = ext.getDeployedContract(MessageBridgeContract.class).getScriptHash();
        messageBridge = new MessageBridgeTestClient(messageBridgeHash, neow3j);
    }

    public static void setupExecutionManager(ContractTestExtension ext) {
        Hash160 executionManagerHash = ext.getDeployedContract(ExecutionManagerContract.class).getScriptHash();
        executionManager = new ExecutionManager(executionManagerHash, neow3j);
    }

    public static void setupMessageTestStorer(ContractTestExtension ext) {
        Hash160 messageTestStoreHash = ext.getDeployedContract(MessageTestStoreContract.class).getScriptHash();
        messageTestStorer = new MessageTestStorer(messageTestStoreHash, neow3j);
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

    public static void registerNeoTokenBridge() throws Throwable {
        BigInteger fee = gasToken.toFractions(new BigDecimal("0.1"));
        BigInteger minAmount = BigInteger.ONE;
        BigInteger maxAmount = new BigInteger("1000");
        int maxWithdrawals = 100;
        int decimalScalingFactor = 0;
        TokenBridge.TokenConfig config = new TokenBridge.TokenConfig(neoXNeoTokenHash, fee, minAmount, maxAmount,
                maxWithdrawals, decimalScalingFactor);

        bridge.registerToken(neoN3NeoTokenHash, config)
                .withSigners(calledByEntry(governor))
                .signSendAndAwait();
        bridge.unpauseTokenBridge(neoN3NeoTokenHash)
                .withSigners(calledByEntry(governor))
                .signSendAndAwait();
    }

}
