package network.bane.bridge;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.ECPoint;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.List;
import io.neow3j.devpack.Map;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.contracts.StdLib;
import network.bane.lib.GasBridgeLib;
import network.bane.structs.Claimable;
import network.bane.structs.GasBridge;
import network.bane.structs.Withdrawal;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Runtime.getExecutingScriptHash;
import static network.bane.bridge.BridgeHelper.managementContract;
import static network.bane.bridge.StorageConstants.KEY_GAS_BRIDGE;
import static network.bane.bridge.StorageConstants.PREFIX_GAS_CLAIMABLES;
import static network.bane.lib.BridgeLib.computeNewRoot;
import static network.bane.lib.BridgeLib.subsequentNonces;
import static network.bane.lib.GasBridgeLib.hashGasBridgeOp;

public class GasBridgeImpl {

    // region pause

    static void onlyGasBridgePaused() {
        if (!BridgeContract.getGasBridge().paused) abort("Gas bridge is unpaused.");
    }

    static void onlyGasBridgeUnpaused() {
        if (BridgeContract.getGasBridge().paused) abort("Gas bridge is paused.");
    }

    static void pauseGasBridge() {
        GasBridge gasBridge = BridgeContract.getGasBridge();
        gasBridge.paused = true;
        BridgeContract.baseMap.put(KEY_GAS_BRIDGE, new StdLib().serialize(gasBridge));
    }

    static void unpauseGasBridge() {
        GasBridge gasBridge = BridgeContract.getGasBridge();
        gasBridge.paused = false;
        BridgeContract.baseMap.put(KEY_GAS_BRIDGE, new StdLib().serialize(gasBridge));
    }

    // endregion
    // region deposit

    static void depositGas(Hash160 from, Hash160 to, int amount, int maxFee) {
        if (to == null || !Hash160.isValid(to) || to.isZero()) abort("Invalid recipient.");
        if (from == null || !Hash160.isValid(from) || from.isZero()) abort("Invalid sender.");
        Hash160 executingScriptHash = getExecutingScriptHash();
        if (executingScriptHash.equals(from)) abort("Invalid sender.");

        GasBridge gasBridge = BridgeContract.getGasBridge();
        if (amount < gasBridge.config.minAmount) abort("Deposit amount is too low.");
        if (amount > gasBridge.config.maxAmount) abort("Deposit amount is too high.");

        // If the deposit fee is higher than the specified max fee, abort.
        int depositFee = gasBridge.config.depositFee;
        if (depositFee > maxFee) abort("Max fee exceeded.");

        if (!BridgeContract.gasToken.transfer(from, executingScriptHash, amount, null)) {
            abort("Gas transfer failed.");
        }

        // The depositAmount is the amount minus the deposit fee. It is the amount that will be distributed on Neo X.
        int depositAmount = amount - depositFee;
        updateGasDepositState(gasBridge, from, to, depositAmount);
    }

    static void updateGasDepositState(GasBridge gasBridge, Hash160 from, Hash160 to, int amount) {
        gasBridge.depositState.nonce++;
        gasBridge.totalDeposited += amount;
        if (gasBridge.totalDeposited > gasBridge.config.maxTotalDeposited) {
            abort("Max total deposited gas exceeded. Await governor to increase.");
        }
        ByteString depositHash = hashGasBridgeOp(BridgeContract.cryptoLib, gasBridge.depositState.nonce, to, amount);
        gasBridge.depositState.root =
                computeNewRoot(BridgeContract.cryptoLib, gasBridge.depositState.root, depositHash);
        BridgeContract.baseMap.put(KEY_GAS_BRIDGE, new StdLib().serialize(gasBridge));
        BridgeContract.onGasDeposit.fire(gasBridge.depositState.nonce, to, amount, from, depositHash,
                gasBridge.depositState.root);
    }

    // endregion
    // region withdrawal

    static void withdrawGas(ByteString withdrawalRoot, Map<ECPoint, ByteString> signatures,
            List<Withdrawal> withdrawals) {
        int withdrawalsSize = withdrawals.size();
        if (withdrawalsSize <= 0) abort("At least one withdrawal is required.");
        GasBridge gasBridge = BridgeContract.getGasBridge();
        if (!subsequentNonces(withdrawals, gasBridge.withdrawalState.nonce)) {
            abort("Provided withdrawals are not subsequent.");
        }
        if (!GasBridgeLib.computeNewTopRoot(BridgeContract.cryptoLib, gasBridge.withdrawalState.root, withdrawals)
                .equals(withdrawalRoot)) {
            abort("Invalid root.");
        }
        if (!managementContract().verifyValidatorSignatures(signatures, withdrawalRoot)) {
            abort("Invalid validator signatures provided.");
        }
        // Update the gas bridge state
        gasBridge.withdrawalState.nonce += withdrawalsSize;
        gasBridge.withdrawalState.root = withdrawalRoot;
        gasBridge.totalDeposited -= computeTotalWithdrawnAmount(withdrawals);
        BridgeContract.baseMap.put(KEY_GAS_BRIDGE, new StdLib().serialize(gasBridge));
        BridgeContract.onGasWithdrawalRootUpdate.fire(gasBridge.withdrawalState.nonce, gasBridge.withdrawalState.root);
        // Execute the Gas transfers
        executeGasTransfers(withdrawals);
    }

    /**
     * Loops through the withdrawals and computes the total amount that is withdrawn.
     * <p>
     * An alternative would have been utilizing the executeGasTransfers method looping through the withdrawals and
     * accumulating the total amount that will be withdrawn. However, this would have meant that the gasBridge state
     * is updated only after the transfers have been executed, which is not good practice. Also, the number of
     * batched withdrawals is limited and looping twice does not incur a significant increase in transaction cost
     * (less than 2300 datoshi per withdrawal with network settings at the time of writing, and it's decreasing,
     * e.g., it becomes less than 600 datoshi per withdrawal if there are 50 withdrawals batched).
     *
     * @param withdrawals the withdrawals.
     * @return the total amount that will be withdrawn.
     */
    private static int computeTotalWithdrawnAmount(List<Withdrawal> withdrawals) {
        int totalWithdrawnAmount = 0;
        for (int i = 0; i < withdrawals.size(); i++) {
            totalWithdrawnAmount += withdrawals.get(i).amount;
        }
        return totalWithdrawnAmount;
    }

    // endregion
    // region claim

    static void claimGas(int nonce) {
        StorageMap gasClaimableMap = new StorageMap(BridgeContract.ctx, PREFIX_GAS_CLAIMABLES);
        ByteString claimableEntry = gasClaimableMap.get(nonce);
        if (claimableEntry == null) abort("No claim for this nonce.");
        Claimable claimable = (Claimable) new StdLib().deserialize(claimableEntry);
        Hash160 to = claimable.to;
        int amount = claimable.amount;

        gasClaimableMap.delete(nonce);

        if (BridgeContract.gasToken.transfer(getExecutingScriptHash(), to, amount, null)) {
            BridgeContract.onGasClaim.fire(nonce, to, amount);
        } else {
            abort("Claim transfer failed.");
        }
    }

    static void addGasClaimable(Withdrawal withdrawal) {
        new StorageMap(BridgeContract.ctx, PREFIX_GAS_CLAIMABLES).put(withdrawal.nonce,
                new StdLib().serialize(new Claimable(withdrawal.to, withdrawal.amount)));
    }

    // endregion
    // region transfer execution

    static void executeGasTransfers(List<Withdrawal> withdrawals) {
        int withdrawalsSize = withdrawals.size();
        Hash160 executingScriptHash = getExecutingScriptHash();
        for (int i = 0; i < withdrawalsSize; i++) {
            Withdrawal withdrawal = withdrawals.get(i);
            // If the to address is a contract, add the withdrawal to the claimable map, otherwise exeucte the transfer.
            if (BridgeHelper.isContract(withdrawal.to)) {
                addGasClaimable(withdrawal);
                BridgeContract.onGasClaimable.fire(withdrawal.nonce, withdrawal.to, withdrawal.amount);
            } else {
                if (BridgeContract.gasToken.transfer(executingScriptHash, withdrawal.to, withdrawal.amount, null)) {
                    BridgeContract.onGasWithdrawal.fire(withdrawal.nonce, withdrawal.to, withdrawal.amount);
                } else {
                    // If the transfer was unsuccessful, add the withdrawal to the claimable map.
                    addGasClaimable(withdrawal);
                    BridgeContract.onGasClaimable.fire(withdrawal.nonce, withdrawal.to, withdrawal.amount);
                }
            }
        }
    }

    static void setMaxTotalDepositedGas(int newMaxTotalDeposited) {
        GasBridge gasBridge = BridgeContract.getGasBridge();
        if (newMaxTotalDeposited < gasBridge.totalDeposited) {
            abort("New value must be greater or equal to the total amount deposited.");
        }
        gasBridge.config.maxTotalDeposited = newMaxTotalDeposited;
        BridgeContract.baseMap.put(KEY_GAS_BRIDGE, new StdLib().serialize(gasBridge));
    }

    // endregion

}
