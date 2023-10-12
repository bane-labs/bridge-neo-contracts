package network.bane;

import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.InvocationResult;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.test.ContractTest;
import io.neow3j.test.ContractTestExtension;
import io.neow3j.test.DeployConfig;
import io.neow3j.test.DeployConfiguration;
import io.neow3j.wallet.Account;
import network.bane.util.TestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static io.neow3j.types.StackItemType.ARRAY;
import static io.neow3j.types.StackItemType.BYTE_STRING;
import static io.neow3j.types.StackItemType.INTEGER;
import static java.util.Arrays.asList;
import static network.bane.util.TestHelper.ownerPubKey;
import static network.bane.util.TestHelper.prepareManagementDeployParameter;
import static network.bane.util.TestHelper.relayerPubKey;
import static network.bane.util.TestHelper.validator1PubKey;
import static network.bane.util.TestHelper.validator2PubKey;
import static network.bane.util.TestHelper.validator3PubKey;
import static network.bane.util.TestHelper.validator4PubKey;
import static network.bane.util.TestHelper.validator5PubKey;
import static network.bane.util.TestHelper.validator6PubKey;
import static network.bane.util.TestHelper.validator7PubKey;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ContractTest(blockTime = 1, contracts = BridgeManagementContract.class, batchFile = "setup.batch")
public class BridgeManagementTest {

    private static SmartContract management;
    private static Neow3j neow3j;

    private static Account alice;

    @RegisterExtension
    public static final ContractTestExtension ext = new ContractTestExtension();

    @BeforeAll
    public static void setUp() throws Exception {
        management = ext.getDeployedContract(BridgeManagementContract.class);
        neow3j = ext.getNeow3j();

        alice = ext.getAccount(TestHelper.ALICE);
    }

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

    // region test deployment

    @Test
    public void testDeployOwnerCorrect() throws IOException {
        InvocationResult result = management.callInvokeFunction("owner").getInvocationResult();

        assertThat(result.getStack(), hasSize(1));
        assertThat(result.getFirstStackItem().getType(), is(BYTE_STRING));
        assertThat(result.getFirstStackItem().getByteArray(), is(ownerPubKey.toArray()));
    }

    @Test
    public void testDeployRelayerCorrect() throws IOException {
        InvocationResult result = management.callInvokeFunction("relayer").getInvocationResult();

        assertThat(result.getStack(), hasSize(1));
        assertThat(result.getFirstStackItem().getType(), is(BYTE_STRING));
        assertThat(result.getFirstStackItem().getByteArray(), is(relayerPubKey.toArray()));
    }

    @Test
    public void testDeployValidatorThresholdCorrect() throws IOException {
        InvocationResult result = management.callInvokeFunction("validatorThreshold").getInvocationResult();

        assertThat(result.getStack(), hasSize(1));
        assertThat(result.getFirstStackItem().getType(), is(INTEGER));
        assertThat(result.getFirstStackItem().getInteger().intValue(), is(5));
    }

    @Test
    public void testDeployValidatorsCorrect() throws IOException {
        InvocationResult result = management.callInvokeFunction("validators").getInvocationResult();

        assertThat(result.getStack(), hasSize(1));
        assertThat(result.getFirstStackItem().getType(), is(ARRAY));

        List<StackItem> stackList = result.getFirstStackItem().getList();
        assertThat(stackList, hasSize(7));

        List<String> validatorList = stackList.stream().map(StackItem::getHexString).collect(Collectors.toList());
        assertTrue(validatorList.contains(validator1PubKey.getEncodedCompressedHex()));
        assertTrue(validatorList.contains(validator2PubKey.getEncodedCompressedHex()));
        assertTrue(validatorList.contains(validator3PubKey.getEncodedCompressedHex()));
        assertTrue(validatorList.contains(validator4PubKey.getEncodedCompressedHex()));
        assertTrue(validatorList.contains(validator5PubKey.getEncodedCompressedHex()));
        assertTrue(validatorList.contains(validator6PubKey.getEncodedCompressedHex()));
        assertTrue(validatorList.contains(validator7PubKey.getEncodedCompressedHex()));
    }

    // endregion

}
