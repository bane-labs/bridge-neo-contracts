package network.bane.message;

import io.neow3j.test.ContractTest;
import io.neow3j.test.ContractTestExtension;
import io.neow3j.test.DeployConfig;
import io.neow3j.test.DeployConfiguration;
import io.neow3j.types.CallFlags;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;
import network.bane.management.BridgeManagementContract;
import network.bane.testhelper.TestContract;
import network.bane.util.MessageHelper;
import network.bane.util.structs.N3MessageDto;
import network.bane.util.structs.N3MessageMetadataDto;
import network.bane.util.structs.N3MessageMetadataExecDto;
import network.bane.util.structs.N3MessageMetadataResultDto;
import network.bane.util.structs.N3MessageMetadataStoreOnlyDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigInteger;
import java.util.List;

import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.utils.Numeric.toHexString;
import static io.neow3j.utils.Numeric.toHexStringNoPrefix;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static network.bane.util.MessageHelper.concatenateOp;
import static network.bane.util.MessageHelper.createN3MessageHash;
import static network.bane.util.MessageHelper.getMessageStorEvents;
import static network.bane.util.TestHelper.concatAndKeccak256;
import static network.bane.util.TestHelper.keccak256Hex;
import static network.bane.util.TestHelper.signMsg;
import static network.bane.util.TestHelper.validator1;
import static network.bane.util.TestHelper.validator2;
import static network.bane.util.TestHelper.validator3;
import static network.bane.util.TestHelper.validator4;
import static network.bane.util.TestHelper.validator5;
import static network.bane.util.helper.DefaultTestValues.MANAGEMENT_CONTRACT_HASH;
import static network.bane.util.helper.PrintHelper.printTransactionFee;
import static network.bane.util.helper.TestHelper.createBridgeManagementDeployConfig;
import static network.bane.util.helper.TestHelper.createMessageBridgeDeployConfig;
import static network.bane.util.helper.TestHelper.management;
import static network.bane.util.helper.TestHelper.messageBridge;
import static network.bane.util.helper.TestHelper.neow3j;
import static network.bane.util.helper.TestHelper.setup;
import static network.bane.util.helper.TestHelper.setupMessageBridge;
import static network.bane.util.helper.TestHelper.setupTestContract;
import static network.bane.util.helper.TestHelper.testContract;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ContractTest(
        blockTime = 1,
        contracts = {BridgeManagementContract.class, MessageBridgeContract.class, TestContract.class},
        batchFile = "setup.batch"
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MessageSyncTest {

    @RegisterExtension
    public static final ContractTestExtension ext = new ContractTestExtension();

    @BeforeAll
    public static void setUp() throws Throwable {
        setup(ext);
        setupMessageBridge(ext);
        setupTestContract(ext);

        // This test requires the constant MANAGEMENT_CONTRACT_HASH to be set correctly. The execution manager
        // requires the contract address at deployment time, so we cannot change it later.
        if (!MANAGEMENT_CONTRACT_HASH.equals(management.getScriptHash())) {
            throw new RuntimeException(format("The management contract hash is not set correctly. Update the script " +
                    "hash to 0x%s.", management.getScriptHash()));
        }

        messageBridge.unpause();
    }

    @DeployConfig(BridgeManagementContract.class)
    public static DeployConfiguration deployConfigManagement() {
        return createBridgeManagementDeployConfig();
    }

    @DeployConfig(MessageBridgeContract.class)
    public static DeployConfiguration deployConfigMessageBridge() {
        return createMessageBridgeDeployConfig();
    }

    // region store

    @Test
    @Order(1)
    public void test_storeMessage_1_executable() throws Throwable {
        // This test only works if it is the first test in the order of storing messages, due to the use of raw data for
        // the nonce, to and amount.
        BigInteger nonce = BigInteger.ONE;
        BigInteger timestamp = new BigInteger("1753000000");
        Hash160 sender = new Hash160("0x69ecca587293047be4c59159bf8bc399985c160d");
        byte[] msgBytes = messageBridge.getSerializedN3MethodCall(testContract, "store", CallFlags.WRITE_STATES,
                asList(integer(nonce), integer(timestamp)));
        assertThat(toHexStringNoPrefix(msgBytes),
                is("400428141418e358c565207768eae8d237241e85d3e9f1cb280573746f72652101024002210101210440a87c68"));

        N3MessageMetadataExecDto metadata = new N3MessageMetadataExecDto(timestamp, sender, true);

        byte[] concatenatedBytes = concatenateOp(nonce, metadata, msgBytes);
        String offChainConcatenatedHex = toHexStringNoPrefix(concatenatedBytes);

        // The concatenated message bytes should look like this: (from EVM side)
        // 0000000000000000000000000000000000000000000000000000000000000001 nonce
        // 00                                                               executable type
        // 00000000000000000000000000000000000000000000000000000000687ca840 timestamp
        // 69ecca587293047be4c59159bf8bc399985c160d                         sender
        // 01                                                               store result
        // 400428141418e358c565207768eae8d237241e85d3e9f1cb280573746f72652101024002210101210440a87c68 arb message

        // Asserting the concatenated hex string. This is not required, but helps to understand the concatenation.
        assertThat(offChainConcatenatedHex,
                is("00000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000687ca84069ecca587293047be4c59159bf8bc399985c160d01400428141418e358c565207768eae8d237241e85d3e9f1cb280573746f72652101024002210101210440a87c68"));

        String msgHash1 = createN3MessageHash(nonce, metadata, msgBytes);
        assertThat(msgHash1, is(keccak256Hex(concatenatedBytes)));

        // Validating the message hash.
        assertThat(msgHash1, is("0x2a9d36cc38d44ab810d0d484bb21f483d4ee4d676f1773859e8043e3a1d3601d"));

        // In the following, the message is stored on-chain to validate that the root will be calculated equally in the
        // contract.

        N3MessageDto n3MessageDto = new N3MessageDto(metadata, msgBytes);

        String root = concatAndKeccak256(Hash256.ZERO.toString(), msgHash1);

        // Validating the root.
        assertThat(root, is("0x2701caa1bdd24a0ec6908676b944925bc9cf117b961d644edcf3d7e20ddfe8ad"));

        ContractParameter messageEnvelope = array(integer(nonce), n3MessageDto.toContractParameter(messageBridge));
        String concatenation = messageBridge.concatenateOperation(messageEnvelope);
        assertThat(concatenation, is(offChainConcatenatedHex));

        List<Account> validators = asList(validator1, validator2, validator3, validator4, validator5);
        Hash256 txHash = messageBridge.storeMessages(root, signMsg(validators, root), array(messageEnvelope));

        printTransactionFee(neow3j, "tx with 1 message", txHash);
        List<MessageHelper.N3MessageStoreEvent> n3MessageStoreEvents = getMessageStorEvents(txHash, neow3j,
                messageBridge.getScriptHash());
        assertThat(n3MessageStoreEvents, hasSize(1));
        MessageHelper.N3MessageStoreEvent n3MessageStoreEvent = n3MessageStoreEvents.get(0);
        assertThat(n3MessageStoreEvent.nonce, is(nonce));
        N3MessageMetadataDto expectedMetadata = metadata;
        assertThat(n3MessageStoreEvent.metadataSerializedHex,
                is(toHexStringNoPrefix(expectedMetadata.serialize(messageBridge))));

        N3MessageDto message = messageBridge.getMessage(nonce);
        assertThat(message, is(n3MessageDto));

        assertTrue(messageBridge.isPending(nonce));
    }

    @Test
    @Order(2)
    public void test_storeMessage_2_storeOnly() throws Throwable {
        // This test only works as the second test in the order of storing messages, due to the use of raw data for
        // the nonce, to and amount. It assumes that the first test has already set up the necessary state.
        BigInteger nonce = BigInteger.valueOf(2);
        BigInteger timestamp = new BigInteger("1753100005");
        Hash160 sender = new Hash160("0x82d53419cdb80a84a1a9c699c6cc333236169b98");
        String msg = "There’s nowhere I can’t go. There’s nowhere I won’t find you.";
        byte[] msgBytes = msg.getBytes();

        N3MessageMetadataStoreOnlyDto metadata = new N3MessageMetadataStoreOnlyDto(timestamp, sender);

        byte[] concatenatedBytes = concatenateOp(nonce, metadata, msgBytes);
        String offChainConcatenatedHex = toHexStringNoPrefix(concatenatedBytes);

        // The concatenated message bytes should look like this: (from EVM side)
        // 0000000000000000000000000000000000000000000000000000000000000002 nonce
        // 01                                                               store-only type
        // 00000000000000000000000000000000000000000000000000000000687e2ee5 timestamp
        // 82d53419cdb80a84a1a9c699c6cc333236169b98                         sender
        // 5468657265e2809973206e6f776865726520492063616ee280997420676f2e205468657265e2809973206e6f7768657265204920776f6ee28099742066696e6420796f752e arb message

        // Asserting the concatenated hex string. This is not required, but helps to understand the concatenation.
        assertThat(offChainConcatenatedHex,
                is("00000000000000000000000000000000000000000000000000000000000000020100000000000000000000000000000000000000000000000000000000687e2ee582d53419cdb80a84a1a9c699c6cc333236169b985468657265e2809973206e6f776865726520492063616ee280997420676f2e205468657265e2809973206e6f7768657265204920776f6ee28099742066696e6420796f752e"));

        String msgHash1 = createN3MessageHash(nonce, metadata, msgBytes);
        assertThat(msgHash1, is(keccak256Hex(concatenatedBytes)));

        // Validating the message hash.
        assertThat(msgHash1, is("0x6616d6d15190a04d878aed300e194a02a6b4e94eeb2084f8a5c4b73d95e92dbb"));

        // In the following, the message is stored on-chain to validate that the root will be calculated equally in the
        // contract.

        N3MessageDto n3MessageDto = new N3MessageDto(metadata, msgBytes);

        // Validating the root.
        String root = concatAndKeccak256(messageBridge.getMessageBridge().evmToN3MessageState.root.toString(),
                msgHash1);
        assertThat(root, is("0x0e13e3d88133f283e6592bb48f1405fb941fab340106cf74445aea07743ca91e"));

        ContractParameter messageEnvelope = array(integer(nonce), n3MessageDto.toContractParameter(messageBridge));
        String concatenation = messageBridge.concatenateOperation(messageEnvelope);
        assertThat(concatenation, is(offChainConcatenatedHex));

        List<Account> validators = asList(validator1, validator2, validator3, validator4, validator5);
        Hash256 txHash = messageBridge.storeMessages(root, signMsg(validators, root), array(messageEnvelope));
        printTransactionFee(neow3j, "tx with 1 message", txHash);
        List<MessageHelper.N3MessageStoreEvent> n3MessageStoreEvents = getMessageStorEvents(txHash, neow3j,
                messageBridge.getScriptHash());
        assertThat(n3MessageStoreEvents, hasSize(1));
        MessageHelper.N3MessageStoreEvent n3MessageStoreEvent = n3MessageStoreEvents.get(0);
        assertThat(n3MessageStoreEvent.nonce, is(nonce));
        N3MessageMetadataDto expectedMetadata = metadata;
        assertThat(n3MessageStoreEvent.metadataSerializedHex,
                is(toHexStringNoPrefix(expectedMetadata.serialize(messageBridge))));

        N3MessageDto message = messageBridge.getMessage(nonce);
        assertThat(message, is(n3MessageDto));

        assertFalse(messageBridge.isPending(nonce));
    }

    @Test
    @Order(3)
    public void test_result_concatenation() throws Throwable {
        // This test depends on specific raw data for the nonce, to, and amount. Ensure the test setup matches the
        // expected state.
        BigInteger nonce = new BigInteger("7592037"); //73d865
        BigInteger timestamp = new BigInteger("1753000097"); //687ca8a1
        Hash160 sender = new Hash160("0xfadd389577eae0af6e59f8476f9d808f120407c2");
        byte[] msgBytes = new BigInteger("1000").toByteArray();
        assertThat(toHexString(msgBytes), is("0x03e8"));

        N3MessageMetadataResultDto metadata = new N3MessageMetadataResultDto(timestamp, sender, BigInteger.ONE);

        byte[] concatenatedBytes = concatenateOp(nonce, metadata, msgBytes);
        String offChainConcatenatedHex = toHexStringNoPrefix(concatenatedBytes);

        // The concatenated message bytes should look like this: (from EVM side)
        // 000000000000000000000000000000000000000000000000000000000073d865 nonce
        // 02                                                               result type
        // 00000000000000000000000000000000000000000000000000000000687ca8a1 timestamp
        // fadd389577eae0af6e59f8476f9d808f120407c2                         sender
        // 01                                                               related message nonce
        // 03e8                                                             arb message

        // Asserting the concatenated hex string. This is not required, but helps to understand the concatenation.
        assertThat(offChainConcatenatedHex,
                is("000000000000000000000000000000000000000000000000000000000073d8650200000000000000000000000000000000000000000000000000000000687ca8a1fadd389577eae0af6e59f8476f9d808f120407c2000000000000000000000000000000000000000000000000000000000000000103e8"));

        String msgHash1 = createN3MessageHash(nonce, metadata, msgBytes);
        assertThat(msgHash1, is(keccak256Hex(concatenatedBytes)));

        // Validating the message hash.
        assertThat(msgHash1, is("0x80d26468c8c67d4bfccc3d2ac6276ab98ccbdf1a43d3f0e18b91a6461d75ac22"));

        N3MessageDto n3MessageDto = new N3MessageDto(metadata, msgBytes);
        ContractParameter messageEnvelope = array(integer(nonce), n3MessageDto.toContractParameter(messageBridge));
        String onChainConcat = messageBridge.concatenateOperation(messageEnvelope);
        assertThat(onChainConcat, is(offChainConcatenatedHex));
    }

    // endregion

}
