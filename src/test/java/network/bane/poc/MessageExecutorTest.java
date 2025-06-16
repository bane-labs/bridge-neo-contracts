package network.bane.poc;

import io.neow3j.protocol.core.response.NeoApplicationLog;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.test.ContractTest;
import io.neow3j.test.ContractTestExtension;
import io.neow3j.test.DeployConfig;
import io.neow3j.test.DeployConfiguration;
import io.neow3j.types.CallFlags;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.types.StackItemType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigInteger;
import java.util.List;

import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.types.ContractParameter.string;
import static java.util.Arrays.asList;
import static network.bane.util.TestHelper.owner;
import static network.bane.util.helper.TestHelper.neow3j;
import static network.bane.util.helper.TestHelper.setup;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;

@ContractTest(blockTime = 1, contracts = {MessageExecutor.class, SimpleStore.class}, batchFile = "setup.batch")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MessageExecutorTest {

    private static network.bane.util.MessageExecutor messageExecutor;
    private static network.bane.util.SimpleStore simpleStoreContract;

    @RegisterExtension
    public static final ContractTestExtension ext = new ContractTestExtension();

    @BeforeAll
    public static void setUp() throws Throwable {
        setup(ext);
        messageExecutor = new network.bane.util.MessageExecutor(
                ext.getDeployedContract(MessageExecutor.class).getScriptHash(), ext.getNeow3j());
        simpleStoreContract = new network.bane.util.SimpleStore(
                ext.getDeployedContract(SimpleStore.class).getScriptHash(), ext.getNeow3j());
    }

    @DeployConfig(MessageExecutor.class)
    public static DeployConfiguration deployConfigMessageExecutor() {
        return new DeployConfiguration();
    }

    @DeployConfig(SimpleStore.class)
    public static DeployConfiguration deplosSimpleStoreConfig() {
        return new DeployConfiguration();
    }

    // region tests

    @Test
    public void storeAndExecuteFunctionCall() throws Throwable {
        Hash160 contract = simpleStoreContract.getScriptHash();
        String function = "storeEmitAndReturnSuccess";
        CallFlags callFlags = CallFlags.ALL;
        BigInteger key = BigInteger.ONE;
        String value = "hello world";
        List<ContractParameter> params = asList(integer(key), string(value));

        byte[] message = messageExecutor.getMessageSerialized(contract, function, callFlags, params).getFirstStackItem()
                .getByteArray();

        // Store the message in the MessageExecutor contract
        Hash256 storeTx = messageExecutor.storeMessage(owner, message);
        NeoApplicationLog.Execution storeExec = neow3j.getApplicationLog(storeTx).send().getApplicationLog()
                .getFirstExecution();
        List<StackItem> storeNotificationState = storeExec.getFirstNotification().getState().getList();
        // The first item in the notification state is the message Id.
        BigInteger messageId = storeNotificationState.get(0).getInteger();

        // Assert that there's no value stored in the simple store contract yet
        StackItem simpleStoreEntryBefore = simpleStoreContract.get(key);
        assertThat(simpleStoreEntryBefore.getType(), is(StackItemType.ANY));
        assertNull(simpleStoreEntryBefore.getValue());

        Hash256 executeTx = messageExecutor.executeMessage(owner, messageId);
        NeoApplicationLog.Execution exec = neow3j.getApplicationLog(executeTx).send().getApplicationLog()
                .getFirstExecution();
        assertThat(exec.getNotifications(), hasSize(3));
        assertThat(exec.getNotifications().get(0).getEventName(), is("MessageExecute"));
        assertThat(exec.getNotifications().get(1).getEventName(), is("Store"));
        assertThat(exec.getNotifications().get(2).getEventName(), is("MessageExecuteReturn"));

        // Assert that there's now a value stored in the simple store contract
        StackItem simpleStoreEntryAfter = simpleStoreContract.callInvokeFunction("get", asList(integer(key)))
                .getInvocationResult().getFirstStackItem();
        assertThat(simpleStoreEntryAfter.getType(), is(StackItemType.BYTE_STRING));
        assertThat(simpleStoreEntryAfter.getString(), is(value));
    }

    // endregion

}
