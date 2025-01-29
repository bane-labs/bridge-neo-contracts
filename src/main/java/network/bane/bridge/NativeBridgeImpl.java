package network.bane.bridge;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.ECPoint;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Hash256;
import io.neow3j.devpack.Helper;
import io.neow3j.devpack.List;
import io.neow3j.devpack.Map;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.contracts.FungibleToken;
import io.neow3j.devpack.contracts.StdLib;
import network.bane.lib.NativeBridgeLib;
import network.bane.structs.Claimable;
import network.bane.structs.NativeTokenBridgeV3;
import network.bane.structs.State;
import network.bane.structs.Withdrawal;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Runtime.getExecutingScriptHash;
import static network.bane.bridge.BridgeContract.linkedChainId;
import static network.bane.bridge.BridgeHelper.managementContract;
import static network.bane.bridge.StorageConstants.KEY_NATIVE_BRIDGE;
import static network.bane.bridge.StorageConstants.PREFIX_NATIVE_CLAIMABLES;
import static network.bane.lib.BridgeLib.computeNewRoot;
import static network.bane.lib.BridgeLib.subsequentNonces;
import static network.bane.lib.NativeBridgeLib.hashNativeBridgeOp;

public class NativeBridgeImpl {

    // region getters

    static boolean nativeBridgeIsSet() {
        ByteString serialized = BridgeContract.baseMap.get(KEY_NATIVE_BRIDGE);
        return serialized != null;
    }

    static NativeTokenBridgeV3 getNativeBridge() {
        ByteString serialized = BridgeContract.baseMap.get(KEY_NATIVE_BRIDGE);
        if (serialized == null) abort("Native bridge not set");
        return (NativeTokenBridgeV3) new StdLib().deserialize(serialized);
    }

    static FungibleToken nativeToken() {
        return new FungibleToken(getNativeBridge().config.nativeToken);
    }

    // endregion
    // region pause

    static void onlyWhenNativeBridgePaused() {
        if (!getNativeBridge().paused) abort("Native bridge not paused");
    }

    static void onlyWhenNativeBridgeNotPaused() {
        if (getNativeBridge().paused) abort("Native bridge paused");
    }

    static void pauseNativeBridge() {
        NativeTokenBridgeV3 nativeTokenBridge = getNativeBridge();
        nativeTokenBridge.paused = true;
        BridgeContract.baseMap.put(KEY_NATIVE_BRIDGE, new StdLib().serialize(nativeTokenBridge));
    }

    static void unpauseNativeBridge() {
        NativeTokenBridgeV3 nativeTokenBridge = getNativeBridge();
        nativeTokenBridge.paused = false;
        BridgeContract.baseMap.put(KEY_NATIVE_BRIDGE, new StdLib().serialize(nativeTokenBridge));
    }

    static void onlyWhenDepositsNotPaused() {
        if (BridgeContract.depositsArePaused()) abort("Deposits paused");
    }

    static void onlyWhenDepositsPaused() {
        if (!BridgeContract.depositsArePaused()) abort("Deposits not paused");
    }

    // endregion
    // region deposit

    static void depositNative(Hash160 from, Hash160 to, int amount, int maxFee) {
        Hash160 executingScriptHash = getExecutingScriptHash();
        BridgeImpl.checkDepositParameters(executingScriptHash, from, to);

        NativeTokenBridgeV3 nativeTokenBridge = getNativeBridge();

        // If the deposit fee is higher than the specified max fee, abort.
        int depositFee = nativeTokenBridge.config.depositFee;
        if (depositFee > maxFee) abort("Max fee exceeded");

        // Fee payment
        BridgeImpl.payFee(from, depositFee);

        // Native token deposit transfer
        int receivedAmount = BridgeImpl.transferDepositToken(nativeToken(), from, executingScriptHash, amount);

        // Check the received amount with the configured min and max deposit amounts.
        if (receivedAmount < nativeTokenBridge.config.minAmount) abort("Deposit amount too low");
        if (receivedAmount > nativeTokenBridge.config.maxAmount) abort("Deposit amount too high");

        int amountForHashing = BridgeImpl.divideByDecimalFactor(receivedAmount,
                nativeTokenBridge.config.decimalScalingFactor);

        // Update the native bridge state.
        updateNativeDepositState(nativeTokenBridge, from, to, amountForHashing);
    }

    static void updateNativeDepositState(NativeTokenBridgeV3 nativeTokenBridge, Hash160 from, Hash160 to, int amount) {
        nativeTokenBridge.depositState.nonce++;
        nativeTokenBridge.totalDeposited += amount;
        if (nativeTokenBridge.totalDeposited > nativeTokenBridge.config.maxTotalDeposited) {
            abort("Max total deposited native tokens exceeded. Wait for governor to increase.");
        }
        ByteString depositHash = hashNativeBridgeOp(BridgeContract.cryptoLib, nativeTokenBridge.depositState.nonce, to,
                amount);
        nativeTokenBridge.depositState.root =
                computeNewRoot(BridgeContract.cryptoLib, nativeTokenBridge.depositState.root, depositHash);
        BridgeContract.baseMap.put(KEY_NATIVE_BRIDGE, new StdLib().serialize(nativeTokenBridge));
        BridgeContract.onNativeDeposit.fire(nativeTokenBridge.depositState.nonce, to, amount, from, depositHash,
                nativeTokenBridge.depositState.root);
    }

    // endregion
    // region withdrawal

    static void withdrawNative(ByteString withdrawalRoot, Map<ECPoint, ByteString> signatures,
            List<Withdrawal> withdrawals) {

        int withdrawalsSize = withdrawals.size();
        if (withdrawalsSize <= 0) abort("At least one withdrawal required.");
        NativeTokenBridgeV3 nativeTokenBridge = getNativeBridge();
        if (!subsequentNonces(withdrawals, nativeTokenBridge.withdrawalState.nonce)) {
            abort("Provided withdrawals not subsequent");
        }
        if (!NativeBridgeLib.computeNewTopRoot(BridgeContract.cryptoLib, nativeTokenBridge.withdrawalState.root,
                        withdrawals)
                .equals(withdrawalRoot)) {
            abort("Invalid root");
        }
        if (!managementContract().verifyValidatorSignatures(linkedChainId(), withdrawalRoot, signatures)) {
            abort("Invalid validator signatures");
        }
        // Update the native bridge state
        nativeTokenBridge.withdrawalState.nonce += withdrawalsSize;
        nativeTokenBridge.withdrawalState.root = withdrawalRoot;
        nativeTokenBridge.totalDeposited -= computeTotalWithdrawnAmount(withdrawals);
        BridgeContract.baseMap.put(KEY_NATIVE_BRIDGE, new StdLib().serialize(nativeTokenBridge));
        BridgeContract.onNativeWithdrawalRootUpdate.fire(nativeTokenBridge.withdrawalState.nonce,
                nativeTokenBridge.withdrawalState.root);
        // Execute the token transfers
        executeNativeTokenTransfers(withdrawals, nativeTokenBridge.config.decimalScalingFactor);
    }

    /**
     * Loops through the withdrawals and computes the total amount that is withdrawn.
     * <p>
     * An alternative would have been utilizing the executeNativeTokenTransfers method looping through the withdrawals
     * and accumulating the total amount that will be withdrawn. However, this would have meant that the nativeBridge
     * state is updated only after the transfers have been executed, which is not good practice. Also, the number of
     * batched withdrawals is limited and looping twice does not incur a significant increase in transaction cost
     * (less than 2300 datoshi per withdrawal with network settings at the time of writing, and it's decreasing, e.g.,
     * it becomes less than 600 datoshi per withdrawal if there are 50 withdrawals batched).
     *
     * @param withdrawals the withdrawals.
     * @return the total amount that will be withdrawn.
     */
    private static int computeTotalWithdrawnAmount(List<Withdrawal> withdrawals) {
        int totalWithdrawnAmount = 0;
        int nrWithdrawals = withdrawals.size();
        for (int i = 0; i < nrWithdrawals; i++) {
            totalWithdrawnAmount += withdrawals.get(i).amount;
        }
        return totalWithdrawnAmount;
    }

    // endregion
    // region claim

    static void claimNative(int nonce) {
        StorageMap nativeClaimableMap = new StorageMap(BridgeContract.ctx, PREFIX_NATIVE_CLAIMABLES);
        ByteString claimableEntry = nativeClaimableMap.get(nonce);
        if (claimableEntry == null) abort("No claim found");
        Claimable claimable = (Claimable) new StdLib().deserialize(claimableEntry);
        Hash160 to = claimable.to;
        int amount = claimable.amount;

        nativeClaimableMap.delete(nonce);

        if (nativeToken().transfer(getExecutingScriptHash(), to, amount, null)) {
            BridgeContract.onNativeClaim.fire(nonce, to, amount);
        } else {
            abort("Claim transfer failed");
        }
    }

    static void addNativeClaimable(Withdrawal withdrawal) {
        new StorageMap(BridgeContract.ctx, PREFIX_NATIVE_CLAIMABLES).put(withdrawal.nonce,
                new StdLib().serialize(new Claimable(withdrawal.to, withdrawal.amount)));
    }

    // endregion
    // region transfer execution

    static void executeNativeTokenTransfers(List<Withdrawal> withdrawals, int decimalScalingFactor) {
        Hash160 executingScriptHash = getExecutingScriptHash();
        int withdrawalsSize = withdrawals.size();
        int scalingFactor = Helper.pow(10, decimalScalingFactor);
        for (int i = 0; i < withdrawalsSize; i++) {
            Withdrawal withdrawal = withdrawals.get(i);
            int transferAmount = withdrawal.amount * scalingFactor;
            withdrawal.amount = transferAmount;
            // If the to address is a contract, add the withdrawal to the claimable map, otherwise exeucte the transfer.
            if (BridgeHelper.isContract(withdrawal.to)) {
                addNativeClaimable(withdrawal);
                BridgeContract.onNativeClaimable.fire(withdrawal.nonce, withdrawal.to, withdrawal.amount);
            } else {
                if (nativeToken().transfer(executingScriptHash, withdrawal.to, withdrawal.amount, null)) {
                    BridgeContract.onNativeWithdrawal.fire(withdrawal.nonce, withdrawal.to, withdrawal.amount);
                } else {
                    // If the transfer was unsuccessful, add the withdrawal to the claimable map.
                    addNativeClaimable(withdrawal);
                    BridgeContract.onNativeClaimable.fire(withdrawal.nonce, withdrawal.to, withdrawal.amount);
                }
            }
        }
    }

    static void setMaxTotalDepositedNative(int newMaxTotalDeposited) {
        NativeTokenBridgeV3 nativeTokenBridge = getNativeBridge();
        if (newMaxTotalDeposited < nativeTokenBridge.totalDeposited) {
            abort("New value must be greater or equal to the total amount deposited.");
        }
        nativeTokenBridge.config.maxTotalDeposited = newMaxTotalDeposited;
        BridgeContract.baseMap.put(KEY_NATIVE_BRIDGE, new StdLib().serialize(nativeTokenBridge));
    }

    static void setNativeBridge(Hash160 tokenForNativeBridge, int decimalsOnLinkedChain, int depositFee,
            int minAmount, int maxAmount, int maxWithdrawals, int maxTotalDeposited) {

        if (nativeBridgeIsSet()) abort("Native bridge already set");

        ByteString zeroHash = Hash256.zero().toByteString();
        State newDepositState = new State(0, zeroHash);
        State newWithdrawalState = new State(0, zeroHash);

        NativeTokenBridgeV3.NativeTokenConfigV3 nativeTokenConfig =
                new NativeTokenBridgeV3.NativeTokenConfigV3(tokenForNativeBridge, decimalsOnLinkedChain,
                        depositFee, minAmount, maxAmount, maxWithdrawals, maxTotalDeposited);
        // The config validity is checked in the NativeTokenBridge validity check.

        NativeTokenBridgeV3 nativeBridge = new NativeTokenBridgeV3(true, 0, newDepositState, newWithdrawalState,
                nativeTokenConfig);
        if (!NativeTokenBridgeV3.isValid(nativeBridge)) abort("Invalid native bridge");

        ByteString serialize = new StdLib().serialize(nativeBridge);
        BridgeContract.baseMap.put(KEY_NATIVE_BRIDGE, serialize);
    }

    // endregion

}
