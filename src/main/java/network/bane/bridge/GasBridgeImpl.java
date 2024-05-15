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

    // region deposit

    static void depositGas(Hash160 from, Hash160 to, int amount) {
        if (to == null || !Hash160.isValid(to) || to.isZero()) abort("Invalid recipient.");
        if (from == null || !Hash160.isValid(from) || from.isZero()) abort("Invalid sender.");
        int depositFee = BridgeContract.getGasBridge().config.depositFee;
        Hash160 executingScriptHash = getExecutingScriptHash();
        if (executingScriptHash.equals(from)) abort("Invalid sender.");

        int transferAmount = amount + depositFee;
        if (!BridgeContract.gasToken.transfer(from, executingScriptHash, transferAmount, null)) {
            abort("Gas transfer failed.");
        }
        updateGasDepositState(from, to, amount);
    }

    static void updateGasDepositState(Hash160 from, Hash160 to, int amount) {
        GasBridge gasBridge = BridgeContract.getGasBridge();
        if (amount < gasBridge.config.minAmount) abort("Deposit amount is too low.");
        if (amount > gasBridge.config.maxAmount) abort("Deposit amount is too high.");
        gasBridge.depositState.nonce++;
        ByteString depositHash = hashGasBridgeOp(BridgeContract.cryptoLib, gasBridge.depositState.nonce, amount, to);
        gasBridge.depositState.root =
                computeNewRoot(BridgeContract.cryptoLib, gasBridge.depositState.root, depositHash);
        BridgeContract.baseMap.put(KEY_GAS_BRIDGE, new StdLib().serialize(gasBridge));
        BridgeContract.onGasDeposit.fire(gasBridge.depositState.nonce, amount, to, from, depositHash,
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

        gasBridge.withdrawalState.nonce += withdrawalsSize;
        gasBridge.withdrawalState.root = withdrawalRoot;
        BridgeContract.baseMap.put(KEY_GAS_BRIDGE, new StdLib().serialize(gasBridge));
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
