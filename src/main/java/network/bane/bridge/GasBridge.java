package network.bane.bridge;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.List;
import io.neow3j.devpack.StorageMap;
import network.bane.structs.Withdrawal;

import static io.neow3j.devpack.Helper.concat;
import static io.neow3j.devpack.Helper.toByteArray;
import static io.neow3j.devpack.Runtime.getExecutingScriptHash;
import static network.bane.TestPaddingContract.padToBytes;
import static network.bane.bridge.StorageConstants.KEY_GAS_DEPOSIT_NONCE;
import static network.bane.bridge.StorageConstants.PREFIX_GAS_CLAIMABLES;
import static network.bane.lib.BridgeLib.UINT256_SIZE;

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

    static void addGasClaimable(StorageMap gasClaimableMap, Withdrawal withdrawal) {
        gasClaimableMap.put(withdrawal.nonce, concat(withdrawal.to.toByteArray(),
                padToBytes(toByteArray(withdrawal.amount), UINT256_SIZE)));
    }

    static void executeGasTransfers(List<Withdrawal> withdrawals) {
        StorageMap gasClaimableMap = new StorageMap(BridgeContract.ctx, PREFIX_GAS_CLAIMABLES);
        int withdrawalsSize = withdrawals.size();
        Hash160 executingScriptHash = getExecutingScriptHash();
        for (int i = 0; i < withdrawalsSize; i++) {
            Withdrawal withdrawal = withdrawals.get(i);
            // If the to address is a contract, add the withdrawal to the claimable map, otherwise exeucte the transfer.
            if (BridgeHelper.isContract(withdrawal.to)) {
                addGasClaimable(gasClaimableMap, withdrawal);
                BridgeContract.onGasClaimable.fire(withdrawal.nonce, withdrawal.amount, withdrawal.to);
            } else {
                if (BridgeContract.gasToken.transfer(executingScriptHash, withdrawal.to, withdrawal.amount, null)) {
                    BridgeContract.onGasWithdrawal.fire(withdrawal.nonce, withdrawal.amount, withdrawal.to);
                } else {
                    // If the transfer was unsuccessful, add the withdrawal to the claimable map.
                    addGasClaimable(gasClaimableMap, withdrawal);
                    BridgeContract.onGasClaimable.fire(withdrawal.nonce, withdrawal.amount, withdrawal.to);
                }
            }
        }
    }
}
