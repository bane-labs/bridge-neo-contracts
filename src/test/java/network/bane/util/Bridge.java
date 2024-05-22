package network.bane.util;

import io.neow3j.contract.ContractManagement;
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
import network.bane.util.structs.GasBridge;
import network.bane.util.structs.State;
import network.bane.util.structs.TokenBridge;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.types.ContractParameter.map;
import static java.util.Arrays.asList;
import static network.bane.util.TestHelper.governor;
import static network.bane.util.TestHelper.owner;
import static network.bane.util.TestHelper.relayer;
import static network.bane.util.TestHelper.securityGuard;

public class Bridge extends SmartContractHelper {

    public Bridge(Hash160 scriptHash, Neow3j neow3j) {
        super(scriptHash, neow3j);
    }

    // region update

    public Hash256 update(NefFile newNefFile, ContractManifest newManifest, ContractParameter data) throws Throwable {
        return update(owner, newNefFile, newManifest, data);
    }

    public Hash256 update(Account sender, NefFile newNefFile, ContractManifest newManifest, ContractParameter data) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
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
                        invokeFunction("update", byteArray(newNefFile.toArray()), byteArray(manifestBytes)) :
                        invokeFunction("update", byteArray(newNefFile.toArray()), byteArray(manifestBytes), data);
                return sendAndAwaitExecution(b.signers(calledByEntry(sender)));
            }
        }
    }

    // endregion
    // region pause/unpause

    public Hash256 pause() throws Throwable {
        return pause(securityGuard);
    }

    public Hash256 pause(Account sender) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("pause").signers(signer));
    }

    public Hash256 unpause() throws Throwable {
        return unpause(governor);
    }

    public Hash256 unpause(Account sender) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("unpause").signers(signer));
    }

    public boolean isPaused() throws IOException {
        return callFunctionReturningBool("isPaused");
    }

    // endregion
    // region onNEP17Payment

    public Hash256 depositGasDirectly(Account from, BigInteger amount, Hash160 to, BigInteger minBridgeAmount)
            throws Throwable {
        return depositGasDirectly(from, from.getScriptHash(), amount, to, minBridgeAmount);
    }

    public Hash256 depositGasDirectly(Account sender, Hash160 from, BigInteger amount, Hash160 to,
            BigInteger minBridgeAmount) throws Throwable {
        Signer signer = AccountSigner.none(sender).setAllowedContracts(GasToken.SCRIPT_HASH);
        return sendAndAwaitExecution(
                new GasToken(neow3j)
                        .transfer(from, getScriptHash(), amount, array(hash160(to), integer(minBridgeAmount)))
                        .signers(signer));
    }

    // endregion
    // region gas bridge
    // region gas deposit/withdraw/claim

    public Hash256 depositGas(Account from, Hash160 to, BigInteger amount) throws Throwable {
        return depositGas(from, from.getScriptHash(), to, amount);
    }

    public Hash256 depositGas(Account sender, Hash160 from, Hash160 to, BigInteger amount) throws Throwable {
        Signer signer = AccountSigner.none(sender).setAllowedContracts(GasToken.SCRIPT_HASH);
        return sendAndAwaitExecution(invokeFunction("depositGas", hash160(from), hash160(to), integer(amount))
                .signers(signer));
    }

    public Hash256 withdrawGas(String withdrawalRoot, Map<ContractParameter, ContractParameter> signatures,
            ContractParameter withdrawals) throws Throwable {
        Hash256 txHash = sendAndAwaitExecution(invokeFunction("withdrawGas",
                byteArray(withdrawalRoot),
                map(signatures),
                withdrawals
        ).signers(calledByEntry(relayer)));
        return txHash;
    }

    public Hash256 claimGas(Account sender, BigInteger nonce) throws Throwable {
        Signer signer = AccountSigner.none(sender).setAllowedContracts(GasToken.SCRIPT_HASH);
        return sendAndAwaitExecution(invokeFunction("claimGas", integer(nonce)).signers(signer));
    }

    // endregion
    // region gas bridge configuration/state
    // region gas bridge configuration

    public GasBridge getGasBridge() throws IOException {
        List<StackItem> gasBridgeList = callInvokeFunction("getGasBridge")
                .getInvocationResult().getFirstStackItem().getList();
        boolean paused = gasBridgeList.get(0).getBoolean();
        List<StackItem> depositStateList = gasBridgeList.get(1).getList();
        State depositState = new State(
                depositStateList.get(0).getInteger(),
                new Hash256(depositStateList.get(1).getByteArray())
        );
        List<StackItem> withdrawalStateList = gasBridgeList.get(2).getList();
        State withdrawalState = new State(
                withdrawalStateList.get(0).getInteger(),
                new Hash256(withdrawalStateList.get(1).getByteArray())
        );
        List<StackItem> gasConfigList = gasBridgeList.get(3).getList();
        GasBridge.GasConfig gasConfig = new GasBridge.GasConfig(
                gasConfigList.get(0).getInteger(),
                gasConfigList.get(1).getInteger(),
                gasConfigList.get(2).getInteger(),
                gasConfigList.get(3).getInteger()
        );
        return new GasBridge(paused, depositState, withdrawalState, gasConfig);
    }

    public BigInteger gasDepositFee() throws IOException {
        return getGasBridge().config.fee;
    }

    public Hash256 setGasDepositFee(BigInteger newFee) throws Throwable {
        return setGasDepositFee(governor, newFee);
    }

    public Hash256 setGasDepositFee(Account sender, BigInteger newFee) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("setGasDepositFee", integer(newFee)).signers(signer));
    }

    public BigInteger minGasDeposit() throws IOException {
        return getGasBridge().config.minAmount;
    }

    public Hash256 setMinGasDeposit(BigInteger newMin) throws Throwable {
        return setMinGasDeposit(governor, newMin);
    }

    public Hash256 setMinGasDeposit(Account sender, BigInteger newMin) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("setMinGasDeposit", integer(newMin)).signers(signer));
    }

    public BigInteger maxGasDeposit() throws IOException {
        return getGasBridge().config.maxAmount;
    }

    public Hash256 setMaxGasDeposit(BigInteger newMax) throws Throwable {
        return setMaxGasDeposit(governor, newMax);
    }

    public Hash256 setMaxGasDeposit(Account sender, BigInteger newMax) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("setMaxGasDeposit", integer(newMax)).signers(signer));
    }

    // endregion
    // region gas bridge state

    public BigInteger gasDepositNonce() throws IOException {
        return getGasBridge().depositState.nonce;
    }

    public String gasDepositRoot() throws IOException {
        return Numeric.toHexString(getGasBridge().depositState.root.toArray());
    }

    public BigInteger gasWithdrawalNonce() throws IOException {
        return getGasBridge().withdrawalState.nonce;
    }

    public String gasWithdrawRoot() throws IOException {
        return Numeric.toHexString(getGasBridge().withdrawalState.root.toArray());
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
                new Hash160(tokenConfigList.get(0).getByteArray()),
                tokenConfigList.get(1).getInteger(),
                tokenConfigList.get(2).getInteger(),
                tokenConfigList.get(3).getInteger(),
                tokenConfigList.get(4).getInteger(),
                tokenConfigList.get(5).getInteger(),
                tokenConfigList.get(6).getInteger()
        );
        return new TokenBridge(paused, depositState, withdrawalState, tokenConfig);
    }

    public Hash256 registerToken(Hash160 tokenHash, Hash160 tokenBridgeHash) throws Throwable {
        return registerToken(governor, tokenHash, tokenBridgeHash);
    }

    public Hash256 registerToken(Account sender, Hash160 tokenHash, Hash160 tokenBridgeHash) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("registerToken", hash160(tokenHash), hash160(tokenBridgeHash))
                .signers(signer));
    }

    public Hash256 unregisterToken(Hash160 tokenHash) throws Throwable {
        return unregisterToken(governor, tokenHash);
    }

    public Hash256 unregisterToken(Account sender, Hash160 tokenHash) throws Throwable {
        Signer signer = AccountSigner.calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("unregisterToken", hash160(tokenHash)).signers(signer));
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

    public Hash256 depositToken(Account sender, Hash160 from, Hash160 tokenHash, Hash160 to, BigInteger amount) throws Throwable {
        Signer signer = AccountSigner.none(sender).setAllowedContracts(tokenHash);
        return sendAndAwaitExecution(invokeFunction("depositToken", hash160(from), hash160(tokenHash), hash160(to),
                integer(amount)).signers(signer));
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
        List<StackItem> response = callInvokeFunction("getTokenConfig", asList(hash160(tokenHash)))
                .getInvocationResult().getFirstStackItem().getList();
        return new TokenBridge.TokenConfig(
                new Hash160(response.get(0).getByteArray()),
                response.get(1).getInteger(),
                response.get(2).getInteger(),
                response.get(3).getInteger(),
                response.get(4).getInteger(),
                response.get(5).getInteger(),
                response.get(6).getInteger()
        );
    }

    public BigInteger tokenDepositFee(Hash160 tokenHash) throws IOException {
        return getTokenConfig(tokenHash).fee;
    }

    public Hash256 setTokenDepositFee(Hash160 tokenHash, BigInteger newFee) throws Throwable {
        return setTokenDepositFee(governor, tokenHash, newFee);
    }

    public Hash256 setTokenDepositFee(Account sender, Hash160 tokenHash, BigInteger newFee) throws Throwable {
        AccountSigner signer = calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("setTokenDepositFee", array(hash160(tokenHash)),
                array(integer(newFee)))
                .signers(signer));
    }

    public BigInteger minTokenDeposit(Hash160 tokenHash) throws IOException {
        return callFunctionReturningInt("minTokenDeposit", hash160(tokenHash));
    }

    public Hash256 setMinTokenDeposit(Hash160 tokenHash, BigInteger newMin) throws Throwable {
        return setMinTokenDeposit(governor, tokenHash, newMin);
    }

    public Hash256 setMinTokenDeposit(Account sender, Hash160 tokenHash, BigInteger newMin) throws Throwable {
        AccountSigner signer = calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("setMinTokenDeposit", array(hash160(tokenHash), integer(newMin)))
                .signers(signer));
    }

    public BigInteger maxTokenDeposit(Hash160 tokenHash) throws IOException {
        return callFunctionReturningInt("maxTokenDeposit", hash160(tokenHash));
    }

    public Hash256 setMaxTokenDeposit(Hash160 tokenHash, BigInteger newMax) throws Throwable {
        return setMaxTokenDeposit(governor, tokenHash, newMax);
    }

    public Hash256 setMaxTokenDeposit(Account sender, Hash160 tokenHash, BigInteger newMax) throws Throwable {
        AccountSigner signer = calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("setMaxTokenDeposit", array(hash160(tokenHash), integer(newMax)))
                .signers(signer));
    }

    public BigInteger maxTokenWithdrawals(Hash160 tokenHash) throws IOException {
        return callFunctionReturningInt("maxTokenWithdrawals", hash160(tokenHash));
    }

    public Hash256 setMaxTokenWithdrawals(Hash160 tokenHash, BigInteger newMax) throws Throwable {
        return setMaxTokenWithdrawals(governor, tokenHash, newMax);
    }

    public Hash256 setMaxTokenWithdrawals(Account sender, Hash160 tokenHash, BigInteger newMax) throws Throwable {
        AccountSigner signer = calledByEntry(sender);
        return sendAndAwaitExecution(invokeFunction("setMaxTokenWithdrawals", array(hash160(tokenHash),
                integer(newMax)))
                .signers(signer));
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
    // region bridge management

    public Hash160 management() throws IOException {
        return callFunctionReturningScriptHash("management");
    }

    // endregion

}
