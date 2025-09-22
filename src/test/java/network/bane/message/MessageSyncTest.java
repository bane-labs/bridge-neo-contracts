package network.bane.message;

import io.neow3j.contract.GasToken;
import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.core.response.NeoApplicationLog;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.protocol.core.response.Notification;
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
import io.neow3j.wallet.Account;
import network.bane.management.BridgeManagementContract;
import network.bane.messageexecution.ExecutionManagerContract;
import network.bane.testhelper.TestContract;
import network.bane.testhelper.TestMessageSenderContract;
import network.bane.util.MessageHelper;
import network.bane.util.structs.ExecutableStateDto;
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

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static io.neow3j.transaction.AccountSigner.global;
import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.types.ContractParameter.string;
import static io.neow3j.utils.Numeric.hexStringToByteArray;
import static io.neow3j.utils.Numeric.toBytesPadded;
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
import static network.bane.util.TestHelper.waitUntilTransactionIsExecuted;
import static network.bane.util.helper.DefaultTestValues.MANAGEMENT_CONTRACT_HASH;
import static network.bane.util.helper.DefaultTestValues.MESSAGE_BRIDGE_CONTRACT_HASH;
import static network.bane.util.helper.PrintHelper.printTransactionFee;
import static network.bane.util.helper.TestHelper.alice;
import static network.bane.util.helper.TestHelper.createBridgeManagementDeployConfig;
import static network.bane.util.helper.TestHelper.createExecutionManagerDeployConfig;
import static network.bane.util.helper.TestHelper.createMessageBridgeDeployConfig;
import static network.bane.util.helper.TestHelper.executionManager;
import static network.bane.util.helper.TestHelper.gasToken;
import static network.bane.util.helper.TestHelper.getNextEvmNonce;
import static network.bane.util.helper.TestHelper.management;
import static network.bane.util.helper.TestHelper.messageBridge;
import static network.bane.util.helper.TestHelper.messageTestStorer;
import static network.bane.util.helper.TestHelper.neow3j;
import static network.bane.util.helper.TestHelper.setup;
import static network.bane.util.helper.TestHelper.setupExecutionManager;
import static network.bane.util.helper.TestHelper.setupMessageBridge;
import static network.bane.util.helper.TestHelper.setupTestContract;
import static network.bane.util.helper.TestHelper.setupTestMessageSender;
import static network.bane.util.helper.TestHelper.testContract;
import static network.bane.util.helper.TestHelper.testMessageSender;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ContractTest(
        blockTime = 1,
        contracts = {BridgeManagementContract.class, MessageBridgeContract.class, TestContract.class,
                TestMessageSenderContract.class, ExecutionManagerContract.class},
        batchFile = "setup.batch"
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MessageSyncTest {

    // Neow3j is missing a wrapper for the native StdLib in their SDK.
    private static SmartContract stdLib;

    @RegisterExtension
    public static final ContractTestExtension ext = new ContractTestExtension();

    @BeforeAll
    public static void setUp() throws Throwable {
        setup(ext);
        setupMessageBridge(ext);
        setupTestContract(ext);
        setupTestMessageSender(ext);
        setupExecutionManager(ext);

        stdLib = new SmartContract(new Hash160("0xacce6fd80d44e1796aa0c2c625e9e4e0ce39efc0"), neow3j);

        // This test requires the constant MESSAGE_BRIDGE_CONTRACT_HASH to be set correctly. The execution manager
        // requires the contract address at deployment time, so we cannot change it later.
        if (!messageBridge.getScriptHash().equals(MESSAGE_BRIDGE_CONTRACT_HASH)) {
            throw new RuntimeException(format("The message bridge contract hash is not set correctly. Update the " +
                    "script hash to 0x%s.", messageBridge.getScriptHash()));
        }
        // This test requires the constant MANAGEMENT_CONTRACT_HASH to be set correctly. The execution manager
        // requires the contract address at deployment time, so we cannot change it later.
        if (!MANAGEMENT_CONTRACT_HASH.equals(management.getScriptHash())) {
            throw new RuntimeException(format("The management contract hash is not set correctly. Update the script " +
                    "hash to 0x%s.", management.getScriptHash()));
        }

        messageBridge.setExecutionManager(executionManager.getScriptHash());
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

    @DeployConfig(ExecutionManagerContract.class)
    public static DeployConfiguration deployConfigExecutionManager() {
        return createExecutionManagerDeployConfig();
    }

    private static BigInteger getBestBlockTime() throws IOException {
        return BigInteger.valueOf(
                neow3j.getBlockHeader(neow3j.getBestBlockHash().send().getBlockHash()).send().getBlock().getTime());
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
        byte[] msgBytes = messageBridge.serializeCall(testContract, "store", CallFlags.WRITE_STATES,
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

        ExecutableStateDto execState = messageBridge.getExecutableState(nonce);
        assertFalse(execState.executed);
        assertThat(execState.expirationTimestamp, greaterThan(getBestBlockTime()));
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

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> messageBridge.getExecutableState(nonce));
        assertThat(thrown.getMessage(), containsString("Executable state not found"));
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
    // region sending

    @Test
    @Order(1)
    public void test_sendMessage_1_executable() throws Throwable {
        BigInteger nextEvmNonce = getNextEvmNonce();
        // a raw valid encoded evm call:
        byte[] rawMessage = hexStringToByteArray(
                "0x00000000000000000000000000000000000000000000000000000000000000200000000000000000000000001d1499e622d69689cdf9004d05ec547d650ff2110000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000044cadc7a78000000000000000000000000000000000000000000000000000000000000006400000000000000000000000000000000000000000000000000000000000000c800000000000000000000000000000000000000000000000000000000");

        Hash256 tx = testMessageSender.sendExecutableMessage(rawMessage, true);

        NeoApplicationLog.Execution firstExec = neow3j.getApplicationLog(tx).send().getApplicationLog()
                .getFirstExecution();
        assertThat(firstExec.getFirstStackItem().getInteger(), is(nextEvmNonce));
        assertThat(firstExec.getNotifications(), hasSize(2));
        Notification msgSendEvent = firstExec.getNotification(1);
        assertThat(msgSendEvent.getContract(), is(messageBridge.getScriptHash()));
        assertThat(msgSendEvent.getEventName(), is("MessageSend"));
        List<StackItem> eventItems = msgSendEvent.getState().getList();
        assertThat(eventItems, hasSize(5));
        assertThat(eventItems.get(0).getInteger(), is(nextEvmNonce));
        assertThat(eventItems.get(1).getType(), is(StackItemType.BYTE_STRING));
        assertThat(eventItems.get(2).getType(), is(StackItemType.BYTE_STRING));  // raw message
        assertThat(eventItems.get(2).getByteArray().length, is(rawMessage.length));
        assertThat(eventItems.get(3).getType(), is(StackItemType.BYTE_STRING));
        assertThat(eventItems.get(3).getByteArray().length, is(Hash256.ZERO.getSize()));
        assertThat(eventItems.get(4).getType(), is(StackItemType.BYTE_STRING));
        assertThat(eventItems.get(4).getByteArray().length, is(Hash256.ZERO.getSize()));

        // Deserialize the serialized metadata bytes from the event to get the individual metadata values.
        List<StackItem> metadataItems = stdLib.callInvokeFunction("deserialize",
                        asList(byteArray(eventItems.get(1).getByteArray()))).getInvocationResult().getFirstStackItem()
                .getList();
        assertThat(metadataItems, hasSize(4));
        BigInteger msgType = metadataItems.get(0).getInteger();
        assertThat(msgType.intValue(), is(0)); // 0: EXECUTABLE
        BigInteger timestamp = metadataItems.get(1).getInteger();
        Hash160 sender = Hash160.fromAddress(metadataItems.get(2).getAddress());
        boolean storeResult = metadataItems.get(3).getBoolean();
        N3MessageMetadataExecDto metadataDto = new N3MessageMetadataExecDto(timestamp, sender, storeResult);
        byte[] concatBytes = concatenateOp(nextEvmNonce, metadataDto, rawMessage);

        assertThat(sender, is(testMessageSender.getScriptHash()));
        assertTrue(storeResult);

        // The pieces of the cocatenated message bytes should include the following in that order:
        // 0000000000000000000000000000000000000000000000000000000000000001 // nonce
        // 00                                                               // msg type
        // 00000000000000000000000000000000000000000000000000000198528a9ce8 // timestamp (based on current N3 timestamp)
        // 639ab3eec9bfc00d00e0e608ec2df87c7d98d79a                         // sender
        // 01                                                               // store result boolean
        // 00000000000000000000000000000000000000000000000000000000000000200000000000000000000000001d1499e622d69689cdf9004d05ec547d650ff2110000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000044cadc7a78000000000000000000000000000000000000000000000000000000000000006400000000000000000000000000000000000000000000000000000000000000c800000000000000000000000000000000000000000000000000000000

        String concatenationHex = toHexStringNoPrefix(concatBytes);
        // nonce (uint256) + msgType (uint8) + timestamp (uint256) + sender (address) + storeResult (bool) +
        // rawMessage (arbitrary bytes)
        assertThat(concatBytes.length, is(32 + 1 + 32 + 20 + 1 + rawMessage.length));
        // The end of the concatenation should be the raw message.
        assertThat(concatenationHex, endsWith(toHexStringNoPrefix(rawMessage)));
        // The beginning of the concatenation should be the nonce padded to 32 bytes and the message type (1 byte).
        assertThat(concatenationHex, startsWith(
                toHexStringNoPrefix(toBytesPadded(nextEvmNonce, 32)) + toHexStringNoPrefix(msgType.toByteArray())
        ));
        assertThat(new Hash160(concatenationHex.substring(130, 130 + 40)), is(testMessageSender.getScriptHash()));
        assertThat(concatenationHex.substring(170, 172), is("01"));

        // msg hash: c5e8122e5466b9c10e150d08df380a10771467d5f6e21c5234a167f690b8934f
        // with nonce: 1
        // with msg type: 0
        // with timestamp: 1753776888950
        // with sender: 639ab3eec9bfc00d00e0e608ec2df87c7d98d79a
        // with store result: true
        // with raw message:
        // 00000000000000000000000000000000000000000000000000000000000000200000000000000000000000001d1499e622d69689cdf9004d05ec547d650ff2110000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000044cadc7a78000000000000000000000000000000000000000000000000000000000000006400000000000000000000000000000000000000000000000000000000000000c800000000000000000000000000000000000000000000000000000000

        // complete concatenated bytes:
        // 00000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000198553f9c76639ab3eec9bfc00d00e0e608ec2df87c7d98d79a0100000000000000000000000000000000000000000000000000000000000000200000000000000000000000001d1499e622d69689cdf9004d05ec547d650ff2110000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000044cadc7a78000000000000000000000000000000000000000000000000000000000000006400000000000000000000000000000000000000000000000000000000000000c800000000000000000000000000000000000000000000000000000000
    }

    @Test
    @Order(2)
    public void test_sendMessage_2_storeOnly() throws Throwable {
        BigInteger nextEvmNonce = getNextEvmNonce();
        // a raw message:
        String msg = "There's nowhere I can't go. There's nowhere I won't find you.";
        byte[] rawMessage = msg.getBytes();

        Hash256 tx = testMessageSender.sendMessage(rawMessage);

        NeoApplicationLog.Execution firstExec = neow3j.getApplicationLog(tx).send().getApplicationLog()
                .getFirstExecution();
        assertThat(firstExec.getFirstStackItem().getInteger(), is(nextEvmNonce));
        assertThat(firstExec.getNotifications(), hasSize(2));
        Notification msgSendEvent = firstExec.getNotification(1);
        assertThat(msgSendEvent.getContract(), is(messageBridge.getScriptHash()));
        assertThat(msgSendEvent.getEventName(), is("MessageSend"));
        List<StackItem> eventItems = msgSendEvent.getState().getList();
        assertThat(eventItems, hasSize(5));
        assertThat(eventItems.get(0).getInteger(), is(nextEvmNonce));
        assertThat(eventItems.get(1).getType(), is(StackItemType.BYTE_STRING));
        assertThat(eventItems.get(2).getType(), is(StackItemType.BYTE_STRING));
        assertThat(eventItems.get(3).getType(), is(StackItemType.BYTE_STRING));
        assertThat(eventItems.get(3).getByteArray().length, is(Hash256.ZERO.getSize()));
        assertThat(eventItems.get(4).getType(), is(StackItemType.BYTE_STRING));
        assertThat(eventItems.get(4).getByteArray().length, is(Hash256.ZERO.getSize()));

        // Deserialize the serialized metadata bytes from the event to get the individual metadata values.
        List<StackItem> metadataItems = stdLib.callInvokeFunction("deserialize",
                        asList(byteArray(eventItems.get(1).getByteArray()))).getInvocationResult().getFirstStackItem()
                .getList();
        assertThat(metadataItems, hasSize(3));
        BigInteger msgType = metadataItems.get(0).getInteger();
        assertThat(msgType.intValue(), is(1)); // 1: STORE_ONLY
        BigInteger timestamp = metadataItems.get(1).getInteger();
        Hash160 sender = Hash160.fromAddress(metadataItems.get(2).getAddress());
        N3MessageMetadataStoreOnlyDto metadataDto = new N3MessageMetadataStoreOnlyDto(timestamp, sender);
        byte[] concatBytes = concatenateOp(nextEvmNonce, metadataDto, rawMessage);

        assertThat(sender, is(testMessageSender.getScriptHash()));

        // The pieces of the cocatenated message bytes should include the following in that order:
        // 0000000000000000000000000000000000000000000000000000000000000002 // nonce
        // 01                                                               // msg type
        // 00000000000000000000000000000000000000000000000000000198528a9ce8 // timestamp (based on current N3 timestamp)
        // 639ab3eec9bfc00d00e0e608ec2df87c7d98d79a                         // sender
        // 54686572652773206e6f776865726520492063616e277420676f2e2054686572652773206e6f7768657265204920776f6e27742066696e6420796f752e

        // nonce (uint256) + msgType (uint8) + timestamp (uint256) + sender (address) + rawMessage (arbitrary bytes)
        assertThat(concatBytes.length, is(32 + 1 + 32 + 20 + rawMessage.length));
        String concatenationHex = toHexStringNoPrefix(concatBytes);
        // The end of the concatenation should be the raw message.
        assertThat(concatenationHex, endsWith(toHexStringNoPrefix(rawMessage)));
        // The beginning of the concatenation should be the nonce padded to 32 bytes and the message type (1 byte).
        assertThat(concatenationHex, startsWith(
                toHexStringNoPrefix(toBytesPadded(nextEvmNonce, 32)) + toHexStringNoPrefix(msgType.toByteArray())
        ));
        // The sender should be part of the concatenation.;
        assertThat(new Hash160(concatenationHex.substring(130, 130 + 40)), is(testMessageSender.getScriptHash()));

        // msg hash: 8537f0ee02066de1943c2205cd4621209dd1ec1a61ae887c5d394d130b63709a
        // with nonce: 2
        // with timestamp: 1753777092836
        // with sender: 639ab3eec9bfc00d00e0e608ec2df87c7d98d79a
        // with raw message:
        // 54686572652773206e6f776865726520492063616e277420676f2e2054686572652773206e6f7768657265204920776f6e27742066696e6420796f752e

        // complete concatenated bytes
        // 000000000000000000000000000000000000000000000000000000000000000201000000000000000000000000000000000000000000000000000001985542b8e4639ab3eec9bfc00d00e0e608ec2df87c7d98d79a54686572652773206e6f776865726520492063616e277420676f2e2054686572652773206e6f7768657265204920776f6e27742066696e6420796f752e
    }

    private BigInteger storeDefaultMessageForTestStoring(String key, ContractParameter value) throws Throwable {
        return messageBridge.storeMessage(getSerializedN3MethodForTestStoring(key, value));
    }

    private byte[] getSerializedN3MethodForTestStoring(String key, ContractParameter value) throws IOException {
        return messageBridge.serializeCall(messageTestStorer.getScriptHash(), "storeValue", CallFlags.ALL,
                asList(string(key), value));
    }

    @Test
    @Order(3)
    public void test_sendMessage_3_result() throws Throwable {
        BigInteger sponsorAmount = BigInteger.valueOf(1000000);
        NeoSendRawTransaction sponsorRawTx = gasToken.transfer(alice, messageBridge.getScriptHash(), sponsorAmount)
                .signers(global(alice)).sign().send();
        waitUntilTransactionIsExecuted(sponsorRawTx, neow3j);

        BigInteger messageBridgeGasBalance = gasToken.getBalanceOf(messageBridge.getScriptHash());
        // Create a function to fetch the current balance of the message bridge contract. Then, return it.
        byte[] getMessageBridgeGasBalance = messageBridge.serializeCall(gasToken.getScriptHash(),
                "balanceOf", CallFlags.ALL, asList(hash160(messageBridge.getScriptHash())));
        BigInteger executableMessageNonce = messageBridge.storeMessage(getMessageBridgeGasBalance);

        byte[] resultBytesBefore = messageBridge.getResult(executableMessageNonce);
        assertThat(resultBytesBefore, is(new byte[0]));

        ExecutableStateDto execStateBeforeExec = messageBridge.getExecutableState(executableMessageNonce);
        assertFalse(execStateBeforeExec.executed);
        assertThat(execStateBeforeExec.expirationTimestamp, greaterThan(getBestBlockTime()));

        Hash256 tx = messageBridge.executeMessage(global(alice), executableMessageNonce);
        NeoApplicationLog.Execution firstExec = neow3j.getApplicationLog(tx).send().getApplicationLog()
                .getFirstExecution();

        assertThat(firstExec.getNotifications(), hasSize(1));
        assertThat(firstExec.getNotifications().get(0).getContract(), is(messageBridge.getScriptHash()));
        assertThat(firstExec.getNotifications().get(0).getEventName(), is("Execution"));
        StackItem event2State = firstExec.getFirstNotification().getState();
        assertThat(event2State.getList().get(0).getInteger(), is(executableMessageNonce));
        assertThat(event2State.getList().get(1).getInteger(), is(BigInteger.ONE));
        assertThat(event2State.getList().get(2).getInteger(), is(BigInteger.ZERO));
        assertThat(event2State.getList().get(3).getInteger(), is(messageBridgeGasBalance));

        ExecutableStateDto execStateAfterExec = messageBridge.getExecutableState(executableMessageNonce);
        assertTrue(execStateAfterExec.executed);

        BigInteger nextEvmNonce = getNextEvmNonce();

        Hash256 resultSendTx = testMessageSender.sendResultMessage(global(alice), executableMessageNonce);
        NeoApplicationLog.Execution resultExec = neow3j.getApplicationLog(resultSendTx).send().getApplicationLog()
                .getFirstExecution();
        assertThat(resultExec.getNotifications(), hasSize(2));
        Notification gasTransferEvent = resultExec.getNotification(0);
        assertThat(gasTransferEvent.getContract(), is(GasToken.SCRIPT_HASH));
        assertThat(gasTransferEvent.getEventName(), is("Transfer"));

        Notification msgSendEvent = resultExec.getNotification(1);
        assertThat(msgSendEvent.getContract(), is(messageBridge.getScriptHash()));
        assertThat(msgSendEvent.getEventName(), is("MessageSend"));
        List<StackItem> eventItems = msgSendEvent.getState().getList();
        assertThat(eventItems, hasSize(5));
        assertThat(eventItems.get(0).getInteger(), is(nextEvmNonce));
        assertThat(eventItems.get(1).getType(), is(StackItemType.BYTE_STRING));
        assertThat(eventItems.get(2).getType(), is(StackItemType.BYTE_STRING));
        assertThat(eventItems.get(3).getType(), is(StackItemType.BYTE_STRING));
        assertThat(eventItems.get(3).getByteArray().length, is(Hash256.ZERO.getSize()));
        assertThat(eventItems.get(4).getType(), is(StackItemType.BYTE_STRING));
        assertThat(eventItems.get(4).getByteArray().length, is(Hash256.ZERO.getSize()));

        // Deserialize the serialized metadata bytes from the event to get the individual metadata values.
        List<StackItem> metadataItems = stdLib.callInvokeFunction("deserialize",
                        asList(byteArray(eventItems.get(1).getByteArray()))).getInvocationResult().getFirstStackItem()
                .getList();
        assertThat(metadataItems, hasSize(4));
        BigInteger msgType = metadataItems.get(0).getInteger();
        assertThat(msgType.intValue(), is(2)); // 2: RESULT
        BigInteger timestamp = metadataItems.get(1).getInteger();
        Hash160 sender = Hash160.fromAddress(metadataItems.get(2).getAddress());
        BigInteger relatedMessageNonce = metadataItems.get(3).getInteger();
        N3MessageMetadataResultDto metadataDto = new N3MessageMetadataResultDto(timestamp, sender, relatedMessageNonce);
        byte[] resultBytes = messageBridge.getResult(executableMessageNonce);
        byte[] concatBytes = concatenateOp(nextEvmNonce, metadataDto, resultBytes);

        // The pieces of the cocatenated message bytes should include the following in that order:
        // 0000000000000000000000000000000000000000000000000000000000000001 // nonce
        // 02                                                               // msg type
        // 0000000000000000000000000000000000000000000000000000019856ccdbdd // timestamp (based on current N3 timestamp)
        // 4e2381e7d7cbb4a23c7776be28276ffc107ae28c                         // sender
        // 0000000000000000000000000000000000000000000000000000000000000001 // related message nonce
        // 210340420f                                                       // raw result message

        // nonce (uint256) + msgType (uint8) + timestamp (uint256) + sender (address) + related message (uint256) +
        // rawMessage (arbitrary bytes)
        assertThat(concatBytes.length, is(32 + 1 + 32 + 20 + 32 + resultBytes.length));
        String concatenationHex = toHexStringNoPrefix(concatBytes);
        // The end of the concatenation should be the raw message.
        assertThat(concatenationHex, endsWith(toHexStringNoPrefix(resultBytes)));
        // The beginning of the concatenation should be the nonce padded to 32 bytes and the message type (1 byte).
        assertThat(concatenationHex, startsWith(
                toHexStringNoPrefix(toBytesPadded(nextEvmNonce, 32)) + toHexStringNoPrefix(msgType.toByteArray())
        ));
        // The sender should be part of the concatenation;
        assertThat(new Hash160(concatenationHex.substring(130, 130 + 40)), is(testMessageSender.getScriptHash()));

        // msg hash: 0x92c4ccaaedaa74d8f0f40138ec1d309dd853e378b70695981070e7a16e732de3
        // with nonce: 0000000000000000000000000000000000000000000000000000000000000001
        // with msg type: 02                                                               // msg type
        // with timestamp: 1753802922973
        // with sender: 4e2381e7d7cbb4a23c7776be28276ffc107ae28c                         // sender
        // with related nonce: 1
        // with raw message: 210340420f

        // complete concatenated bytes:
        // 0000000000000000000000000000000000000000000000000000000000000001020000000000000000000000000000000000000000000000000000019856ccdbdd4e2381e7d7cbb4a23c7776be28276ffc107ae28c0000000000000000000000000000000000000000000000000000000000000001210340420f
    }

    // endregion

}
