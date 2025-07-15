package network.bane.bridge;

import io.neow3j.protocol.core.response.Notification;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.test.ContractTest;
import io.neow3j.test.ContractTestExtension;
import io.neow3j.test.DeployConfig;
import io.neow3j.test.DeployConfiguration;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.types.StackItemType;
import io.neow3j.wallet.Account;
import network.bane.management.BridgeManagementContract;
import network.bane.testhelper.TestContract;
import network.bane.util.structs.MessageBridgeDto;
import network.bane.util.structs.State;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigInteger;

import static network.bane.util.TestHelper.account0;
import static network.bane.util.TestHelper.governor;
import static network.bane.util.TestHelper.securityGuard;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_EXECUTION_MANAGER_SCRIPT_HASH;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_MSG_MAX_BYTES_FOR_SENDING;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_MSG_NR_MSGS_PER_STORING_INVOCATION;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_MSG_SENDING_FEE;
import static network.bane.util.helper.TestHelper.bridge;
import static network.bane.util.helper.TestHelper.createBridgeDeployConfig;
import static network.bane.util.helper.TestHelper.createBridgeManagementDeployConfig;
import static network.bane.util.helper.TestHelper.setup;
import static network.bane.util.helper.TestHelper.setupBridge;
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
public class MessageBridgeSetTest {

    @RegisterExtension
    public static final ContractTestExtension ext = new ContractTestExtension();

    @BeforeAll
    public static void setUp() throws Throwable {
        setup(ext);
        setupBridge(ext);
    }

    @DeployConfig(BridgeManagementContract.class)
    public static DeployConfiguration deployConfigManagement() {
        return createBridgeManagementDeployConfig();
    }

    @DeployConfig(BridgeContract.class)
    public static DeployConfiguration deployConfigBridge() {
        return createBridgeDeployConfig();
    }

    @Test
    @Order(0)
    public void test_setMessageBridge_notGovernor() {
        Account notGovernor = securityGuard;
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.setMessageBridge(notGovernor, DEFAULT_EXECUTION_MANAGER_SCRIPT_HASH,
                        DEFAULT_MSG_SENDING_FEE, DEFAULT_MSG_MAX_BYTES_FOR_SENDING,
                        DEFAULT_MSG_NR_MSGS_PER_STORING_INVOCATION));
        assertThat(thrown.getMessage(), containsString("No authorization - only governor"));
    }

    @Test
    @Order(0)
    public void test_setMessageBridge_execManagerNotContract() {
        Hash160 eoa = account0.getScriptHash();
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.setMessageBridge(governor, eoa, DEFAULT_MSG_SENDING_FEE,
                        DEFAULT_MSG_MAX_BYTES_FOR_SENDING, DEFAULT_MSG_NR_MSGS_PER_STORING_INVOCATION));
        assertThat(thrown.getMessage(), containsString("ExecutionManager must be a contract"));
    }

    @Test
    @Order(0)
    public void test_setMessageBridge_invalidConfig() {
        BigInteger invalidFee = new BigInteger("-1");
        int invalidMaxMsgBytes = 0;
        int invalidMaxNrMsgs = 0;

        String invalidMsgBridgeConfigAbortMsg = "Invalid message bridge configuration";

        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.setMessageBridge(governor, DEFAULT_EXECUTION_MANAGER_SCRIPT_HASH, invalidFee,
                        DEFAULT_MSG_MAX_BYTES_FOR_SENDING, DEFAULT_MSG_NR_MSGS_PER_STORING_INVOCATION));
        assertThat(thrown.getMessage(), containsString(invalidMsgBridgeConfigAbortMsg));

        thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.setMessageBridge(governor, DEFAULT_EXECUTION_MANAGER_SCRIPT_HASH, DEFAULT_MSG_SENDING_FEE,
                        invalidMaxMsgBytes, DEFAULT_MSG_NR_MSGS_PER_STORING_INVOCATION));
        assertThat(thrown.getMessage(), containsString(invalidMsgBridgeConfigAbortMsg));

        thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.setMessageBridge(governor, DEFAULT_EXECUTION_MANAGER_SCRIPT_HASH, DEFAULT_MSG_SENDING_FEE,
                        DEFAULT_MSG_MAX_BYTES_FOR_SENDING, invalidMaxNrMsgs));
        assertThat(thrown.getMessage(), containsString(invalidMsgBridgeConfigAbortMsg));
    }

    @Test
    @Order(1)
    public void test_setMessageBridge() throws Throwable {
        Hash256 bridgeMsgSetTx = bridge.setMessageBridge(governor, DEFAULT_EXECUTION_MANAGER_SCRIPT_HASH,
                DEFAULT_MSG_SENDING_FEE, DEFAULT_MSG_MAX_BYTES_FOR_SENDING,
                DEFAULT_MSG_NR_MSGS_PER_STORING_INVOCATION);

        Notification setEvent = ext.getNeow3j().getApplicationLog(bridgeMsgSetTx).send().getApplicationLog()
                .getFirstExecution().getFirstNotification();
        assertThat(setEvent.getEventName(), is("MessageBridgeSet"));
        assertThat(setEvent.getContract(), is(bridge.getScriptHash()));
        StackItem state = setEvent.getState();
        assertThat(state.getType(), is(StackItemType.ARRAY));
        assertThat(state.getList(), hasSize(0));

        MessageBridgeDto messageBridge = bridge.getMessageBridge();
        State initState = new State(BigInteger.ZERO, Hash256.ZERO);
        MessageBridgeDto.MessageConfig messageConfig = new MessageBridgeDto.MessageConfig(DEFAULT_MSG_SENDING_FEE,
                DEFAULT_MSG_MAX_BYTES_FOR_SENDING, DEFAULT_MSG_NR_MSGS_PER_STORING_INVOCATION);
        MessageBridgeDto expectedMsgBridge = new MessageBridgeDto(true, initState, initState, messageConfig);

        assertThat(messageBridge, is(expectedMsgBridge));
        assertThat(bridge.messageExecutionManager(), is(DEFAULT_EXECUTION_MANAGER_SCRIPT_HASH));

        // Unpause the message bridge for further tests
        bridge.unpauseMessageBridge();
    }

    @Test
    @Order(2)
    public void test_setMessageBridge_alreadySet() {
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> bridge.setDefaultMessageBridge());
        assertThat(thrown.getMessage(), containsString("Message bridge already set"));
    }

}
