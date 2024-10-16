package network.bane.management;

import io.neow3j.contract.ContractManagement;
import io.neow3j.contract.NefFile;
import io.neow3j.crypto.ECKeyPair.ECPublicKey;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.ObjectMapperFactory;
import io.neow3j.protocol.core.response.ContractManifest;
import io.neow3j.protocol.core.response.InvocationResult;
import io.neow3j.protocol.core.response.NeoApplicationLog;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.protocol.core.response.Notification;
import io.neow3j.protocol.core.stackitem.ArrayStackItem;
import io.neow3j.protocol.core.stackitem.ByteStringStackItem;
import io.neow3j.protocol.core.stackitem.IntegerStackItem;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.test.ContractTest;
import io.neow3j.test.ContractTestExtension;
import io.neow3j.test.DeployConfig;
import io.neow3j.test.DeployConfiguration;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.Transaction;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.utils.Await;
import io.neow3j.wallet.Account;
import network.bane.util.Management;
import network.bane.util.TestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.transaction.AccountSigner.none;
import static io.neow3j.types.ContractParameter.any;
import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.bool;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.types.ContractParameter.byteArrayFromString;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.types.ContractParameter.publicKey;
import static io.neow3j.types.ContractParameter.string;
import static io.neow3j.types.StackItemType.ARRAY;
import static io.neow3j.types.StackItemType.BYTE_STRING;
import static io.neow3j.types.StackItemType.INTEGER;
import static io.neow3j.utils.Numeric.prependHexPrefix;
import static java.util.Arrays.asList;
import static network.bane.util.TestHelper.defaultValidatorThreshold;
import static network.bane.util.TestHelper.defaultValidators;
import static network.bane.util.TestHelper.governorScriptHash;
import static network.bane.util.TestHelper.owner;
import static network.bane.util.TestHelper.ownerScriptHash;
import static network.bane.util.TestHelper.relayer;
import static network.bane.util.TestHelper.relayerScriptHash;
import static network.bane.util.TestHelper.securityGuardScriptHash;
import static network.bane.util.TestHelper.setDefaultValidators;
import static network.bane.util.TestHelper.validator1PubKey;
import static network.bane.util.TestHelper.validator2PubKey;
import static network.bane.util.TestHelper.validator3PubKey;
import static network.bane.util.TestHelper.validator4PubKey;
import static network.bane.util.TestHelper.validator5PubKey;
import static network.bane.util.TestHelper.validator6;
import static network.bane.util.TestHelper.validator6PubKey;
import static network.bane.util.TestHelper.validator7;
import static network.bane.util.TestHelper.validator7PubKey;
import static network.bane.util.TestHelper.waitUntilTransactionIsExecuted;
import static network.bane.util.helper.TestHelper.createBridgeManagementDeployConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@ContractTest(blockTime = 1, contracts = BridgeManagementContract.class, batchFile = "setup.batch")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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
        return createBridgeManagementDeployConfig();
    }

    // region manifest

    @Test
    @Order(0)
    public void testManifestMethods() throws IOException {
        assertThat(management.getManifest().getName(), is("NeoXBridgeManagement"));
        assertThat(management.getManifest().getAbi().getMethods(), hasSize(15));
        assertThat(management.getManifest().getAbi().getEvents(), hasSize(5));
        assertThat(management.getManifest().getSupportedStandards(), hasSize(0));
        assertThat(management.getManifest().getPermissions(), hasSize(1));
        assertThat(management.getManifest().getFirstPermission().getContract(),
                is(prependHexPrefix(ContractManagement.SCRIPT_HASH.toString())));
        assertThat(management.getManifest().getFirstPermission().getMethods(), hasSize(1));
        assertThat(management.getManifest().getFirstPermission().getMethod(0), is("update"));
        assertThat(management.getManifest().getTrusts(), hasSize(0));
        assertThat(management.getManifest().getGroups(), hasSize(0));
    }

    // endregion
    // region deployment

    @Test
    @Order(0)
    public void testDeployOwnerCorrect() throws IOException {
        InvocationResult result = management.callInvokeFunction("owner").getInvocationResult();

        assertThat(result.getStack(), hasSize(1));
        assertThat(result.getFirstStackItem().getType(), is(BYTE_STRING));
        assertThat(Hash160.fromAddress(result.getFirstStackItem().getAddress()), is(ownerScriptHash));
    }

    @Test
    @Order(0)
    public void testDeployRelayerCorrect() throws IOException {
        InvocationResult result = management.callInvokeFunction("relayer").getInvocationResult();

        assertThat(result.getStack(), hasSize(1));
        assertThat(result.getFirstStackItem().getType(), is(BYTE_STRING));
        assertThat(Hash160.fromAddress(result.getFirstStackItem().getAddress()), is(relayerScriptHash));
    }

    @Test
    @Order(0)
    public void testDeployValidatorThresholdCorrect() throws IOException {
        InvocationResult result = management.callInvokeFunction("validatorThreshold").getInvocationResult();

        assertThat(result.getStack(), hasSize(1));
        assertThat(result.getFirstStackItem().getType(), is(INTEGER));
        assertThat(result.getFirstStackItem().getInteger().intValue(), is(5));
    }

    @Test
    @Order(0)
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
    // region set owner

    @Test
    @Order(0)
    public void testSetOwner() throws Throwable {
        assertThat(management.owner(), is(ownerScriptHash));

        Transaction tx = management.invokeFunction("setOwner", hash160(alice))
                .signers(calledByEntry(owner), none(alice).setAllowedContracts(management.getScriptHash()))
                .sign();
        NeoSendRawTransaction response = tx.send();
        assertFalse(response.hasError());
        waitUntilTransactionIsExecuted(response, neow3j);

        Notification expected = new Notification(
                management.getScriptHash(),
                "OwnerChange",
                new ArrayStackItem(asList(new ByteStringStackItem(alice.getScriptHash().toLittleEndianArray())))
        );
        assertThat(tx.getApplicationLog().getFirstExecution().getNotifications(), hasSize(1));
        assertThat(tx.getApplicationLog().getFirstExecution().getFirstNotification(), is(expected));

        assertThat(management.owner(), is(alice.getScriptHash()));

        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("setOwner", hash160(bob))
                        .signers(calledByEntry(owner))
                        .sign()
        );
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No authorization."));

        // reverse set owner
        response = management.invokeFunction("setOwner", hash160(ownerScriptHash))
                .signers(calledByEntry(alice), none(owner).setAllowedContracts(management.getScriptHash()))
                .sign()
                .send();
        assertFalse(response.hasError());
        waitUntilTransactionIsExecuted(response, neow3j);

        assertThat(management.owner(), is(ownerScriptHash));
    }

    @Test
    @Order(0)
    public void testSetOwner_InvalidParameters() {
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("setOwner", any(null))
                        .signers(calledByEntry(owner))
                        .sign()
        );
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Invalid script hash provided."));

        thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("setOwner", byteArrayFromString("invalid"))
                        .signers(calledByEntry(owner))
                        .sign()
        );
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Invalid script hash provided."));
    }

    @Test
    @Order(0)
    public void testSetOwner_unauthorized() throws IOException {
        assertThat(management.owner(), is(ownerScriptHash));
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("setOwner", hash160(ownerScriptHash))
                        .signers(calledByEntry(alice))
                        .sign()
        );
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No authorization."));
    }

    // endregion
    // region set relayer

    @Test
    @Order(0)
    public void testSetRelayer() throws Throwable {
        assertThat(management.relayer(), is(relayerScriptHash));

        Transaction tx = management.invokeFunction("setRelayer", hash160(bob))
                .signers(calledByEntry(owner))
                .sign();
        NeoSendRawTransaction response = tx.send();
        assertFalse(response.hasError());
        waitUntilTransactionIsExecuted(response, neow3j);

        Notification expected = new Notification(
                management.getScriptHash(),
                "RelayerChange",
                new ArrayStackItem(asList(new ByteStringStackItem(bob.getScriptHash().toLittleEndianArray())))
        );
        assertThat(tx.getApplicationLog().getFirstExecution().getNotifications(), hasSize(1));
        assertThat(tx.getApplicationLog().getFirstExecution().getFirstNotification(), is(expected));

        assertThat(management.relayer(), is(bob.getScriptHash()));

        // reverse set owner
        response = management.invokeFunction("setRelayer", hash160(relayerScriptHash))
                .signers(calledByEntry(owner))
                .sign()
                .send();
        assertFalse(response.hasError());
        waitUntilTransactionIsExecuted(response, neow3j);

        assertThat(management.relayer(), is(relayerScriptHash));
    }

    @Test
    @Order(0)
    public void testSetRelayer_unauthorized() throws IOException {
        assertThat(management.relayer(), is(relayerScriptHash));
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("setRelayer", hash160(charlie))
                        .signers(calledByEntry(alice))
                        .sign()
        );
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No authorization."));
    }

    // endregion
    // region validator threshold

    @Test
    public void testSetValidatorThreshold() throws Throwable {
        assertThat(management.validatorThreshold(), is(defaultValidatorThreshold));
        Hash256 txHash = management.setValidatorThreshold(owner, 3);
        assertThat(management.validatorThreshold(), is(3));
        Notification expected = new Notification(
                management.getScriptHash(),
                "ValidatorThresholdChange",
                new ArrayStackItem(asList(new IntegerStackItem(BigInteger.valueOf(3))))
        );
        NeoApplicationLog.Execution firstExecution =
                neow3j.getApplicationLog(txHash).send().getApplicationLog().getFirstExecution();
        assertThat(firstExecution.getNotifications(), hasSize(1));
        assertThat(firstExecution.getFirstNotification(), is(expected));

        // reset validator threshold to default
        management.setValidatorThreshold(owner, defaultValidatorThreshold);
        assertThat(management.validatorThreshold(), is(defaultValidatorThreshold));
    }

    @Test
    public void testSetValidatorThreshold_tooLow() throws Throwable {
        // Threshold lower than 2 should not be allowed.
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.setValidatorThreshold(owner, 1));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Threshold too low."));

        // Threshold equal to 2 (minimum) should be allowed.
        management.setValidatorThreshold(owner, 2);
        assertThat(management.validatorThreshold(), is(2));

        // Reset the threshold to the default used in these tests.
        management.setValidatorThreshold(owner, defaultValidatorThreshold);
        assertThat(management.validatorThreshold(), is(defaultValidatorThreshold));
    }

    @Test
    public void testSetValidatorThreshold_tooHigh() throws Throwable {
        int nrValidators = management.validators().size();
        // Threshold higher than the number of validators should not be allowed.
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.setValidatorThreshold(owner, nrValidators + 1));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Threshold too high."));

        // Threshold equal to the number of validators should be allowed.
        management.setValidatorThreshold(owner, nrValidators);
        assertThat(management.validatorThreshold(), is(nrValidators));

        // Reset the threshold to the default used in these tests.
        management.setValidatorThreshold(owner, defaultValidatorThreshold);
        assertThat(management.validatorThreshold(), is(defaultValidatorThreshold));
    }

    @Test
    public void testSetValidatorThreshold_unauthorized() {
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.setValidatorThreshold(relayer, 3));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No authorization."));
    }

    // endregion
    // region validators add

    @Test
    @Order(0)
    public void testValidatorAdd() throws Throwable {
        assertFalse(management.isValidator(florianPubKey));
        assertThat(management.validatorThreshold(), is(5));
        management.addValidator(owner, florianPubKey, false);
        assertTrue(management.callFunctionReturningBool("isValidator", publicKey(florianPubKey)));
        assertThat(management.callFunctionReturningInt("validatorThreshold").intValue(), is(5));

        // reverse validator addition
        management.removeValidator(owner, florianPubKey, false);
    }

    @Test
    @Order(0)
    public void testValidatorAdd_incrementThreshold() throws Throwable {
        assertThat(management.validators().size(), is(7));
        assertFalse(management.isValidator(florianPubKey));
        assertThat(management.validatorThreshold(), is(5));
        management.addValidator(owner, florianPubKey, true);
        assertThat(management.validators().size(), is(8));
        assertTrue(management.isValidator(florianPubKey));
        assertThat(management.validatorThreshold(), is(6));

        // reverse add validator
        management.removeValidator(owner, florianPubKey, true);
        assertThat(management.validators().size(), is(7));
        assertFalse(management.isValidator(florianPubKey));
        assertThat(management.validatorThreshold(), is(5));
    }

    @Test
    @Order(0)
    public void testValidatorAdd_alreadyValidator() throws IOException {
        assertFalse(management.isValidator(validator2PubKey));
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.addValidator(owner, validator2PubKey, false));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Already a validator."));
    }

    @Test
    @Order(0)
    public void testValidatorAdd_failWithInvalidPublicKey() {
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("addValidator", any(null), bool(false))
                        .signers(calledByEntry(owner))
                        .sign());
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Invalid public key."));

        thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("addValidator", byteArrayFromString("hello"), bool(false))
                        .signers(calledByEntry(owner))
                        .sign());
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Invalid public key."));
    }

    @Test
    @Order(0)
    public void testValidatorAdd_unauthorized() {
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.addValidator(relayer, validator6PubKey, false));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No authorization."));
    }

    // endregion
    // region validators remove

    @Test
    @Order(0)
    public void testValidatorRemove() throws Throwable {
        assertTrue(management.isValidator(validator6PubKey));
        assertThat(management.validatorThreshold(), is(5));
        Hash256 txHash = management.removeValidator(owner, validator6PubKey, false);
        assertFalse(management.isValidator(validator6PubKey));
        assertThat(management.validatorThreshold(), is(5));

        NeoApplicationLog.Execution exec = neow3j.getApplicationLog(txHash).send()
                .getApplicationLog().getFirstExecution();
        assertThat(exec.getNotifications(), hasSize(1));
        assertThat(exec.getFirstNotification().getContract(), is(management.getScriptHash()));
        assertThat(exec.getFirstNotification().getEventName(), is("ValidatorRemove"));
        assertThat(exec.getFirstNotification().getState().getList(), hasSize(1));
        assertThat(exec.getFirstNotification().getState().getList().get(0).getAddress(), is(validator6.getAddress()));

        // reverse add validator
        management.addValidator(owner, validator6PubKey, false);
        assertTrue(management.isValidator(validator6PubKey));
        assertThat(management.validatorThreshold(), is(5));
    }

    @Test
    @Order(0)
    public void testValidatorRemove_notAValidator() throws Throwable {
        assertFalse(management.isValidator(florianPubKey));
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.removeValidator(owner, validator6PubKey, false));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Not a validator."));
    }

    @Test
    @Order(0)
    public void testValidatorRemove_decrementThreshold() throws Throwable {
        assertThat(management.validators().size(), is(7));
        assertTrue(management.isValidator(validator6PubKey));
        assertThat(management.validatorThreshold(), is(5));
        management.removeValidator(owner, validator6PubKey, true);
        assertThat(management.validators().size(), is(6));
        assertFalse(management.isValidator(validator6PubKey));
        assertThat(management.validatorThreshold(), is(4));

        // reverse add validator
        management.addValidator(owner, validator6PubKey, true);
        assertThat(management.validators().size(), is(7));
        assertTrue(management.isValidator(validator6PubKey));
        assertThat(management.validatorThreshold(), is(5));
    }

    @Test
    @Order(0)
    public void testValidatorRemove_failToRemoveIfMinimumLimitReached() throws Throwable {
        // If there are only 2 validators left, validators cannot be removed.
        assertThat(management.validators().size(), is(7));
        management.removeValidator(owner, validator7PubKey, true);
        management.removeValidator(owner, validator6PubKey, true);
        management.removeValidator(owner, validator5PubKey, true);
        assertThat(management.validatorThreshold(), is(2));
        assertTrue(management.isValidator(validator4PubKey));

        // Threshold should be 2 now. Removing a validator and decrementing the threshold further should fail.
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.removeValidator(owner, validator4PubKey, true));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Min threshold limit reached."));

        management.removeValidator(owner, validator4PubKey, false);
        management.removeValidator(owner, validator3PubKey, false);

        // Threshold and number of validators should be 2 now. Removing a validator should not be possible.
        TransactionConfigurationException thrown2 = assertThrows(TransactionConfigurationException.class,
                () -> management.removeValidator(owner, validator2PubKey, false));
        assertThat(thrown2.getMessage(),
                containsString("ABORTMSG is executed. Reason: Min number of validators reached."));
    }

    @Test
    @Order(0)
    public void testValidatorRemove_failToRemoveAndDecrementThreshold() throws Throwable {
        // If the threshold is equal to the number of validators, a validator should not be removable with
        // decrementing the threshold as well.
        assertThat(management.validators().size(), is(7));
        management.setValidatorThreshold(owner, 7);
        assertTrue(management.isValidator(validator6PubKey));
        assertThat(management.validatorThreshold(), is(7));

        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.removeValidator(owner, validator6PubKey, true));

        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Threshold too high."));
    }

    @Test
    @Order(0)
    public void testValidatorRemove_failWithInvalidPublicKey() {
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("removeValidator", any(null), bool(false))
                        .signers(calledByEntry(owner))
                        .sign());
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Invalid public key."));

        thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("removeValidator", byteArrayFromString("hello"), bool(false))
                        .signers(calledByEntry(owner))
                        .sign());
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Invalid public key."));
    }

    @Test
    @Order(0)
    public void testValidatorRemove_unauthorized() {
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.removeValidator(relayer, validator6PubKey, false));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No authorization."));
    }

    // endregion
    // region validators replace

    @Test
    @Order(0)
    public void testValidatorReplace() throws Throwable {
        assertTrue(management.isValidator(validator6PubKey));
        assertFalse(management.isValidator(florianPubKey));
        assertThat(management.validatorThreshold(), is(5));
        Hash256 txHash = management.replaceValidator(owner, validator6PubKey, florianPubKey);
        assertFalse(management.isValidator(validator6PubKey));
        assertTrue(management.isValidator(florianPubKey));
        assertThat(management.validatorThreshold(), is(5));

        NeoApplicationLog.Execution exec = neow3j.getApplicationLog(txHash).send()
                .getApplicationLog().getFirstExecution();
        assertThat(exec.getNotifications(), hasSize(1));
        assertThat(exec.getFirstNotification().getContract(), is(management.getScriptHash()));
        assertThat(exec.getFirstNotification().getEventName(), is("ValidatorReplace"));
        assertThat(exec.getFirstNotification().getState().getList(), hasSize(2));
        assertThat(exec.getFirstNotification().getState().getList().get(0).getAddress(), is(validator6.getAddress()));
        assertThat(exec.getFirstNotification().getState().getList().get(1).getAddress(), is(florian.getAddress()));

        // reverse add validator
        management.replaceValidator(owner, florianPubKey, validator6PubKey);
        assertTrue(management.isValidator(validator6PubKey));
        assertFalse(management.isValidator(validator6PubKey));
        assertThat(management.validatorThreshold(), is(5));
    }

    @Test
    @Order(0)
    public void testValidatorReplace_failReplacingWithInvalidPublicKey() {
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("replaceValidator", publicKey(validator6PubKey), any(null))
                        .signers(calledByEntry(owner))
                        .sign());
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Invalid public key."));

        thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("replaceValidator", publicKey(validator6PubKey),
                                byteArrayFromString("hello"))
                        .signers(calledByEntry(owner))
                        .sign());
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Invalid public key."));
    }

    @Test
    @Order(0)
    public void testValidatorReplace_failReplacingWithSamePublicKey() {
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.replaceValidator(relayer, validator6PubKey, validator6PubKey));
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Public keys must be different."));
    }

    @Test
    @Order(0)
    public void testValidatorReplace_unauthorized() {
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.replaceValidator(relayer, validator6PubKey, florianPubKey));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No authorization."));
    }

    // endregion
    // region validators is

    @Test
    @Order(0)
    public void testIsValidator() throws IOException {
        assertTrue(management.isValidator(validator1PubKey));
        assertTrue(management.isValidator(validator2PubKey));
        assertTrue(management.isValidator(validator3PubKey));
        assertTrue(management.isValidator(validator4PubKey));
        assertTrue(management.isValidator(validator5PubKey));
        assertTrue(management.isValidator(validator6PubKey));
        assertTrue(management.isValidator(validator7PubKey));
        assertFalse(management.isValidator(relayer.getECKeyPair().getPublicKey()));
    }

    @Test
    @Order(0)
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
        assertThat(notification.getEventName(), is("ValidatorsChange"));
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
    @Order(0)
    public void testSetValidators_unauthorized() {
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("setValidators", array(publicKey(alicePubKey)), integer(1))
                        .signers(calledByEntry(alice))
                        .sign()
        );
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No authorization."));
    }

    @Test
    @Order(0)
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
    @Order(0)
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
    @Order(0)
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
    @Order(0)
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
                containsString("ABORTMSG is executed. Reason: Duplicate validators provided."));
    }

    // endregion
    // region set governor

    @Test
    @Order(0)
    public void testSetGovernor() throws Throwable {
        assertThat(management.governor(), is(governorScriptHash));

        Transaction tx = management.invokeFunction("setGovernor", hash160(bob))
                .signers(calledByEntry(owner))
                .sign();
        NeoSendRawTransaction response = tx.send();
        assertFalse(response.hasError());
        waitUntilTransactionIsExecuted(response, neow3j);

        Notification expected = new Notification(
                management.getScriptHash(),
                "GovernorChange",
                new ArrayStackItem(asList(new ByteStringStackItem(bob.getScriptHash().toLittleEndianArray())))
        );
        assertThat(tx.getApplicationLog().getFirstExecution().getNotifications(), hasSize(1));
        assertThat(tx.getApplicationLog().getFirstExecution().getFirstNotification(), is(expected));

        assertThat(management.governor(), is(bob.getScriptHash()));

        // reverse set owner
        response = management.invokeFunction("setGovernor", hash160(governorScriptHash))
                .signers(calledByEntry(owner))
                .sign()
                .send();
        assertFalse(response.hasError());
        waitUntilTransactionIsExecuted(response, neow3j);

        assertThat(management.governor(), is(governorScriptHash));
    }

    @Test
    @Order(0)
    public void testSetGovernor_unauthorized() throws IOException {
        assertThat(management.governor(), is(governorScriptHash));
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("setGovernor", publicKey(charliePubKey))
                        .signers(calledByEntry(alice))
                        .sign()
        );
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No authorization."));
    }

    // endregion
    // region set security guard

    @Test
    @Order(0)
    public void testSetSecurityGuard() throws Throwable {
        assertThat(management.securityGuard(), is(securityGuardScriptHash));

        Transaction tx = management.invokeFunction("setSecurityGuard", hash160(florian))
                .signers(calledByEntry(owner))
                .sign();
        NeoSendRawTransaction response = tx.send();
        assertFalse(response.hasError());
        waitUntilTransactionIsExecuted(response, neow3j);

        Notification expected = new Notification(
                management.getScriptHash(),
                "SecurityGuardChange",
                new ArrayStackItem(asList(new ByteStringStackItem(florian.getScriptHash().toLittleEndianArray())))
        );
        assertThat(tx.getApplicationLog().getFirstExecution().getNotifications(), hasSize(1));
        assertThat(tx.getApplicationLog().getFirstExecution().getFirstNotification(), is(expected));

        assertThat(management.securityGuard(), is(florian.getScriptHash()));

        // reverse set owner
        response = management.invokeFunction("setSecurityGuard", hash160(securityGuardScriptHash))
                .signers(calledByEntry(owner))
                .sign()
                .send();
        assertFalse(response.hasError());
        waitUntilTransactionIsExecuted(response, neow3j);

        assertThat(management.securityGuard(), is(securityGuardScriptHash));
    }

    @Test
    @Order(0)
    public void testSetSecurityGuard_unauthorized() throws IOException {
        assertThat(management.securityGuard(), is(securityGuardScriptHash));
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("setSecurityGuard", publicKey(charliePubKey))
                        .signers(calledByEntry(alice))
                        .sign()
        );
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No authorization."));
    }

    // endregion
    // region contract update

    @Test
    @Order(100)
    public void testUpdateContract() throws Throwable {
        File contractNefFile = Paths.get("src", "test", "resources", "DummyBridgeManagement.nef").toFile();
        NefFile nefFile = NefFile.readFromFile(contractNefFile);

        File manifestFile = Paths.get("src", "test", "resources", "DummyBridgeManagement.manifest.json").toFile();
        ContractManifest manifest;
        try (FileInputStream s = new FileInputStream(manifestFile)) {
            manifest = ObjectMapperFactory.getObjectMapper().readValue(s, ContractManifest.class);
        }
        byte[] manifestBytes = ObjectMapperFactory.getObjectMapper().writeValueAsBytes(manifest);

        NeoSendRawTransaction response =
                management.invokeFunction("update", byteArray(nefFile.toArray()), byteArray(manifestBytes), any(null))
                        .signers(AccountSigner.calledByEntry(owner))
                        .sign()
                        .send();
        Await.waitUntilTransactionIsExecuted(response.getSendRawTransaction().getHash(), ext.getNeow3j());

        assertThat(management.getManifest().getAbi().getMethods(), hasSize(1));
        assertThat(management.callFunctionReturningString("sayHello", string("World")), is("Hello World!"));
    }

    @Test
    @Order(0)
    public void testContractUpdate_notOwner() {
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("update", byteArrayFromString(""), string(""), any(null))
                        .signers(calledByEntry(relayer))
                        .sign()
        );
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No authorization."));
    }

    // endregion

}
