package network.bane;

import io.neow3j.crypto.ECKeyPair.ECPublicKey;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.InvocationResult;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.protocol.core.response.Notification;
import io.neow3j.protocol.core.stackitem.ArrayStackItem;
import io.neow3j.protocol.core.stackitem.ByteStringStackItem;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.test.ContractTest;
import io.neow3j.test.ContractTestExtension;
import io.neow3j.test.DeployConfig;
import io.neow3j.test.DeployConfiguration;
import io.neow3j.transaction.Transaction;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.types.ContractParameter;
import io.neow3j.wallet.Account;
import network.bane.util.Management;
import network.bane.util.TestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.types.ContractParameter.publicKey;
import static io.neow3j.types.StackItemType.ARRAY;
import static io.neow3j.types.StackItemType.BYTE_STRING;
import static io.neow3j.types.StackItemType.INTEGER;
import static java.util.Arrays.asList;
import static network.bane.util.TestHelper.defaultValidatorThreshold;
import static network.bane.util.TestHelper.defaultValidators;
import static network.bane.util.TestHelper.governorPubKey;
import static network.bane.util.TestHelper.owner;
import static network.bane.util.TestHelper.ownerPubKey;
import static network.bane.util.TestHelper.prepareManagementDeployParameter;
import static network.bane.util.TestHelper.relayerPubKey;
import static network.bane.util.TestHelper.setDefaultValidators;
import static network.bane.util.TestHelper.validator1PubKey;
import static network.bane.util.TestHelper.validator2PubKey;
import static network.bane.util.TestHelper.validator3PubKey;
import static network.bane.util.TestHelper.validator4PubKey;
import static network.bane.util.TestHelper.validator5PubKey;
import static network.bane.util.TestHelper.validator6PubKey;
import static network.bane.util.TestHelper.validator7PubKey;
import static network.bane.util.TestHelper.waitUntilTransactionIsExecuted;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ContractTest(blockTime = 1, contracts = BridgeManagementContract.class, batchFile = "setup.batch")
public class BridgeManagementTest {

    private static Management management;
    private static Neow3j neow3j;

    private static Account alice;
    private static ECPublicKey alicePubKey;
    private static Account bob;
    private static ECPublicKey bobPubKey;
    private static Account charlie;
    private static ECPublicKey charliePubKey;
    private static Account denise;
    private static ECPublicKey denisePubKey;
    private static Account eve;
    private static ECPublicKey evePubKey;
    private static Account florian;
    private static ECPublicKey florianPubKey;
    private static Account gabriel;
    private static ECPublicKey gabrielPubKey;
    private static Account henry;
    private static ECPublicKey henryPubKey;
    private static Account isabella;
    private static ECPublicKey isabellaPubKey;

    @RegisterExtension
    public static final ContractTestExtension ext = new ContractTestExtension();

    @BeforeAll
    public static void setUp() throws Exception {
        neow3j = ext.getNeow3j();

        management = new Management(ext.getDeployedContract(BridgeManagementContract.class).getScriptHash(), neow3j);

        alice = ext.getAccount(TestHelper.ALICE);
        bob = ext.getAccount(TestHelper.BOB);
        charlie = ext.getAccount(TestHelper.CHARLIE);
        denise = ext.getAccount(TestHelper.DENISE);
        eve = ext.getAccount(TestHelper.EVE);
        florian = ext.getAccount(TestHelper.FLORIAN);
        gabriel = ext.getAccount(TestHelper.GABRIEL);
        henry = ext.getAccount(TestHelper.HENRY);
        isabella = ext.getAccount(TestHelper.ISABELLA);

        alicePubKey = alice.getECKeyPair().getPublicKey();
        bobPubKey = bob.getECKeyPair().getPublicKey();
        charliePubKey = charlie.getECKeyPair().getPublicKey();
        denisePubKey = denise.getECKeyPair().getPublicKey();
        evePubKey = eve.getECKeyPair().getPublicKey();
        florianPubKey = florian.getECKeyPair().getPublicKey();
        gabrielPubKey = gabriel.getECKeyPair().getPublicKey();
        henryPubKey = henry.getECKeyPair().getPublicKey();
        isabellaPubKey = isabella.getECKeyPair().getPublicKey();
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
                        5,
                        governorPubKey
                )
        );
        return config;
    }

    // region manifest

    @Test
    public void testManifestMethods() throws IOException {
        assertThat(management.getManifest().getName(), is("BridgeManagement"));
        assertThat(management.getManifest().getAbi().getMethods(), hasSize(11));
        assertThat(management.getManifest().getAbi().getEvents(), hasSize(4));
        assertThat(management.getManifest().getSupportedStandards(), hasSize(0));
        assertThat(management.getManifest().getPermissions(), hasSize(0));
        assertThat(management.getManifest().getTrusts(), hasSize(0));
        assertThat(management.getManifest().getGroups(), hasSize(0));
    }

    // endregion
    // region deployment

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
    // region setters

    @Test
    public void testSetOwner() throws Throwable {
        assertThat(management.owner(), is(ownerPubKey));

        Transaction tx = management.invokeFunction("setOwner", publicKey(alicePubKey))
                .signers(calledByEntry(owner))
                .sign();
        NeoSendRawTransaction response = tx.send();
        assertFalse(response.hasError());
        waitUntilTransactionIsExecuted(response, neow3j);

        Notification expected = new Notification(
                management.getScriptHash(),
                "SetOwner",
                new ArrayStackItem(asList(new ByteStringStackItem(alicePubKey.toArray())))
        );
        assertThat(tx.getApplicationLog().getFirstExecution().getNotifications(), hasSize(1));
        assertThat(tx.getApplicationLog().getFirstExecution().getFirstNotification(), is(expected));

        assertThat(management.owner(), is(alicePubKey));

        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("setOwner", publicKey(bobPubKey))
                        .signers(calledByEntry(owner))
                        .sign()
        );
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No authorization."));

        // reverse set owner
        response = management.invokeFunction("setOwner", publicKey(ownerPubKey))
                .signers(calledByEntry(alice))
                .sign()
                .send();
        assertFalse(response.hasError());
        waitUntilTransactionIsExecuted(response, neow3j);

        assertThat(management.owner(), is(ownerPubKey));
    }

    @Test
    public void testSetOwner_unauthorized() throws IOException {
        assertThat(management.owner(), is(ownerPubKey));
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("setOwner", publicKey(ownerPubKey))
                        .signers(calledByEntry(alice))
                        .sign()
        );
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No authorization."));
    }

    @Test
    public void testSetRelayer() throws Throwable {
        assertThat(management.relayer(), is(relayerPubKey));

        Transaction tx = management.invokeFunction("setRelayer", publicKey(bobPubKey))
                .signers(calledByEntry(owner))
                .sign();
        NeoSendRawTransaction response = tx.send();
        assertFalse(response.hasError());
        waitUntilTransactionIsExecuted(response, neow3j);

        Notification expected = new Notification(
                management.getScriptHash(),
                "SetRelayer",
                new ArrayStackItem(asList(new ByteStringStackItem(bobPubKey.toArray())))
        );
        assertThat(tx.getApplicationLog().getFirstExecution().getNotifications(), hasSize(1));
        assertThat(tx.getApplicationLog().getFirstExecution().getFirstNotification(), is(expected));

        assertThat(management.relayer(), is(bobPubKey));

        // reverse set owner
        response = management.invokeFunction("setRelayer", publicKey(relayerPubKey))
                .signers(calledByEntry(owner))
                .sign()
                .send();
        assertFalse(response.hasError());
        waitUntilTransactionIsExecuted(response, neow3j);

        assertThat(management.relayer(), is(relayerPubKey));
    }

    @Test
    public void testSetRelayer_unauthorized() throws IOException {
        assertThat(management.relayer(), is(relayerPubKey));
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("setRelayer", publicKey(charliePubKey))
                        .signers(calledByEntry(alice))
                        .sign()
        );
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No authorization."));
    }

    @Test
    public void testSetValidators() throws Throwable {
        assertThat(management.validators(), hasSize(defaultValidators.size()));
        assertThat(management.validators(), containsInAnyOrder(defaultValidators.toArray()));
        assertThat(management.validatorThreshold(), is(defaultValidatorThreshold));

        Transaction tx = management.invokeFunction("setValidators",
                        array(
                                publicKey(alicePubKey),
                                publicKey(bobPubKey),
                                publicKey(charliePubKey),
                                publicKey(denisePubKey),
                                publicKey(evePubKey)
                        ),
                        integer(3)
                )
                .signers(calledByEntry(owner))
                .sign();
        NeoSendRawTransaction response = tx.send();
        assertFalse(response.hasError());
        waitUntilTransactionIsExecuted(response, neow3j);

        assertThat(tx.getApplicationLog().getFirstExecution().getNotifications(), hasSize(1));
        Notification notification = tx.getApplicationLog().getFirstExecution().getFirstNotification();
        assertThat(notification.getEventName(), is("SetValidators"));
        assertThat(notification.getContract(), is(management.getScriptHash()));
        List<StackItem> stateList = notification.getState().getList();
        assertThat(stateList, hasSize(2));
        assertThat(stateList.get(0).getType(), is(ARRAY));
        assertThat(stateList.get(0).getList(), hasSize(5));
        assertThat(stateList.get(0).getList().stream().map(StackItem::getHexString).collect(Collectors.toList()),
                contains(
                        alicePubKey.getEncodedCompressedHex(),
                        bobPubKey.getEncodedCompressedHex(),
                        charliePubKey.getEncodedCompressedHex(),
                        denisePubKey.getEncodedCompressedHex(),
                        evePubKey.getEncodedCompressedHex()
                ));
        assertThat(stateList.get(1).getType(), is(INTEGER));
        assertThat(stateList.get(1).getInteger().intValue(), is(3));

        assertThat(management.validators(),
                containsInAnyOrder(alicePubKey, bobPubKey, charliePubKey, denisePubKey, evePubKey));
        assertThat(management.validatorThreshold(), is(3));

        setDefaultValidators(management, neow3j);
        assertThat(management.validators(), hasSize(defaultValidators.size()));
        assertThat(management.validators(), containsInAnyOrder(defaultValidators.toArray()));
        assertThat(management.validatorThreshold(), is(defaultValidatorThreshold));
    }

    @Test
    public void testSetValidators_unauthorized() {
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("setValidators", array(publicKey(alicePubKey)), integer(1))
                        .signers(calledByEntry(alice))
                        .sign()
        );
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No authorization."));
    }

    @Test
    public void testSetValidators_invalidThreshold() {
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("setValidators",
                                array(publicKey(charliePubKey), publicKey(denisePubKey)), integer(1))
                        .signers(calledByEntry(alice))
                        .sign()
        );
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No authorization."));
    }

    @Test
    public void testSetValidators_moreThanMax() throws IOException {
        List<ContractParameter> newValidators = new ArrayList<>();
        for (int i = 0; i <= 21; i++) {
            newValidators.add(publicKey(Account.create().getECKeyPair().getPublicKey()));
        }
        assertThat(newValidators, hasSize(22));

        assertThat(management.validators(), hasSize(defaultValidators.size()));
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("setValidators", array(newValidators), integer(5))
                        .signers(calledByEntry(alice))
                        .sign()
        );
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No authorization."));
    }

    @Test
    public void testSetValidators_exactlyMax() throws Throwable {
        List<ContractParameter> newValidators = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            newValidators.add(publicKey(Account.create().getECKeyPair().getPublicKey()));
        }
        assertThat(newValidators, hasSize(21));

        assertThat(management.validators(), hasSize(7));
        NeoSendRawTransaction response = management.invokeFunction("setValidators", array(newValidators), integer(10))
                .signers(calledByEntry(owner)).sign().send();
        assertFalse(response.hasError());
        waitUntilTransactionIsExecuted(response, neow3j);
        assertThat(management.validators(), hasSize(21));
        assertThat(management.validatorThreshold(), is(10));

        // reverse set validators
        setDefaultValidators(management, neow3j);
        assertThat(management.validators(), hasSize(defaultValidators.size()));
        assertThat(management.validators(), containsInAnyOrder(defaultValidators.toArray()));
        assertThat(management.validatorThreshold(), is(defaultValidatorThreshold));
    }

    @Test
    public void testSetValidators_sameValidatorMultipleTimesInParams() throws Throwable {
        assertThat(management.validators(), hasSize(defaultValidators.size()));
        assertThat(management.validators(), containsInAnyOrder(defaultValidators.toArray()));
        assertThat(management.validatorThreshold(), is(defaultValidatorThreshold));

        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("setValidators",
                                array(
                                        publicKey(alicePubKey),
                                        publicKey(bobPubKey),
                                        publicKey(charliePubKey),
                                        publicKey(alicePubKey)
                                ),
                                integer(3))
                        .signers(calledByEntry(owner))
                        .sign()
        );
        assertThat(thrown.getMessage(),
                containsString("ASSERTMSG is executed with false result. Reason: Duplicate validators provided."));
    }

    @Test
    public void testSetGovernor() throws Throwable {
        assertThat(management.governor(), is(governorPubKey));

        Transaction tx = management.invokeFunction("setGovernor", publicKey(bobPubKey))
                .signers(calledByEntry(owner))
                .sign();
        NeoSendRawTransaction response = tx.send();
        assertFalse(response.hasError());
        waitUntilTransactionIsExecuted(response, neow3j);

        Notification expected = new Notification(
                management.getScriptHash(),
                "SetGovernor",
                new ArrayStackItem(asList(new ByteStringStackItem(bobPubKey.toArray())))
        );
        assertThat(tx.getApplicationLog().getFirstExecution().getNotifications(), hasSize(1));
        assertThat(tx.getApplicationLog().getFirstExecution().getFirstNotification(), is(expected));

        assertThat(management.governor(), is(bobPubKey));

        // reverse set owner
        response = management.invokeFunction("setGovernor", publicKey(governorPubKey))
                .signers(calledByEntry(owner))
                .sign()
                .send();
        assertFalse(response.hasError());
        waitUntilTransactionIsExecuted(response, neow3j);

        assertThat(management.governor(), is(governorPubKey));
    }

    @Test
    public void testSetGovernor_unauthorized() throws IOException {
        assertThat(management.governor(), is(governorPubKey));
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("setGovernor", publicKey(charliePubKey))
                        .signers(calledByEntry(alice))
                        .sign()
        );
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No authorization."));
    }

    // endregion

}
