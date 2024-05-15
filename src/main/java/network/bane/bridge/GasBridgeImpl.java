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
import network.bane.structs.Withdrawal;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Runtime.getExecutingScriptHash;
import static network.bane.bridge.BridgeHelper.managementContract;
import static network.bane.bridge.StorageConstants.KEY_GAS_DEPOSIT_NONCE;
import static network.bane.bridge.StorageConstants.KEY_GAS_DEPOSIT_ROOT;
import static network.bane.bridge.StorageConstants.KEY_GAS_WITHDRAWAL_NONCE;
import static network.bane.bridge.StorageConstants.KEY_GAS_WITHDRAWAL_ROOT;
import static network.bane.bridge.StorageConstants.PREFIX_GAS_CLAIMABLES;
import static network.bane.lib.BridgeLib.computeNewRoot;
import static network.bane.lib.BridgeLib.subsequentNonces;
import static network.bane.lib.GasBridgeLib.hashGasBridgeOp;

public class GasBridgeImpl {

    // region deposit

    static void depositGas(Hash160 from, Hash160 to, int amount) {
        if (to == null || !Hash160.isValid(to) || to.isZero()) abort("Invalid recipient.");
        if (from == null || !Hash160.isValid(from) || from.isZero()) abort("Invalid sender.");
        int depositFee = BridgeContract.gasDepositFee();
        Hash160 executingScriptHash = getExecutingScriptHash();
        if (executingScriptHash.equals(from)) abort("Invalid sender.");

        int transferAmount = amount + depositFee;
        if (!BridgeContract.gasToken.transfer(from, executingScriptHash, transferAmount, null)) {
            abort("Gas transfer failed.");
        }
        updateGasDepositState(from, to, amount);
    }

    static void updateGasDepositState(Hash160 from, Hash160 to, int amount) {
        if (amount < BridgeContract.minGasDeposit()) abort("Deposit amount is too low.");
        if (amount > BridgeContract.maxGasDeposit()) abort("Deposit amount is too high.");
        // Update the Gas bridge deposit state.
        int newNonce = GasBridgeImpl.incrementGasDepositNonce();
        ByteString depositHash = hashGasBridgeOp(BridgeContract.cryptoLib, newNonce, amount, to);
        ByteString newRoot = computeNewRoot(BridgeContract.cryptoLib, BridgeContract.baseMap.get(KEY_GAS_DEPOSIT_ROOT),
                depositHash);
        BridgeContract.baseMap.put(KEY_GAS_DEPOSIT_ROOT, newRoot);
        BridgeContract.onGasDeposit.fire(newNonce, amount, to, from, depositHash, newRoot);
    }

    /**
     * Increments the Gas deposit nonce and returns the new value.
     *
     * @return the new nonce value.
     */
    static int incrementGasDepositNonce() {
        int nextNonce = BridgeContract.baseMap.getInt(KEY_GAS_DEPOSIT_NONCE) + 1;
        BridgeContract.baseMap.put(KEY_GAS_DEPOSIT_NONCE, nextNonce);
        return nextNonce;
    }

    // endregion
    // region withdrawal

    static void withdrawGas(ByteString withdrawalRoot, Map<ECPoint, ByteString> signatures,
            List<Withdrawal> withdrawals) {
        int withdrawalsSize = withdrawals.size();
        if (withdrawalsSize <= 0) abort("At least one withdrawal is required.");
        if (!subsequentNonces(withdrawals, BridgeContract.gasWithdrawalNonce())) {
            abort("Provided withdrawals are not subsequent.");
        }
        if (!GasBridgeLib.computeNewTopRoot(BridgeContract.cryptoLib, BridgeContract.gasWithdrawalRoot(), withdrawals)
                .equals(withdrawalRoot)) {
            abort("Invalid root.");
        }
        if (!managementContract().verifyValidatorSignatures(signatures, withdrawalRoot)) {
            abort("Invalid validator signatures provided.");
        }

        BridgeContract.baseMap.put(KEY_GAS_WITHDRAWAL_NONCE, withdrawals.get(withdrawalsSize - 1).nonce);
        BridgeContract.baseMap.put(KEY_GAS_WITHDRAWAL_ROOT, withdrawalRoot);
        executeGasTransfers(withdrawals);
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
            BridgeContract.onGasClaim.fire(nonce, amount, to);
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
                BridgeContract.onGasClaimable.fire(withdrawal.nonce, withdrawal.amount, withdrawal.to);
            } else {
                if (BridgeContract.gasToken.transfer(executingScriptHash, withdrawal.to, withdrawal.amount, null)) {
                    BridgeContract.onGasWithdrawal.fire(withdrawal.nonce, withdrawal.amount, withdrawal.to);
                } else {
                    // If the transfer was unsuccessful, add the withdrawal to the claimable map.
                    addGasClaimable(withdrawal);
                    BridgeContract.onGasClaimable.fire(withdrawal.nonce, withdrawal.amount, withdrawal.to);
                }
            }
        }
    }

    // endregion

}
