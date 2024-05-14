package network.bane.bridge;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.List;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.contracts.StdLib;
import network.bane.structs.Claimable;
import network.bane.structs.Withdrawal;

import static io.neow3j.devpack.Runtime.getExecutingScriptHash;
import static network.bane.bridge.StorageConstants.KEY_GAS_DEPOSIT_NONCE;
import static network.bane.bridge.StorageConstants.PREFIX_GAS_CLAIMABLES;

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
