package network.bane.bridge;

import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.core.response.NeoApplicationLog;
import io.neow3j.protocol.core.response.Notification;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.test.ContractTest;
import io.neow3j.test.ContractTestExtension;
import io.neow3j.test.DeployConfig;
import io.neow3j.test.DeployConfiguration;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.types.NeoVMStateType;
import io.neow3j.utils.Await;
import io.neow3j.wallet.Account;
import network.bane.management.BridgeManagementContract;
import network.bane.testhelper.Nep17TokenContract;
import network.bane.testhelper.TokenTestContract;
import network.bane.util.structs.State;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.utils.Numeric.hexStringToByteArray;
import static io.neow3j.utils.Numeric.prependHexPrefix;
import static io.neow3j.utils.Numeric.toHexStringNoPrefix;
import static java.util.Arrays.asList;
import static network.bane.util.TestHelper.computeNewTokenRootNoPrefix;
import static network.bane.util.TestHelper.sha256HexNoPrefix;
import static network.bane.util.TestHelper.signMsg;
import static network.bane.util.TestHelper.validator1;
import static network.bane.util.TestHelper.validator3;
import static network.bane.util.TestHelper.validator4;
import static network.bane.util.TestHelper.validator5;
import static network.bane.util.TestHelper.validator7;
import static network.bane.util.helper.TestHelper.alice;
import static network.bane.util.helper.TestHelper.bridge;
import static network.bane.util.helper.TestHelper.createBridgeDeployConfig;
import static network.bane.util.helper.TestHelper.createBridgeManagementDeployConfig;
import static network.bane.util.helper.TestHelper.gasToken;
import static network.bane.util.helper.TestHelper.neoN3NeoTokenHash;
import static network.bane.util.helper.TestHelper.neoToken;
import static network.bane.util.helper.TestHelper.neoXNeoTokenHash;
import static network.bane.util.helper.TestHelper.neow3j;
import static network.bane.util.helper.TestHelper.registerNeoTokenBridge;
import static network.bane.util.helper.TestHelper.setup;
import static network.bane.util.helper.TestHelper.setupBridge;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

/**
 * The inputs and outputs in this test are synchronized with tests on the Neo X side.
 */
@ContractTest(
        blockTime = 1,
        contracts = {BridgeManagementContract.class, BridgeContract.class, TokenTestContract.class, Nep17TokenContract.class},
        batchFile = "setup.batch"
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TokenBridgeSyncTest {

    private static SmartContract tokenTestContract;

    @RegisterExtension
    public static final ContractTestExtension ext = new ContractTestExtension();

    @BeforeAll
    public static void setUp() throws Throwable {
        setup(ext);
        setupBridge(ext);
        registerNeoTokenBridge();
        tokenTestContract = ext.getDeployedContract(TokenTestContract.class);
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
    public void testConcatTokenOpData() throws IOException {
        Hash160 neoN3Token = new Hash160("0xEf4073A0F2b305a38EC4050e4d3d28bC40eA63F5");
        Hash160 neoXToken = new Hash160("0x5615dEB798BB3E4dFa0139dFa1b3D433Cc23b72f");
        BigInteger nonce = new BigInteger("330500");
        String nonceHex = toHexStringNoPrefix(nonce);
        assertThat(nonceHex, is("50b04"));
        Hash160 to = new Hash160("0xD44304966f6e74cfd0E2215649D5C57892BfBAaB");
        BigInteger value = new BigInteger("1234567890");
        String valueHex = toHexStringNoPrefix(value);
        assertThat(valueHex, is("499602d2"));

        String expectedConcatenatedData = "ef4073a0f2b305a38ec4050e4d3d28bc40ea63f5" +
                "5615deb798bb3e4dfa0139dfa1b3d433cc23b72f" +
                "0000000000000000000000000000000000000000000000000000000000050b04" +
                "d44304966f6e74cfd0e2215649d5c57892bfbaab" +
                "00000000000000000000000000000000000000000000000000000000499602d2";

        String concatenated = tokenTestContract.callInvokeFunction("concatTokenBridgeOpData",
                        asList(hash160(neoN3Token), hash160(neoXToken), integer(nonce), hash160(to), integer(value)))
                .getInvocationResult().getFirstStackItem().getHexString();
        assertThat(concatenated, is(expectedConcatenatedData));

        String hashed = tokenTestContract.callInvokeFunction("hashTokenBridge",
                        asList(hash160(neoN3Token), hash160(neoXToken), integer(nonce), hash160(to), integer(value)))
                .getInvocationResult().getFirstStackItem().getHexString();
        assertThat(hashed, is(sha256HexNoPrefix(hexStringToByteArray(expectedConcatenatedData))));
        assertThat(hashed, is("5dcc8d59cfb9446288dc79f3e2a776d05e7bce0b82efe28ec554d6dcb08acda4"));
    }

    @Order(0)
    @Test
    public void testNeoDeposit() throws Throwable {
        assertThat(neoToken.getBalanceOf(bridge.getScriptHash()), is(BigInteger.ZERO));

        Hash160 recipientOnNeoX_1 = new Hash160("0xF3D4D6320dd41f14B8Fa6550a6F33c46c6F44407");
        Hash160 recipientOnNeoX_2 = new Hash160("0x3220a7ee654E1f84f13E8021B7E2b09775E1BDf2");

        BigInteger amount_1 = new BigInteger("355");
        BigInteger amount_2 = new BigInteger("445");

        State startingDepositState = bridge.getTokenBridge(neoN3NeoTokenHash).depositState;
        assertThat(startingDepositState.root, is(Hash256.ZERO));
        assertThat(startingDepositState.nonce, is(BigInteger.ZERO));

        // Create deposit to give the bridge some NEO funds.
        Hash256 depositTx_1 = bridge.depositToken(alice, neoN3NeoTokenHash, recipientOnNeoX_1, amount_1);
        Await.waitUntilTransactionIsExecuted(depositTx_1, neow3j);
        Hash256 depositTx_2 = bridge.depositToken(alice, neoN3NeoTokenHash, recipientOnNeoX_2, amount_2);
        Await.waitUntilTransactionIsExecuted(depositTx_2, neow3j);

        NeoApplicationLog.Execution exec_2 =
                neow3j.getApplicationLog(depositTx_2).send().getApplicationLog().getFirstExecution();
        assertThat(exec_2.getState(), is(NeoVMStateType.HALT));

        Notification depositNotification = exec_2.getNotification(4);
        assertThat(depositNotification.getEventName(), is("TokenDeposit"));
        List<StackItem> depositState = depositNotification.getState().getList();
        assertThat(depositState.get(0).getAddress(), is(neoN3NeoTokenHash.toAddress()));
        assertThat(depositState.get(1).getAddress(), is(neoXNeoTokenHash.toAddress()));
        assertThat(depositState.get(2).getInteger(), is(new BigInteger("2")));

        String tokenDepositRoot = prependHexPrefix(depositState.get(7).getHexString());
        assertThat(tokenDepositRoot, is("0x6c995cd010e797f62716b58053fbfbf4834c68d96c5f0361bf49186cbef6d457"));

        BigInteger collectedFees = bridge.tokenDepositFee(neoN3NeoTokenHash).multiply(BigInteger.valueOf(2));
        assertThat(gasToken.getBalanceOf(bridge.getScriptHash()), greaterThan(collectedFees));
        assertThat(neoToken.getBalanceOf(bridge.getScriptHash()), is(amount_1.add(amount_2)));
    }

    @Order(1)
    @Test
    public void testNeoWithdrawal() throws Throwable {
        BigInteger balanceBefore = neoToken.getBalanceOf(bridge.getScriptHash());
        assertThat(balanceBefore, greaterThan(BigInteger.valueOf(600)));

        Hash160 recipientOnNeoN3_1 = new Hash160("0xDA1fE5cf6Eb14785aA9d4dCC7bc87aab7DEcb625");
        BigInteger amount_1 = new BigInteger("12");
        Hash160 recipientOnNeoN3_2 = new Hash160("0xC17940c0bf2A801266f3669c19E0c594e751f868");
        BigInteger amount_2 = new BigInteger("1");
        Hash160 recipientOnNeoN3_3 = new Hash160("0x278bc15652D2cBF8a1a098843a659Cf300760e2e");
        BigInteger amount_3 = new BigInteger("196");

        State withdrawalState_before = bridge.getTokenBridge(neoN3NeoTokenHash).withdrawalState;
        String newRoot_1 = computeNewTokenRootNoPrefix(withdrawalState_before.root.toString(), neoN3NeoTokenHash,
                neoXNeoTokenHash, BigInteger.ONE, recipientOnNeoN3_1, amount_1);
        String newRoot_2 = computeNewTokenRootNoPrefix(newRoot_1, neoN3NeoTokenHash, neoXNeoTokenHash,
                BigInteger.valueOf(2), recipientOnNeoN3_2, amount_2);
        String newRoot_3 = computeNewTokenRootNoPrefix(newRoot_2, neoN3NeoTokenHash, neoXNeoTokenHash,
                BigInteger.valueOf(3), recipientOnNeoN3_3, amount_3);

        assertThat(prependHexPrefix(newRoot_3),
                is("0x2b1eb8388620f2ba81eeecda0b88c41a35d8aef678cb1eb22ecc488b087d9c99"));

        List<Account> signingValidators = asList(validator1, validator3, validator4, validator5, validator7);
        Hash256 withdrawalTxHash = bridge.withdrawToken(neoN3NeoTokenHash, newRoot_3,
                signMsg(signingValidators, newRoot_3),
                array(
                        array(integer(BigInteger.ONE), hash160(recipientOnNeoN3_1), integer(amount_1)),
                        array(integer(BigInteger.valueOf(2)), hash160(recipientOnNeoN3_2), integer(amount_2)),
                        array(integer(BigInteger.valueOf(3)), hash160(recipientOnNeoN3_3), integer(amount_3)
                        )
                )
        );
        Await.waitUntilTransactionIsExecuted(withdrawalTxHash, neow3j);

        NeoApplicationLog.Execution exec_3 = neow3j.getApplicationLog(withdrawalTxHash).send()
                .getApplicationLog().getFirstExecution();

        assertThat(exec_3.getState(), is(NeoVMStateType.HALT));
        List<Notification> notifications = exec_3.getNotifications();
        assertThat(notifications.get(0).getEventName(), is("TokenWithdrawalRootUpdate"));
        assertThat(notifications.get(0).getState().getList().get(3).getHexString(), is(newRoot_3));
        Notification tokenWithdrawalNotification_3 = notifications.get(7);
        assertThat(tokenWithdrawalNotification_3.getEventName(), is("TokenWithdrawal"));
        assertThat(Hash160.fromAddress(tokenWithdrawalNotification_3.getState().getList().get(0).getAddress()),
                is(neoN3NeoTokenHash));
        assertThat(tokenWithdrawalNotification_3.getState().getList().get(1).getInteger(), is(BigInteger.valueOf(3)));
        assertThat(Hash160.fromAddress(tokenWithdrawalNotification_3.getState().getList().get(2).getAddress()),
                is(recipientOnNeoN3_3));
        assertThat(tokenWithdrawalNotification_3.getState().getList().get(3).getInteger(), is(amount_3));

        State withdrawalState_after = bridge.getTokenBridge(neoN3NeoTokenHash).withdrawalState;
        assertThat(withdrawalState_after.root, is(new Hash256(newRoot_3)));
        assertThat(withdrawalState_after.nonce, is(BigInteger.valueOf(3)));

        assertThat(neoToken.getBalanceOf(bridge.getScriptHash()),
                is(balanceBefore.subtract(amount_1).subtract(amount_2).subtract(amount_3)));
        assertThat(neoToken.getBalanceOf(recipientOnNeoN3_1), is(amount_1));
        assertThat(neoToken.getBalanceOf(recipientOnNeoN3_2), is(amount_2));
        assertThat(neoToken.getBalanceOf(recipientOnNeoN3_3), is(amount_3));
    }

}
