package network.bane.management;

import io.neow3j.contract.ContractManagement;
import io.neow3j.contract.NefFile;
import io.neow3j.crypto.ECKeyPair.ECPublicKey;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.ObjectMapperFactory;
import io.neow3j.protocol.core.response.ContractManifest;
import io.neow3j.protocol.core.response.ContractStorageEntry;
import io.neow3j.protocol.core.response.NeoApplicationLog;
import io.neow3j.protocol.core.response.Notification;
import io.neow3j.protocol.core.stackitem.ArrayStackItem;
import io.neow3j.protocol.core.stackitem.ByteStringStackItem;
import io.neow3j.protocol.core.stackitem.IntegerStackItem;
import io.neow3j.test.ContractTest;
import io.neow3j.test.ContractTestExtension;
import io.neow3j.test.DeployConfig;
import io.neow3j.test.DeployConfiguration;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;
import network.bane.client.ManagementTestClient;
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
import java.util.List;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.transaction.AccountSigner.none;
import static io.neow3j.types.ContractParameter.any;
import static io.neow3j.types.ContractParameter.bool;
import static io.neow3j.types.ContractParameter.byteArrayFromString;
import static io.neow3j.types.ContractParameter.publicKey;
import static io.neow3j.types.ContractParameter.string;
import static io.neow3j.utils.Numeric.prependHexPrefix;
import static java.util.Arrays.asList;
import static network.bane.util.TestHelper.defaultValidatorThreshold;
import static network.bane.util.TestHelper.governor;
import static network.bane.util.TestHelper.governorScriptHash;
import static network.bane.util.TestHelper.owner;
import static network.bane.util.TestHelper.ownerScriptHash;
import static network.bane.util.TestHelper.relayer;
import static network.bane.util.TestHelper.relayerScriptHash;
import static network.bane.util.TestHelper.securityGuard;
import static network.bane.util.TestHelper.securityGuardScriptHash;
import static network.bane.util.TestHelper.validator1PubKey;
import static network.bane.util.TestHelper.validator2PubKey;
import static network.bane.util.TestHelper.validator3PubKey;
import static network.bane.util.TestHelper.validator4PubKey;
import static network.bane.util.TestHelper.validator5PubKey;
import static network.bane.util.TestHelper.validator6;
import static network.bane.util.TestHelper.validator6PubKey;
import static network.bane.util.TestHelper.validator7PubKey;
import static network.bane.util.helper.TestHelper.createBridgeManagementDeployConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ContractTest(blockTime = 1, contracts = BridgeManagementContract.class, batchFile = "setup.batch")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BridgeManagementTest {

    private static ManagementTestClient management;
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

        Hash160 managementHash = ext.getDeployedContract(BridgeManagementContract.class).getScriptHash();
        management = new ManagementTestClient(managementHash, neow3j);

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
        assertThat(management.getManifest().getAbi().getMethods(), hasSize(19));
        assertThat(management.getManifest().getAbi().getEvents(), hasSize(8));
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
    public void testDeployment_deploymentDataSetCorrectly() throws IOException {
        assertThat(management.owner(), is(owner.getScriptHash()));
        assertThat(management.relayer(), is(relayer.getScriptHash()));
        assertThat(management.governor(), is(governor.getScriptHash()));
        assertThat(management.securityGuard(), is(securityGuard.getScriptHash()));

        List<ECPublicKey> validators = management.validators();
        assertThat(validators, hasSize(7));
        assertThat(validators, containsInAnyOrder(validator1PubKey, validator2PubKey, validator3PubKey,
                validator4PubKey, validator5PubKey, validator6PubKey, validator7PubKey));
        assertThat(management.validatorThreshold(), is(defaultValidatorThreshold));

        List<ContractStorageEntry> allStorageEntries = management.findStorage("0x");
        assertThat(allStorageEntries, hasSize(13)); // 4 roles, 7 validators, 1 threshold, 1 version

        // Check version storage entry
        List<ContractStorageEntry> nonValidatorStorageEntries = management.findStorage("0x0a");
        assertThat(nonValidatorStorageEntries, hasSize(6));
        assertThat(nonValidatorStorageEntries.get(5).getKeyHex(), is("0x0a7f"));
        assertThat(nonValidatorStorageEntries.get(5).getValueHex(), is("0x04"));
    }

    // endregion
    // region set owner

    @Test
    @Order(0)
    public void testSetOwner() throws Throwable {
        assertThat(management.owner(), is(ownerScriptHash));

        Hash256 txHash = management.setOwner(alice.getScriptHash())
                .withSigners(
                        none(owner).setAllowedContracts(management.getScriptHash()),
                        none(alice).setAllowedContracts(management.getScriptHash())
                ).signSendAndAwait();

        Notification expected = new Notification(
                management.getScriptHash(),
                "OwnerChange",
                new ArrayStackItem(asList(new ByteStringStackItem(alice.getScriptHash().toLittleEndianArray())))
        );
        NeoApplicationLog.Execution exec = neow3j.getApplicationLog(txHash).send().getApplicationLog()
                .getFirstExecution();
        assertThat(exec.getNotifications(), hasSize(1));
        assertThat(exec.getFirstNotification(), is(expected));

        assertThat(management.owner(), is(alice.getScriptHash()));

        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.setOwner(bob.getScriptHash()).withSigners(calledByEntry(owner)).signSendAndAwait());
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No authorization - only owner"));

        // reverse set owner
        management.setOwner(ownerScriptHash).withSigners(
                none(owner).setAllowedContracts(management.getScriptHash()),
                none(alice).setAllowedContracts(management.getScriptHash())
        ).signSendAndAwait();
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
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Invalid new owner"));

        thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("setOwner", byteArrayFromString("invalid"))
                        .signers(calledByEntry(owner))
                        .sign()
        );
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Invalid new owner"));
    }

    @Test
    @Order(0)
    public void testSetOwner_unauthorized() throws IOException {
        assertThat(management.owner(), is(ownerScriptHash));
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.setOwner(ownerScriptHash).withSigners(calledByEntry(alice)).signSendAndAwait());
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No authorization - only owner"));
    }

    // endregion
    // region set relayer

    @Test
    @Order(0)
    public void testSetRelayer() throws Throwable {
        assertThat(management.relayer(), is(relayerScriptHash));

        Hash256 txHash = management.setRelayer(owner, bob.getScriptHash());
        Notification expected = new Notification(
                management.getScriptHash(),
                "RelayerChange",
                new ArrayStackItem(asList(new ByteStringStackItem(bob.getScriptHash().toLittleEndianArray())))
        );
        NeoApplicationLog.Execution exec = neow3j.getApplicationLog(txHash).send().getApplicationLog()
                .getFirstExecution();
        assertThat(exec.getNotifications(), hasSize(1));
        assertThat(exec.getFirstNotification(), is(expected));

        assertThat(management.relayer(), is(bob.getScriptHash()));

        // reverse set owner
        management.setRelayer(owner, relayerScriptHash);
        assertThat(management.relayer(), is(relayerScriptHash));
    }

    @Test
    @Order(0)
    public void testSetRelayer_unauthorized() throws IOException {
        assertThat(management.relayer(), is(relayerScriptHash));
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.setRelayer(alice, charlie.getScriptHash()));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No authorization - only owner"));
    }

    // endregion
    // region validator threshold

    @Test
    @Order(0)
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
    @Order(0)
    public void testSetValidatorThreshold_tooLow() throws Throwable {
        // Threshold lower than 2 should not be allowed.
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.setValidatorThreshold(owner, 1));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Threshold too low"));

        // Threshold equal to 2 (minimum) should be allowed.
        management.setValidatorThreshold(owner, 2);
        assertThat(management.validatorThreshold(), is(2));

        // Reset the threshold to the default used in these tests.
        management.setValidatorThreshold(owner, defaultValidatorThreshold);
        assertThat(management.validatorThreshold(), is(defaultValidatorThreshold));
    }

    @Test
    @Order(0)
    public void testSetValidatorThreshold_tooHigh() throws Throwable {
        int nrValidators = management.validators().size();
        // Threshold higher than the number of validators should not be allowed.
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.setValidatorThreshold(owner, nrValidators + 1));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Threshold too high"));

        // Threshold equal to the number of validators should be allowed.
        management.setValidatorThreshold(owner, nrValidators);
        assertThat(management.validatorThreshold(), is(nrValidators));

        // Reset the threshold to the default used in these tests.
        management.setValidatorThreshold(owner, defaultValidatorThreshold);
        assertThat(management.validatorThreshold(), is(defaultValidatorThreshold));
    }

    @Test
    @Order(0)
    public void testSetValidatorThreshold_unauthorized() {
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.setValidatorThreshold(relayer, 3));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No authorization - only owner"));
    }

    // endregion
    // region validators add

    @Test
    @Order(0)
    public void testValidatorAdd() throws Throwable {
        assertFalse(management.isValidator(florianPubKey));
        assertThat(management.validatorThreshold(), is(5));
        management.addValidator(owner, florianPubKey, false);
        assertTrue(management.isValidator(florianPubKey));
        assertThat(management.validatorThreshold(), is(5));

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
        assertTrue(management.isValidator(validator2PubKey));
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.addValidator(owner, validator2PubKey, false));
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Already a validator"));
    }

    @Test
    @Order(0)
    public void testValidatorAdd_failWithInvalidPublicKey() {
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("addValidator", any(null), bool(false))
                        .signers(calledByEntry(owner))
                        .sign());
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Invalid public key"));

        thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("addValidator", byteArrayFromString("hello"), bool(false))
                        .signers(calledByEntry(owner))
                        .sign());
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Invalid public key"));
    }

    @Test
    @Order(0)
    public void testValidatorAdd_unauthorized() {
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.addValidator(relayer, validator6PubKey, false));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No authorization - only owner"));
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
        assertThat(exec.getFirstNotification().getState().getList().get(0).getHexString(),
                is(validator6.getECKeyPair().getPublicKey().getEncodedCompressedHex()));

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
                () -> management.removeValidator(owner, florianPubKey, false));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Not a validator"));
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
        management.setValidatorThreshold(owner, 2);
        management.removeValidator(owner, validator7PubKey, false);
        management.removeValidator(owner, validator6PubKey, false);
        management.removeValidator(owner, validator5PubKey, false);
        assertThat(management.validatorThreshold(), is(2));
        assertTrue(management.isValidator(validator4PubKey));

        // Threshold should be 2 now. Removing a validator and decrementing the threshold further should fail.
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.removeValidator(owner, validator4PubKey, true));
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Min validator threshold reached."));

        management.removeValidator(owner, validator4PubKey, false);
        management.removeValidator(owner, validator3PubKey, false);

        // Threshold and number of validators should be 2 now. Removing a validator should not be possible.
        TransactionConfigurationException thrown2 = assertThrows(TransactionConfigurationException.class,
                () -> management.removeValidator(owner, validator2PubKey, false));
        assertThat(thrown2.getMessage(),
                containsString("ABORTMSG is executed. Reason: Min number of validators reached."));

        // Reset the threshold to the default used in these tests.
        management.addValidator(owner, validator3PubKey, false);
        management.addValidator(owner, validator4PubKey, false);
        management.addValidator(owner, validator5PubKey, false);
        management.addValidator(owner, validator6PubKey, false);
        management.addValidator(owner, validator7PubKey, false);
        management.setValidatorThreshold(owner, defaultValidatorThreshold);
    }

    @Test
    @Order(0)
    public void testValidatorRemove_failToRemoveAndNotDecrementThresholdIfMaxThreshold() throws Throwable {
        // If the threshold is equal to the number of validators, a validator should not be removable without
        // decrementing the threshold as well.
        assertThat(management.validators().size(), is(7));
        management.setValidatorThreshold(owner, 7);
        assertTrue(management.isValidator(validator6PubKey));
        assertThat(management.validatorThreshold(), is(7));

        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.removeValidator(owner, validator6PubKey, false));

        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Threshold too high"));

        // Reset the threshold to the default used in these tests.
        management.setValidatorThreshold(owner, defaultValidatorThreshold);
    }

    @Test
    @Order(0)
    public void testValidatorRemove_failWithInvalidPublicKey() {
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("removeValidator", any(null), bool(false))
                        .signers(calledByEntry(owner))
                        .sign());
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Invalid public key"));

        thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("removeValidator", byteArrayFromString("hello"), bool(false))
                        .signers(calledByEntry(owner))
                        .sign());
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Invalid public key"));
    }

    @Test
    @Order(0)
    public void testValidatorRemove_unauthorized() {
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.removeValidator(relayer, validator6PubKey, false));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No authorization - only owner"));
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
        assertThat(exec.getFirstNotification().getState().getList().get(0).getHexString(),
                is(validator6.getECKeyPair().getPublicKey().getEncodedCompressedHex()));
        assertThat(exec.getFirstNotification().getState().getList().get(1).getHexString(),
                is(florian.getECKeyPair().getPublicKey().getEncodedCompressedHex()));

        // reverse add validator
        management.replaceValidator(owner, florianPubKey, validator6PubKey);
        assertTrue(management.isValidator(validator6PubKey));
        assertFalse(management.isValidator(florianPubKey));
        assertThat(management.validatorThreshold(), is(5));
    }

    @Test
    @Order(0)
    public void testValidatorReplace_ifThresholdEqualsNrValidators() throws Throwable {
        management.setValidatorThreshold(owner, 7);
        assertThat(management.validatorThreshold(), is(7));
        assertThat(management.validators().size(), is(7));

        assertTrue(management.isValidator(validator6PubKey));
        assertFalse(management.isValidator(florianPubKey));
        management.replaceValidator(owner, validator6PubKey, florianPubKey);

        assertTrue(management.isValidator(florianPubKey));
        assertFalse(management.isValidator(validator6PubKey));

        // Reverse the replacement
        management.replaceValidator(owner, florianPubKey, validator6PubKey);
        assertTrue(management.isValidator(validator6PubKey));
        assertFalse(management.isValidator(florianPubKey));
        management.setValidatorThreshold(owner, defaultValidatorThreshold);
    }

    @Test
    @Order(0)
    public void testValidatorReplace_ifMinValidators() throws Throwable {
        // If there are only 2 validators left, validators cannot be removed.
        management.setValidatorThreshold(owner, 2);
        management.removeValidator(owner, validator7PubKey, false);
        management.removeValidator(owner, validator6PubKey, false);
        management.removeValidator(owner, validator5PubKey, false);
        management.removeValidator(owner, validator4PubKey, false);
        management.removeValidator(owner, validator3PubKey, false);
        assertThat(management.validatorThreshold(), is(2));
        assertThat(management.validators().size(), is(2));
        assertTrue(management.isValidator(validator2PubKey));
        assertFalse(management.isValidator(florianPubKey));

        management.replaceValidator(owner, validator2PubKey, florianPubKey);
        assertTrue(management.isValidator(florianPubKey));
        assertFalse(management.isValidator(validator2PubKey));

        // Reset to the default state used in these tests.
        management.replaceValidator(owner, florianPubKey, validator2PubKey);
        management.addValidator(owner, validator3PubKey, false);
        management.addValidator(owner, validator4PubKey, false);
        management.addValidator(owner, validator5PubKey, false);
        management.addValidator(owner, validator6PubKey, false);
        management.addValidator(owner, validator7PubKey, false);
        management.setValidatorThreshold(owner, defaultValidatorThreshold);
    }

    @Test
    @Order(0)
    public void testValidatorReplace_failReplacingWithInvalidPublicKey() {
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("replaceValidator", publicKey(validator6PubKey), any(null))
                        .signers(calledByEntry(owner))
                        .sign());
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Invalid public key"));

        thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.invokeFunction("replaceValidator", publicKey(validator6PubKey),
                                byteArrayFromString("hello"))
                        .signers(calledByEntry(owner))
                        .sign());
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: Invalid public key"));
    }

    @Test
    @Order(0)
    public void testValidatorReplace_failReplacingWithSamePublicKey() {
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.replaceValidator(owner, validator6PubKey, validator6PubKey));
        assertThat(thrown.getMessage(),
                containsString("ABORTMSG is executed. Reason: Public keys must differ."));
    }

    @Test
    @Order(0)
    public void testValidatorReplace_unauthorized() {
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.replaceValidator(relayer, validator6PubKey, florianPubKey));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No authorization - only owner"));
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

    // endregion
    // region set governor

    @Test
    @Order(0)
    public void testSetGovernor() throws Throwable {
        assertThat(management.governor(), is(governorScriptHash));

        Hash256 txHash = management.setGovernor(owner, bob.getScriptHash());
        Notification expected = new Notification(
                management.getScriptHash(),
                "GovernorChange",
                new ArrayStackItem(asList(new ByteStringStackItem(bob.getScriptHash().toLittleEndianArray())))
        );
        NeoApplicationLog.Execution exec = neow3j.getApplicationLog(txHash).send().getApplicationLog()
                .getFirstExecution();
        assertThat(exec.getNotifications(), hasSize(1));
        assertThat(exec.getFirstNotification(), is(expected));

        assertThat(management.governor(), is(bob.getScriptHash()));

        // reverse set governor
        management.setGovernor(owner, governorScriptHash);
        assertThat(management.governor(), is(governorScriptHash));
    }

    @Test
    @Order(0)
    public void testSetGovernor_unauthorized() throws IOException {
        assertThat(management.governor(), is(governorScriptHash));
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.setGovernor(alice, charlie.getScriptHash()));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No authorization - only owner"));
    }

    // endregion
    // region set security guard

    @Test
    @Order(0)
    public void testSetSecurityGuard() throws Throwable {
        assertThat(management.securityGuard(), is(securityGuardScriptHash));

        Hash256 txHash = management.setSecurityGuard(owner, florian.getScriptHash());
        Notification expected = new Notification(
                management.getScriptHash(),
                "SecurityGuardChange",
                new ArrayStackItem(asList(new ByteStringStackItem(florian.getScriptHash().toLittleEndianArray())))
        );
        NeoApplicationLog.Execution exec = neow3j.getApplicationLog(txHash).send().getApplicationLog()
                .getFirstExecution();
        assertThat(exec.getNotifications(), hasSize(1));
        assertThat(exec.getFirstNotification(), is(expected));

        assertThat(management.securityGuard(), is(florian.getScriptHash()));

        // reverse set security guard
        management.setSecurityGuard(owner, securityGuardScriptHash);
        assertThat(management.securityGuard(), is(securityGuardScriptHash));
    }

    @Test
    @Order(0)
    public void testSetSecurityGuard_unauthorized() throws IOException {
        assertThat(management.securityGuard(), is(securityGuardScriptHash));
        TransactionConfigurationException thrown = assertThrows(TransactionConfigurationException.class,
                () -> management.setSecurityGuard(alice, charlie.getScriptHash()));
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No authorization - only owner"));
    }

    // endregion
    // region contract update

    @Test
    @Order(100)
    public void testUpdateContract() throws Throwable {
        File contractNefFile = Paths.get("src", "test", "resources", "DummyBridgeManagement.nef").toFile();
        NefFile nefFile = NefFile.readFromFile(contractNefFile);

        File manifestFile = Paths.get("src", "test", "resources", "DummyBridgeManagement.manifest.json").toFile();
        FileInputStream s = new FileInputStream(manifestFile);
        ContractManifest        manifest = ObjectMapperFactory.getObjectMapper().readValue(s, ContractManifest.class);

        management.update(owner, nefFile, manifest, null);
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
        assertThat(thrown.getMessage(), containsString("ABORTMSG is executed. Reason: No authorization - only owner"));
    }

    // endregion

}
