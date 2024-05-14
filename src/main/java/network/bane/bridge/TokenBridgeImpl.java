package network.bane.bridge;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.List;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.contracts.FungibleToken;
import io.neow3j.devpack.contracts.StdLib;
import network.bane.structs.Claimable;
import network.bane.structs.Withdrawal;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Helper.concat;
import static io.neow3j.devpack.Runtime.getExecutingScriptHash;
import static network.bane.bridge.StorageConstants.PREFIX_TOKEN_CLAIMABLES;

public class TokenBridgeImpl {
    static void onlyTokenBridgePaused(Hash160 token) {
        if (!BridgeContract.getTokenBridge(token).paused) abort("Token bridge is unpaused.");
    }

    static void onlyTokenBridgeUnpaused(Hash160 token) {
        if (BridgeContract.getTokenBridge(token).paused) abort("Token bridge is paused.");
    }

    private static void addTokenClaimable(Hash160 token, Withdrawal withdrawal) {
        new StorageMap(BridgeContract.ctx, concat(PREFIX_TOKEN_CLAIMABLES, token.toByteString()))
                .put(withdrawal.nonce, new StdLib().serialize(new Claimable(withdrawal.to, withdrawal.amount)));
    }

    static void executeTokenTransfers(Hash160 token, int tokenType, List<Withdrawal> withdrawals) {
        assert tokenType == TokenTypeConstants.NEO ||
                tokenType == TokenTypeConstants.NEP17_CAPPED : "Invalid token type.";
        Hash160 executingScriptHash = getExecutingScriptHash();
        int withdrawalsSize = withdrawals.size();
        for (int i = 0; i < withdrawalsSize; i++) {
            Withdrawal withdrawal = withdrawals.get(i);
            // If to is a contract, add the withdrawal to the claimable map, otherwise exeucte the transfer.
            if (BridgeHelper.isContract(withdrawal.to)) {
                addTokenClaimable(token, withdrawal);
                BridgeContract.onTokenClaimable.fire(token, withdrawal.nonce, withdrawal.to, withdrawal.amount);
            } else {
                if (new FungibleToken(token).transfer(executingScriptHash, withdrawal.to, withdrawal.amount,
                        null)) {
                    BridgeContract.onTokenWithdrawal.fire(token, withdrawal.nonce, withdrawal.to,
                            withdrawal.amount);
                } else {
                    // If the transfer was unsuccessful, add the withdrawal to the claimable map.
                    addTokenClaimable(token, withdrawal);
                    BridgeContract.onTokenClaimable.fire(token, withdrawal.nonce, withdrawal.to, withdrawal.amount);
                }
            }
        }
    }
}
