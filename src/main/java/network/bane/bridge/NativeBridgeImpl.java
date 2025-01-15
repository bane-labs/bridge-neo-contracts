package network.bane.bridge;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.ECPoint;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.List;
import io.neow3j.devpack.Map;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.contracts.FungibleToken;
import io.neow3j.devpack.contracts.GasToken;
import io.neow3j.devpack.contracts.StdLib;
import network.bane.lib.NativeBridgeLib;
import network.bane.structs.Claimable;
import network.bane.structs.NativeTokenBridge;
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

    static FungibleToken nativeToken() {
        // Todo mialbu 14.01.25: Replace with storage value once v3 migration code includes the native token address
        //  configuration upon deployment.
        return new GasToken();
    }

    // region pause

    static void onlyWhenNativeBridgePaused() {
        if (!BridgeContract.getNativeBridge().paused) abort("Native bridge is not paused.");
    }

    static void onlyWhenNativeBridgeNotPaused() {
        if (BridgeContract.getNativeBridge().paused) abort("Native bridge is paused.");
    }

    static void pauseNativeBridge() {
        NativeTokenBridge nativeTokenBridge = BridgeContract.getNativeBridge();
        nativeTokenBridge.paused = true;
        BridgeContract.baseMap.put(KEY_NATIVE_BRIDGE, new StdLib().serialize(nativeTokenBridge));
    }

    static void unpauseNativeBridge() {
        NativeTokenBridge nativeTokenBridge = BridgeContract.getNativeBridge();
        nativeTokenBridge.paused = false;
        BridgeContract.baseMap.put(KEY_NATIVE_BRIDGE, new StdLib().serialize(nativeTokenBridge));
    }

    static void onlyWhenDepositsNotPaused() {
        if (BridgeContract.depositsArePaused()) abort("Deposits are paused.");
    }

    static void onlyWhenDepositsPaused() {
        if (!BridgeContract.depositsArePaused()) abort("Deposits are not paused.");
    }

    // endregion
    // region deposit

    static void depositNative(Hash160 from, Hash160 to, int amount, int maxFee) {
        if (to == null || !Hash160.isValid(to) || to.isZero()) abort("Invalid recipient.");
        if (from == null || !Hash160.isValid(from) || from.isZero()) abort("Invalid sender.");
        Hash160 executingScriptHash = getExecutingScriptHash();
        if (executingScriptHash.equals(from)) abort("Invalid sender.");

        NativeTokenBridge nativeTokenBridge = BridgeContract.getNativeBridge();
        if (amount < nativeTokenBridge.config.minAmount) abort("Deposit amount is too low.");
        if (amount > nativeTokenBridge.config.maxAmount) abort("Deposit amount is too high.");

        // If the deposit fee is higher than the specified max fee, abort.
        int depositFee = nativeTokenBridge.config.depositFee;
        if (depositFee > maxFee) abort("Max fee exceeded.");

        // The depositAmount is the amount minus the deposit fee. It is the amount that will be distributed on Neo X.
        int depositAmount = amount - depositFee;
        updateNativeDepositState(nativeTokenBridge, from, to, depositAmount);
        BridgeImpl.addToUnclaimedRewards(depositFee);

        // Distribute the token used for the native bridge
        if (!nativeToken().transfer(from, executingScriptHash, amount, null)) {
            abort("Token transfer failed.");
        }
    }

    static void updateNativeDepositState(NativeTokenBridge nativeTokenBridge, Hash160 from, Hash160 to, int amount) {
        nativeTokenBridge.depositState.nonce++;
        nativeTokenBridge.totalDeposited += amount;
        if (nativeTokenBridge.totalDeposited > nativeTokenBridge.config.maxTotalDeposited) {
            abort("Max total deposited native tokens exceeded. Await governor to increase.");
        }
        ByteString depositHash = hashNativeBridgeOp(BridgeContract.cryptoLib, nativeTokenBridge.depositState.nonce, to, amount);
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
        if (withdrawalsSize <= 0) abort("At least one withdrawal is required.");
        NativeTokenBridge nativeTokenBridge = BridgeContract.getNativeBridge();
        if (!subsequentNonces(withdrawals, nativeTokenBridge.withdrawalState.nonce)) {
            abort("Provided withdrawals are not subsequent.");
        }
        if (!NativeBridgeLib.computeNewTopRoot(BridgeContract.cryptoLib, nativeTokenBridge.withdrawalState.root, withdrawals)
                .equals(withdrawalRoot)) {
            abort("Invalid root.");
        }
        if (!managementContract().verifyValidatorSignatures(linkedChainId(), withdrawalRoot, signatures)) {
            abort("Invalid validator signatures provided.");
        }
        // Update the native bridge state
        nativeTokenBridge.withdrawalState.nonce += withdrawalsSize;
        nativeTokenBridge.withdrawalState.root = withdrawalRoot;
        nativeTokenBridge.totalDeposited -= computeTotalWithdrawnAmount(withdrawals);
        BridgeContract.baseMap.put(KEY_NATIVE_BRIDGE, new StdLib().serialize(nativeTokenBridge));
        BridgeContract.onNativeWithdrawalRootUpdate.fire(nativeTokenBridge.withdrawalState.nonce, nativeTokenBridge.withdrawalState.root);
        // Execute the token transfers
        executeNativeTokenTransfers(withdrawals);
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
        if (claimableEntry == null) abort("No claim for this nonce.");
        Claimable claimable = (Claimable) new StdLib().deserialize(claimableEntry);
        Hash160 to = claimable.to;
        int amount = claimable.amount;

        nativeClaimableMap.delete(nonce);

        if (nativeToken().transfer(getExecutingScriptHash(), to, amount, null)) {
            BridgeContract.onNativeClaim.fire(nonce, to, amount);
        } else {
            abort("Claim transfer failed.");
        }
    }

    static void addNativeClaimable(Withdrawal withdrawal) {
        new StorageMap(BridgeContract.ctx, PREFIX_NATIVE_CLAIMABLES).put(withdrawal.nonce,
                new StdLib().serialize(new Claimable(withdrawal.to, withdrawal.amount)));
    }

    // endregion
    // region transfer execution

    static void executeNativeTokenTransfers(List<Withdrawal> withdrawals) {
        int withdrawalsSize = withdrawals.size();
        Hash160 executingScriptHash = getExecutingScriptHash();
        for (int i = 0; i < withdrawalsSize; i++) {
            Withdrawal withdrawal = withdrawals.get(i);
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
        NativeTokenBridge nativeTokenBridge = BridgeContract.getNativeBridge();
        if (newMaxTotalDeposited < nativeTokenBridge.totalDeposited) {
            abort("New value must be greater or equal to the total amount deposited.");
        }
        nativeTokenBridge.config.maxTotalDeposited = newMaxTotalDeposited;
        BridgeContract.baseMap.put(KEY_NATIVE_BRIDGE, new StdLib().serialize(nativeTokenBridge));
    }

    // endregion

}
