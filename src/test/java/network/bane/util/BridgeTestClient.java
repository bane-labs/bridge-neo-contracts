package network.bane.util;

import io.neow3j.contract.GasToken;
import io.neow3j.contract.NefFile;
import io.neow3j.crypto.Base64;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.ContractManifest;
import io.neow3j.protocol.core.response.ContractStorageEntry;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;
import network.bane.client.BridgeClient;
import network.bane.client.interfaces.IWriteCaller;
import network.bane.dto.bridge.TokenBridge;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.transaction.AccountSigner.none;
import static io.neow3j.utils.Numeric.toHexString;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_DEPOSIT_FEE;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_MAX_DEPOSIT_GAS;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_MAX_WITHDRAWALS;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_MIN_DEPOSIT_GAS;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_TOTAL_MAX_DEPOSITED_GAS;

public class BridgeTestClient extends BridgeClient {

    public BridgeTestClient(Hash160 scriptHash, Neow3j neow3j) {
        super(scriptHash, neow3j);
    }

    // General

    public Hash256 update(Account sender, NefFile nefFile, ContractManifest manifest, Object data) throws Throwable {
        return update(nefFile, manifest, data).withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 pauseBridge(Account sender) throws Throwable {
        return pauseBridge().withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 unpauseBridge(Account sender) throws Throwable {
        return unpauseBridge().withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 pauseDeposits(Account sender) throws Throwable {
        return pauseDeposits().withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 unpauseDeposits(Account sender) throws Throwable {
        return unpauseDeposits().withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    // Native bridge

    public IWriteCaller setDefaultNativeBridge() {
        return setNativeBridge(GasToken.SCRIPT_HASH, 18, DEFAULT_DEPOSIT_FEE, DEFAULT_MIN_DEPOSIT_GAS,
                DEFAULT_MAX_DEPOSIT_GAS, DEFAULT_MAX_WITHDRAWALS, DEFAULT_TOTAL_MAX_DEPOSITED_GAS);
    }

    public Hash256 pauseNativeBridge(Account sender) throws Throwable {
        return pauseNativeBridge().withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 unpauseNativeBridge(Account sender) throws Throwable {
        return unpauseNativeBridge().withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 depositNative(Account from, Hash160 to, BigInteger amount) throws Throwable {
        return depositNative(from.getScriptHash(), to, amount, nativeDepositFee())
                .withSigners(none(from).setAllowedContracts(GasToken.SCRIPT_HASH))
                .signSendAndAwait();
    }

    public Hash256 withdrawNative(Account sender, String withdrawalRoot,
            Map<ContractParameter, ContractParameter> signatures, ContractParameter withdrawals) throws Throwable {
        return withdrawNative(withdrawalRoot, signatures, withdrawals).withSigners(calledByEntry(sender))
                .signSendAndAwait();
    }

    public Hash256 claimNative(Account sender, BigInteger nonce) throws Throwable {
        return claimNative(nonce).withSigners(none(sender)).signSendAndAwait();
    }

    public Hash256 setNativeDepositFee(Account sender, BigInteger newValue) throws Throwable {
        return setNativeDepositFee(newValue).withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 setMinNativeDeposit(Account sender, BigInteger newValue) throws Throwable {
        return setMinNativeDeposit(newValue).withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 setMaxNativeDeposit(Account sender, BigInteger newValue) throws Throwable {
        return setMaxNativeDeposit(newValue).withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 setMaxTotalDepositedNative(Account sender, BigInteger newValue) throws Throwable {
        return setMaxTotalDepositedNative(newValue).withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    // Token bridge

    public Hash256 registerToken(Account sender, Hash160 token, TokenBridge.TokenConfig config) throws Throwable {
        return registerToken(token, config).withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 pauseTokenBridge(Account sender, Hash160 token) throws Throwable {
        return pauseTokenBridge(token).withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 unpauseTokenBridge(Account sender, Hash160 token) throws Throwable {
        return unpauseTokenBridge(token).withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 depositToken(Account from, Hash160 n3Token, Hash160 to, BigInteger amount) throws Throwable {
        return depositToken(n3Token, from.getScriptHash(), to, amount, tokenDepositFee(n3Token))
                .withSigners(none(from).setAllowedContracts(n3Token, GasToken.SCRIPT_HASH))
                .signSendAndAwait();
    }

    public Hash256 withdrawToken(Account sender, Hash160 token, String withdrawalRoot,
            Map<ContractParameter, ContractParameter> signatures, ContractParameter withdrawals) throws Throwable {
        return withdrawToken(token, withdrawalRoot, signatures, withdrawals).withSigners(calledByEntry(sender))
                .signSendAndAwait();
    }

    public Hash256 claimToken(Account sender, Hash160 token, BigInteger nonce) throws Throwable {
        return claimToken(token, nonce).withSigners(none(sender)).signSendAndAwait();
    }

    public Hash256 setTokenDepositFee(Account sender, HashMap<Hash160, BigInteger> newValues) throws Throwable {
        return setTokenDepositFee(newValues).withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 setMinTokenDeposit(Account sender, HashMap<Hash160, BigInteger> newValues) throws Throwable {
        return setMinTokenDeposit(newValues).withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 setMaxTokenDeposit(Account sender, HashMap<Hash160, BigInteger> newValues) throws Throwable {
        return setMaxTokenDeposit(newValues).withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 setMaxTokenWithdrawals(Account sender, HashMap<Hash160, Integer> newValues) throws Throwable {
        return setMaxTokenWithdrawals(newValues).withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    // Generic

    public List<ContractStorageEntry> findStorage(String prefixHex) throws IOException {
        return neow3j.findStorage(scriptHash, prefixHex, BigInteger.ZERO).send().getFoundStorage().getStorageEntries();
    }

    public String getStorage(String keyHex) throws IOException {
        byte[] storageBytes = Base64.decode(neow3j.getStorage(scriptHash, keyHex).send().getStorage());
        return toHexString(storageBytes);
    }
}
