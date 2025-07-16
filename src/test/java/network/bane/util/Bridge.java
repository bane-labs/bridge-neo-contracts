package network.bane.util;

import io.neow3j.contract.GasToken;
import io.neow3j.contract.NefFile;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.ObjectMapperFactory;
import io.neow3j.protocol.core.response.ContractManifest;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.Signer;
import io.neow3j.transaction.TransactionBuilder;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.utils.Numeric;
import io.neow3j.wallet.Account;
import network.bane.util.helper.SmartContractHelper;
import network.bane.util.structs.MessageBridgeDto;
import network.bane.util.structs.N3MessageDto;
import network.bane.util.structs.NativeBridge;
import network.bane.util.structs.State;
import network.bane.util.structs.TokenBridge;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.types.ContractParameter.any;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.types.ContractParameter.map;
import static java.util.Arrays.asList;
import static network.bane.util.TestHelper.governor;
import static network.bane.util.TestHelper.owner;
import static network.bane.util.TestHelper.relayer;
import static network.bane.util.TestHelper.securityGuard;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_DEPOSIT_FEE;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_MSG_EXEC_MANAGER_SCRIPT_HASH;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_MAX_DEPOSIT;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_MIN_DEPOSIT;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_MSG_EXEC_WINDOW_SECONDS;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_MSG_MAX_BYTES_FOR_SENDING;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_MSG_NR_MSGS_PER_STORING_INVOCATION;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_MSG_SENDING_FEE;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_TOTAL_MAX_DEPOSITED_NATIVE;
import static network.bane.util.structs.TokenBridge.getAsContractParameter;

public class Bridge extends SmartContractHelper {

    public Bridge(Hash160 scriptHash, Neow3j neow3j) {
        super(scriptHash, neow3j);
    }

    // region update

    public Hash256 update(NefFile newNefFile, ContractManifest newManifest, ContractParameter data) throws Throwable {
        return update(owner, newNefFile, newManifest, data);
    }

    public Hash256 update(Account sender, NefFile newNefFile, ContractManifest newManifest, ContractParameter data)
            throws Throwable {
        if (newNefFile == null) {
            throw new IllegalArgumentException("The NEF file cannot be null.");
        } else if (newManifest == null) {
            throw new IllegalArgumentException("The manifest cannot be null.");
        } else {
            byte[] manifestBytes = ObjectMapperFactory.getObjectMapper().writeValueAsBytes(newManifest);
            if (manifestBytes.length > 65535) {
                throw new IllegalArgumentException(String.format("The given contract manifest is too long. Manifest " +
                        "was %d bytes big, but a max of %d bytes is allowed.", manifestBytes.length, 65535));
            } else {
                TransactionBuilder b = data == null ?
                        invokeFunction("update", byteArray(newNefFile.toArray()), byteArray(manifestBytes), any(null)) :
                        invokeFunction("update", byteArray(newNefFile.toArray()), byteArray(manifestBytes), data);
                return sendAndAwaitExecution(b.signers(calledByEntry(sender)));
            }
        }
    }

    // endregion
    // region pause/unpause

    public Hash256 pauseBridge() throws Throwable {
        return pauseBridge(securityGuard);
    }

    public Hash256 pauseBridge(Account sender) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("pauseBridge").signers(signer));
    }

    public Hash256 unpause() throws Throwable {
        return unpause(governor);
    }

    public Hash256 unpause(Account sender) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("unpauseBridge").signers(signer));
    }

    public boolean isPaused() throws IOException {
        return callFunctionReturningBool("isPaused");
    }

    public Hash256 pauseDeposits() throws Throwable {
        return pauseDeposits(governor);
    }

    public Hash256 pauseDeposits(Account sender) throws Throwable {
        AccountSigner signer = calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("pauseDeposits").signers(signer));
    }

    public Hash256 unpauseDeposits() throws Throwable {
        return unpauseDeposits(governor);
    }

    public Hash256 unpauseDeposits(Account sender) throws Throwable {
        AccountSigner signer = calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("unpauseDeposits").signers(signer));
    }

    public boolean depositsArePaused() throws IOException {
        return callFunctionReturningBool("depositsArePaused");
    }

    // endregion
    // region native bridge
    // region native bridge setting

    public Hash256 setDefaultNativeBridge() throws Throwable {
        return setNativeBridge(governor, GasToken.SCRIPT_HASH, 18, DEFAULT_DEPOSIT_FEE, DEFAULT_MIN_DEPOSIT,
                DEFAULT_MAX_DEPOSIT, 100, DEFAULT_TOTAL_MAX_DEPOSITED_NATIVE);
    }

    public Hash256 setNativeBridge(Account sender, Hash160 token, int decimalsOnLinkedChain, BigInteger depositFee,
            BigInteger minAmount, BigInteger maxAmount, int maxWithdrawals, BigInteger maxTotalDeposited)
            throws Throwable {
        return sendAndAwaitExecution(
                invokeFunction("setNativeBridge",
                        hash160(token),
                        integer(decimalsOnLinkedChain),
                        integer(depositFee),
                        integer(minAmount),
                        integer(maxAmount),
                        integer(maxWithdrawals),
                        integer(maxTotalDeposited)
                ).signers(calledByEntry(sender)));
    }

    // endregion
    // region native bridge pausing

    public Hash256 pauseNativeBridge() throws Throwable {
        return pauseNativeBridge(securityGuard);
    }

    public Hash256 pauseNativeBridge(Account sender) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("pauseNativeBridge").signers(signer));
    }

    public Hash256 unpauseNativeBridge() throws Throwable {
        return unpauseNativeBridge(governor);
    }

    public Hash256 unpauseNativeBridge(Account sender) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("unpauseNativeBridge").signers(signer));
    }

    public boolean nativeBridgeIsPaused() throws IOException {
        return getNativeBridge().paused;
    }

    // endregion
    // region native deposit/withdraw/claim

    public Hash256 depositNative(Account from, Hash160 to, BigInteger amount) throws Throwable {
        return depositNative(from, to, amount, DEFAULT_MIN_DEPOSIT);
    }

    public Hash256 depositNative(Account from, Hash160 to, BigInteger amount, BigInteger maxFee) throws Throwable {
        return depositNative(from, from.getScriptHash(), to, amount, maxFee);
    }

    public Hash256 depositNative(Account sender, Hash160 from, Hash160 to, BigInteger amount) throws Throwable {
        return depositNative(sender, from, to, amount, DEFAULT_DEPOSIT_FEE);
    }

    public Hash256 depositNative(Account sender, Hash160 from, Hash160 to, BigInteger amount, BigInteger maxFee)
            throws Throwable {
        Signer signer = AccountSigner.none(sender).setAllowedContracts(GasToken.SCRIPT_HASH);
        return sendAndAwaitExecution(
                invokeFunction("depositNative",
                        hash160(from),
                        hash160(to),
                        integer(amount),
                        integer(maxFee)
                ).signers(signer));
    }

    public Hash256 depositNative(Account sender, Account from, Hash160 to, BigInteger amount, BigInteger maxFee,
            Account feeSponsor) throws Throwable {
        Signer senderSigner = AccountSigner.none(sender);
        Signer fromSigner = AccountSigner.none(from).setAllowedContracts(GasToken.SCRIPT_HASH);
        Signer feeSponsorSigner = AccountSigner.none(feeSponsor).setAllowedContracts(GasToken.SCRIPT_HASH);
        return sendAndAwaitExecution(
                invokeFunction("depositNative",
                        hash160(from),
                        hash160(to),
                        integer(amount),
                        integer(maxFee),
                        hash160(feeSponsor)
                ).signers(senderSigner, fromSigner, feeSponsorSigner));
    }

    public Hash256 withdrawNative(String withdrawalRoot, Map<ContractParameter, ContractParameter> signatures,
            ContractParameter withdrawals) throws Throwable {
        Hash256 txHash = sendAndAwaitExecution(
                invokeFunction("withdrawNative",
                        byteArray(withdrawalRoot),
                        map(signatures),
                        withdrawals
                ).signers(calledByEntry(relayer)));
        return txHash;
    }

    public Hash256 claimNative(Account sender, BigInteger nonce) throws Throwable {
        Signer signer = AccountSigner.none(sender).setAllowedContracts(GasToken.SCRIPT_HASH);
        return sendAndAwaitExecution(invokeFunction("claimNative", integer(nonce)).signers(signer));
    }

    // endregion
    // region native bridge configuration/state
    // region native bridge configuration

    public Hash160 nativeToken() throws IOException {
        return callFunctionReturningScriptHash("nativeToken");
    }

    public NativeBridge getNativeBridge() throws IOException {
        List<StackItem> nativeBridgeList = callInvokeFunction("getNativeBridge")
                .getInvocationResult().getFirstStackItem().getList();
        boolean paused = nativeBridgeList.get(0).getBoolean();
        BigInteger totalDeposited = nativeBridgeList.get(1).getInteger();
        List<StackItem> depositStateList = nativeBridgeList.get(2).getList();
        State depositState = new State(
                depositStateList.get(0).getInteger(),
                new Hash256(depositStateList.get(1).getByteArray())
        );
        List<StackItem> withdrawalStateList = nativeBridgeList.get(3).getList();
        State withdrawalState = new State(
                withdrawalStateList.get(0).getInteger(),
                new Hash256(withdrawalStateList.get(1).getByteArray())
        );
        List<StackItem> nativeConfigList = nativeBridgeList.get(4).getList();
        NativeBridge.NativeConfig nativeConfig = new NativeBridge.NativeConfig(
                nativeConfigList.get(0).getInteger(),
                nativeConfigList.get(1).getInteger(),
                nativeConfigList.get(2).getInteger(),
                nativeConfigList.get(3).getInteger().intValue(),
                nativeConfigList.get(4).getInteger(),
                Hash160.fromAddress(nativeConfigList.get(5).getAddress()),
                nativeConfigList.get(6).getInteger().intValue()
        );
        return new NativeBridge(paused, totalDeposited, depositState, withdrawalState, nativeConfig);
    }

    public BigInteger nativeDepositFee() throws IOException {
        return getNativeBridge().config.fee;
    }

    public Hash256 setNativeDepositFee(BigInteger newFee) throws Throwable {
        return setNativeDepositFee(governor, newFee);
    }

    public Hash256 setNativeDepositFee(Account sender, BigInteger newFee) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("setNativeDepositFee", integer(newFee)).signers(signer));
    }

    public BigInteger minNativeDeposit() throws IOException {
        return getNativeBridge().config.minAmount;
    }

    public Hash256 setMinNativeDeposit(BigInteger newMin) throws Throwable {
        return setMinNativeDeposit(governor, newMin);
    }

    public Hash256 setMinNativeDeposit(Account sender, BigInteger newMin) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("setMinNativeDeposit", integer(newMin)).signers(signer));
    }

    public BigInteger maxNativeDeposit() throws IOException {
        return getNativeBridge().config.maxAmount;
    }

    public Hash256 setMaxNativeDeposit(BigInteger newMax) throws Throwable {
        return setMaxNativeDeposit(governor, newMax);
    }

    public Hash256 setMaxNativeDeposit(Account sender, BigInteger newMax) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("setMaxNativeDeposit", integer(newMax)).signers(signer));
    }

    public BigInteger maxTotalDepositedNative() throws IOException {
        return getNativeBridge().config.maxTotalDeposit;
    }

    public Hash256 setMaxTotalDepositedNative(BigInteger newMax) throws Throwable {
        return setMaxTotalDepositedNative(governor, newMax);
    }

    public Hash256 setMaxTotalDepositedNative(Account sender, BigInteger newMax) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("setMaxTotalDepositedNative", integer(newMax)).signers(signer));
    }

    // endregion
    // region native bridge state

    public BigInteger nativeDepositNonce() throws IOException {
        return getNativeBridge().depositState.nonce;
    }

    public String nativeDepositRoot() throws IOException {
        return Numeric.toHexString(getNativeBridge().depositState.root.toArray());
    }

    public BigInteger nativeWithdrawalNonce() throws IOException {
        return getNativeBridge().withdrawalState.nonce;
    }

    public String nativeWithdrawRoot() throws IOException {
        return Numeric.toHexString(getNativeBridge().withdrawalState.root.toArray());
    }

    // endregion
    // endregion
    // endregion
    // region token bridge
    // region token registration

    public TokenBridge getTokenBridge(Hash160 tokenHash) throws IOException {
        List<StackItem> response = callInvokeFunction("getTokenBridge", asList(hash160(tokenHash)))
                .getInvocationResult().getFirstStackItem().getList();
        boolean paused = response.get(0).getBoolean();
        List<StackItem> depositStateList = response.get(1).getList();
        State depositState = new State(
                depositStateList.get(0).getInteger(),
                new Hash256(depositStateList.get(1).getByteArray())
        );
        List<StackItem> withdrawalStateList = response.get(2).getList();
        State withdrawalState = new State(
                withdrawalStateList.get(0).getInteger(),
                new Hash256(withdrawalStateList.get(1).getByteArray())
        );
        List<StackItem> tokenConfigList = response.get(3).getList();
        TokenBridge.TokenConfig tokenConfig = new TokenBridge.TokenConfig(
                Hash160.fromAddress(tokenConfigList.get(0).getAddress()),
                tokenConfigList.get(1).getInteger(),
                tokenConfigList.get(2).getInteger(),
                tokenConfigList.get(3).getInteger(),
                tokenConfigList.get(4).getInteger().intValue(),
                tokenConfigList.get(5).getInteger().intValue()
        );
        return new TokenBridge(paused, depositState, withdrawalState, tokenConfig);
    }

    public Hash256 registerToken(Hash160 neoN3TokenHash, TokenBridge.TokenConfig config) throws Throwable {
        return registerToken(governor, neoN3TokenHash, config);
    }

    public Hash256 registerToken(Account sender, Hash160 neoN3TokenHash, TokenBridge.TokenConfig config)
            throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(
                invokeFunction("registerToken",
                        hash160(neoN3TokenHash),
                        getAsContractParameter(config)
                ).signers(signer));
    }

    // endregion
    // region token pausing

    public Hash256 pauseTokenBridge(Hash160 tokenHash) throws Throwable {
        return pauseTokenBridge(securityGuard, tokenHash);
    }

    public Hash256 pauseTokenBridge(Account sender, Hash160 tokenHash) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("pauseTokenBridge", hash160(tokenHash)).signers(signer));
    }

    public Hash256 unpauseTokenBridge(Hash160 tokenHash) throws Throwable {
        return unpauseTokenBridge(governor, tokenHash);
    }

    public Hash256 unpauseTokenBridge(Account sender, Hash160 tokenHash) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("unpauseTokenBridge", hash160(tokenHash)).signers(signer));
    }

    // endregion
    // region token deposit/withdrawal/claim

    public Hash256 depositToken(Account from, Hash160 tokenHash, Hash160 to, BigInteger amount) throws Throwable {
        return depositToken(from, from.getScriptHash(), tokenHash, to, amount);
    }

    public Hash256 depositToken(Account sender, Hash160 from, Hash160 tokenHash, Hash160 to, BigInteger amount)
            throws Throwable {
        Signer signer = AccountSigner.none(sender).setAllowedContracts(tokenHash, GasToken.SCRIPT_HASH);
        BigInteger fee = getTokenConfig(tokenHash).fee;
        return sendAndAwaitExecution(
                invokeFunction("depositToken",
                        hash160(tokenHash),
                        hash160(from),
                        hash160(to),
                        integer(amount),
                        integer(fee)
                ).signers(signer));
    }

    public Hash256 depositToken(Account sender, Account from, Hash160 tokenHash, Hash160 to, BigInteger amount,
            Account feeSponsor) throws Throwable {
        Signer senderSigner = AccountSigner.none(sender);
        Signer fromSigner = AccountSigner.none(sender).setAllowedContracts(tokenHash);
        Signer feeSponsorSigner = AccountSigner.none(sender).setAllowedContracts(GasToken.SCRIPT_HASH);
        BigInteger fee = getTokenConfig(tokenHash).fee;
        return sendAndAwaitExecution(
                invokeFunction("depositToken",
                        hash160(tokenHash),
                        hash160(from),
                        hash160(to),
                        integer(amount),
                        integer(fee),
                        hash160(feeSponsor)
                ).signers(senderSigner, fromSigner, feeSponsorSigner));
    }

    public Hash256 withdrawToken(Hash160 tokenHash, String withdrawalRoot,
            Map<ContractParameter, ContractParameter> signatures, ContractParameter withdrawals) throws Throwable {
        return withdrawToken(relayer, tokenHash, withdrawalRoot, signatures, withdrawals);
    }

    public Hash256 withdrawToken(Account sender, Hash160 tokenHash, String withdrawalRoot, Map<ContractParameter,
                    ContractParameter> signatures,
            ContractParameter withdrawals) throws Throwable {
        return sendAndAwaitExecution(invokeFunction("withdrawToken", hash160(tokenHash),
                byteArray(withdrawalRoot),
                map(signatures),
                withdrawals
        ).signers(calledByEntry(sender)));
    }

    public Hash256 claimToken(Account sender, Hash160 tokenHash, BigInteger nonce) throws Throwable {
        Signer signer = AccountSigner.none(sender).setAllowedContracts(tokenHash);
        return sendAndAwaitExecution(invokeFunction("claimToken", hash160(tokenHash), integer(nonce)).signers(signer));
    }

    // endregion
    // region token bridge configuration/state
    // region token bridge configuration

    public TokenBridge.TokenConfig getTokenConfig(Hash160 tokenHash) throws IOException {
        return getTokenBridge(tokenHash).config;
    }

    public BigInteger tokenDepositFee(Hash160 tokenHash) throws IOException {
        return getTokenConfig(tokenHash).fee;
    }

    public Hash256 setTokenDepositFee(Map<Hash160, BigInteger> newFees) throws Throwable {
        return setTokenDepositFee(governor, newFees);
    }

    public Hash256 setTokenDepositFee(Account sender, Map<Hash160, BigInteger> newFees) throws Throwable {
        AccountSigner signer = calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("setTokenDepositFee", map(newFees)).signers(signer));
    }

    public BigInteger minTokenDeposit(Hash160 tokenHash) throws IOException {
        return callFunctionReturningInt("minTokenDeposit", hash160(tokenHash));
    }

    public Hash256 setMinTokenDeposit(Map<Hash160, BigInteger> newMins) throws Throwable {
        return setMinTokenDeposit(governor, newMins);
    }

    public Hash256 setMinTokenDeposit(Account sender, Map<Hash160, BigInteger> newMins) throws Throwable {
        AccountSigner signer = calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("setMinTokenDeposit", map(newMins)).signers(signer));
    }

    public BigInteger maxTokenDeposit(Hash160 tokenHash) throws IOException {
        return callFunctionReturningInt("maxTokenDeposit", hash160(tokenHash));
    }

    public Hash256 setMaxTokenDeposit(Map<Hash160, BigInteger> newMaxs) throws Throwable {
        return setMaxTokenDeposit(governor, newMaxs);
    }

    public Hash256 setMaxTokenDeposit(Account sender, Map<Hash160, BigInteger> newMaxs) throws Throwable {
        AccountSigner signer = calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("setMaxTokenDeposit", map(newMaxs)).signers(signer));
    }

    public Integer maxTokenWithdrawals(Hash160 tokenHash) throws IOException {
        return callFunctionReturningInt("maxTokenWithdrawals", hash160(tokenHash)).intValue();
    }

    public Hash256 setMaxTokenWithdrawals(Map<Hash160, Integer> newMaxs) throws Throwable {
        return setMaxTokenWithdrawals(governor, newMaxs);
    }

    public Hash256 setMaxTokenWithdrawals(Account sender, Map<Hash160, Integer> newMaxs) throws Throwable {
        AccountSigner signer = calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("setMaxTokenWithdrawals", map(newMaxs)).signers(signer));
    }

    // endregion
    // region token bridge state

    public BigInteger tokenDepositNonce(Hash160 tokenHash) throws IOException {
        return callFunctionReturningInt("tokenDepositNonce", hash160(tokenHash));
    }

    public String tokenDepositRoot(Hash160 tokenHash) throws IOException {
        return callFunctionReturningString("tokenDepositRoot", hash160(tokenHash));
    }

    public BigInteger tokenWithdrawalNonce(Hash160 tokenHash) throws IOException {
        return callFunctionReturningInt("tokenWithdrawalNonce", hash160(tokenHash));
    }

    public String tokenWithdrawalRoot(Hash160 tokenHash) throws IOException {
        return callFunctionReturningString("tokenWithdrawalRoot", hash160(tokenHash));
    }

    // endregion
    // endregion
    // endregion
    // region message bridge
    // region message bridge setting

    public Hash256 setDefaultMessageBridge() throws Throwable {
        return setMessageBridge(governor, DEFAULT_MSG_SENDING_FEE, DEFAULT_MSG_MAX_BYTES_FOR_SENDING,
                DEFAULT_MSG_NR_MSGS_PER_STORING_INVOCATION, DEFAULT_MSG_EXEC_MANAGER_SCRIPT_HASH,
                DEFAULT_MSG_EXEC_WINDOW_SECONDS);
    }

    public Hash256 setMessageBridge(Account sender, BigInteger sendingFee, int maxMsgSizeForStoring,
            int maxNrMessagesPerStoring, Hash160 executionManager, int executionWindowSeconds) throws Throwable {
        return sendAndAwaitExecution(
                invokeFunction("setMessageBridge",
                        integer(sendingFee),
                        integer(maxMsgSizeForStoring),
                        integer(maxNrMessagesPerStoring),
                        hash160(executionManager),
                        integer(executionWindowSeconds)
                ).signers(calledByEntry(sender)));
    }

    // endregion
    // region message bridge pausing

    public boolean messageBridgeIsPaused() throws IOException {
        return callFunctionReturningBool("messageBridgeIsPaused");
    }

    public Hash256 pauseMessageBridge() throws Throwable {
        return pauseMessageBridge(securityGuard);
    }

    public Hash256 pauseMessageBridge(Account sender) throws Throwable {
        return sendAndAwaitExecution(invokeFunction("pauseMessageBridge").signers(calledByEntry(sender)));
    }

    public Hash256 unpauseMessageBridge() throws Throwable {
        return unpauseMessageBridge(governor);
    }

    public Hash256 unpauseMessageBridge(Account sender) throws Throwable {
        return sendAndAwaitExecution(invokeFunction("unpauseMessageBridge").signers(calledByEntry(sender)));
    }

    // endregion
    // region storing/executing messages

    public Hash256 storeMessages(String n3MessageRoot, Map<ContractParameter, ContractParameter> signatures,
            ContractParameter messages) throws Throwable {
        return storeMessages(relayer, n3MessageRoot, signatures, messages);
    }

    public Hash256 storeMessages(Account sender, String n3MessageRoot,
            Map<ContractParameter, ContractParameter> signatures, ContractParameter messages) throws Throwable {
        return sendAndAwaitExecution(
                invokeFunction("storeMessages", byteArray(n3MessageRoot), map(signatures), messages)
                        .signers(calledByEntry(sender)));
    }


    public N3MessageDto getMessage(int nonce) throws IOException {
        return getMessage(BigInteger.valueOf(nonce));
    }

    public N3MessageDto getMessage(BigInteger nonce) throws IOException {
        List<StackItem> stackItemList = callInvokeFunction("getMessage", asList(integer(nonce))).getInvocationResult()
                .getFirstStackItem().getList();
        List<StackItem> metadataStackItemList = stackItemList.get(0).getList();
        N3MessageDto.N3MessageMetadataDto metadata = new N3MessageDto.N3MessageMetadataDto(
                metadataStackItemList.get(0).getInteger(),
                Hash160.fromAddress(metadataStackItemList.get(1).getAddress())
        );
        return new N3MessageDto(metadata, stackItemList.get(1).getHexString());
    }

    public boolean messageHasBeenExecuted(BigInteger nonce) throws IOException {
        return callFunctionReturningBool("messageHasBeenExecuted", integer(nonce));
    }

    // endregion
    // region message bridge configuration/state
    // region message bridge configuration

    public boolean messageBridgeIsSet() throws IOException {
        return callFunctionReturningBool("messageBridgeIsSet");
    }

    public MessageBridgeDto getMessageBridge() throws IOException {
        List<StackItem> messageBridgeList = callInvokeFunction("getMessageBridge")
                .getInvocationResult().getFirstStackItem().getList();
        boolean paused = messageBridgeList.get(0).getBoolean();
        List<StackItem> evmToN3StateList = messageBridgeList.get(1).getList();
        State evmToN3State = new State(
                evmToN3StateList.get(0).getInteger(),
                new Hash256(evmToN3StateList.get(1).getByteArray())
        );
        List<StackItem> n3ToEvmStateList = messageBridgeList.get(2).getList();
        State n3ToEvmState = new State(
                n3ToEvmStateList.get(0).getInteger(),
                new Hash256(n3ToEvmStateList.get(1).getByteArray())
        );
        List<StackItem> messageConfigList = messageBridgeList.get(3).getList();
        MessageBridgeDto.MessageConfig messageConfig = new MessageBridgeDto.MessageConfig(
                messageConfigList.get(0).getInteger(),
                messageConfigList.get(1).getInteger().intValue(),
                messageConfigList.get(2).getInteger().intValue(),
                Hash160.fromAddress(messageConfigList.get(3).getAddress()),
                messageConfigList.get(4).getInteger().intValue()
        );
        return new MessageBridgeDto(paused, evmToN3State, n3ToEvmState, messageConfig);
    }

    public BigInteger messageSendingFee() throws IOException {
        return callFunctionReturningInt("messageSendingFee");
    }

    public Hash256 setMessageSendingFee(BigInteger newFee) throws Throwable {
        return setMessageSendingFee(governor, newFee);
    }

    public Hash256 setMessageSendingFee(Account sender, BigInteger newFee) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("setMessageSendingFee", integer(newFee)).signers(signer));
    }

    public BigInteger maxBytesForSending() throws IOException {
        return callFunctionReturningInt("maxBytesForSending");
    }

    public Hash256 setMaxBytesForSending(BigInteger newMaxSize) throws Throwable {
        return setMaxBytesForSending(governor, newMaxSize);
    }

    public Hash256 setMaxBytesForSending(Account sender, BigInteger newMaxSize) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(
                invokeFunction("setMaxBytesForSending", integer(newMaxSize)).signers(signer));
    }

    public BigInteger maxNrMessagesForStoring() throws IOException {
        return callFunctionReturningInt("maxNrMessagesForStoring");
    }

    public Hash256 setMaxNrMessagesForStoring(BigInteger newMaxNrMessages) throws Throwable {
        return setMaxNrMessagesForStoring(governor, newMaxNrMessages);
    }

    public Hash256 setMaxNrMessagesForStoring(Account sender, BigInteger newMaxNrMessages) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(
                invokeFunction("setMaxNrMessagesForStoring", integer(newMaxNrMessages)).signers(signer));
    }

    public Hash160 messageExecutionManager() throws IOException {
        return callFunctionReturningScriptHash("messageExecutionManager");
    }

    public Hash256 setMessageExecutionManager(Hash160 newExecutionManager) throws Throwable {
        return setMessageExecutionManager(governor, newExecutionManager);
    }

    public Hash256 setMessageExecutionManager(Account sender, Hash160 newExecutionManager) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("setMessageExecutionManager", hash160(newExecutionManager))
                .signers(signer));
    }

    public BigInteger executionWindowSeconds() throws IOException {
        return callFunctionReturningInt("executionWindowSeconds");
    }

    public Hash256 setExecutionWindowSeconds(BigInteger newExecutionWindowSeconds) throws Throwable {
        return setExecutionWindowSeconds(governor, newExecutionWindowSeconds);
    }

    public Hash256 setExecutionWindowSeconds(Account sender, BigInteger newExecutionWindowSeconds) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(
                invokeFunction("setExecutionWindowSeconds", integer(newExecutionWindowSeconds)).signers(signer));
    }

    // endregion
    // region message bridge state

    public BigInteger messageEvmToN3Nonce() throws IOException {
        return getMessageBridge().evmToN3MessageState.nonce;
    }

    public String messageEvmToN3Root() throws IOException {
        return Numeric.toHexString(getMessageBridge().evmToN3MessageState.root.toArray());
    }

    public BigInteger messageN3ToEvmNonce() throws IOException {
        return getMessageBridge().n3ToEvmMessageState.nonce;
    }

    public String messageN3ToEvmRoot() throws IOException {
        return Numeric.toHexString(getMessageBridge().n3ToEvmMessageState.root.toArray());
    }

    // endregion
    // endregion
    // endregion
    // region bridge management

    public Hash160 management() throws IOException {
        return callFunctionReturningScriptHash("management");
    }

    public BigInteger unclaimedRewards() throws IOException {
        return callFunctionReturningInt("unclaimedRewards");
    }

    // endregion

}
