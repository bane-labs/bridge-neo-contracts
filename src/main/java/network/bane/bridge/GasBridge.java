package network.bane.bridge;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.List;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.contracts.StdLib;
import network.bane.structs.Claimable;
import network.bane.structs.Withdrawal;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Runtime.getExecutingScriptHash;
import static network.bane.bridge.StorageConstants.KEY_GAS_DEPOSIT_NONCE;
import static network.bane.bridge.StorageConstants.KEY_GAS_DEPOSIT_ROOT;
import static network.bane.bridge.StorageConstants.PREFIX_GAS_CLAIMABLES;
import static network.bane.lib.BridgeLib.computeNewRoot;
import static network.bane.lib.GasBridgeLib.hashGasBridgeOp;

public class GasBridge {

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

    static void addGasClaimable(Withdrawal withdrawal) {
        new StorageMap(BridgeContract.ctx, PREFIX_GAS_CLAIMABLES).put(withdrawal.nonce,
                new StdLib().serialize(new Claimable(withdrawal.to, withdrawal.amount)));
    }

    static void depositGas(Hash160 from, Hash160 to, int amount) {
        if (to == null || !Hash160.isValid(to) || to.isZero()) abort("Invalid recipient.");
        if (from == null || !Hash160.isValid(from) || from.isZero()) abort("Invalid sender.");
        int depositFee = BridgeContract.gasDepositFee();
        Hash160 executingScriptHash = getExecutingScriptHash();
        if (executingScriptHash.equals(from)) abort("Invalid sender.");

        if (amount < BridgeContract.minGasDeposit()) abort("Deposit amount is too low.");
        if (amount > BridgeContract.maxGasDeposit()) abort("Deposit amount is too high.");

        int transferAmount = amount + depositFee;
        if (!BridgeContract.gasToken.transfer(from, executingScriptHash, transferAmount, null)) {
            abort("Gas transfer failed.");
        }
        // Update the Gas bridge deposit state.
        int newNonce = GasBridge.incrementGasDepositNonce();
        ByteString depositHash = hashGasBridgeOp(BridgeContract.cryptoLib, newNonce, amount, to);
        ByteString newRoot = computeNewRoot(BridgeContract.cryptoLib, BridgeContract.baseMap.get(KEY_GAS_DEPOSIT_ROOT),
                depositHash);
        BridgeContract.baseMap.put(KEY_GAS_DEPOSIT_ROOT, newRoot);
        BridgeContract.onGasDeposit.fire(newNonce, amount, to, from, depositHash, newRoot);
    }

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
}
