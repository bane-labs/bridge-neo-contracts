package network.bane.bridge;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.ECPoint;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Hash256;
import io.neow3j.devpack.List;
import io.neow3j.devpack.Map;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.contracts.FungibleToken;
import io.neow3j.devpack.contracts.StdLib;
import network.bane.lib.BridgeLib;
import network.bane.lib.TokenBridgeLib;
import network.bane.structs.Claimable;
import network.bane.structs.State;
import network.bane.structs.TokenBridge;
import network.bane.structs.Withdrawal;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Helper.concat;
import static io.neow3j.devpack.Runtime.getExecutingScriptHash;
import static network.bane.bridge.BridgeHelper.managementContract;
import static network.bane.bridge.StorageConstants.PREFIX_TOKEN_BRIDGES;
import static network.bane.lib.BridgeLib.subsequentNonces;

public class TokenBridgeImpl {

    // region registration

    static void registerToken(Hash160 token, TokenBridge.TokenConfig tokenConfig) {
        StorageMap tokenBridges = new StorageMap(BridgeContract.ctx, PREFIX_TOKEN_BRIDGES);
        if (tokenBridges.get(token) != null) abort("Token already registered.");
        ByteString zeroHash = Hash256.zero().toByteString();
        State newDepositState = new State(0, zeroHash);
        State newWithdrawalState = new State(0, zeroHash);
        tokenBridges.put(token, new StdLib().serialize(
                new TokenBridge(
                        false,
                        newDepositState,
                        newWithdrawalState,
                        tokenConfig
                )
        ));
    }

    static void unregisterToken(Hash160 token) {
        new StorageMap(BridgeContract.ctx, PREFIX_TOKEN_BRIDGES).delete(token);
    }

    static TokenBridge checkRegisteredAndGetTokenBridge(Hash160 token) {
        ByteString serializedTokenBridge = new StorageMap(BridgeContract.ctx, PREFIX_TOKEN_BRIDGES).get(token);
        if (serializedTokenBridge == null) abort("Token not registered.");
        return (TokenBridge) new StdLib().deserialize(serializedTokenBridge);
    }

    // endregion
    // region pausing

    static void onlyTokenBridgePaused(Hash160 token) {
        if (!BridgeContract.getTokenBridge(token).paused) abort("Token bridge is unpaused.");
    }

    static void onlyTokenBridgeUnpaused(Hash160 token) {
        if (BridgeContract.getTokenBridge(token).paused) abort("Token bridge is paused.");
    }

    static void pauseTokenBridge(Hash160 token) {
        TokenBridge tokenBridge = checkRegisteredAndGetTokenBridge(token);
        if (tokenBridge.paused) abort("Token bridge already paused.");
        tokenBridge.paused = true;
        new StorageMap(BridgeContract.ctx, PREFIX_TOKEN_BRIDGES).put(token, new StdLib().serialize(tokenBridge));
    }

    static void unpauseTokenBridge(Hash160 token) {
        TokenBridge tokenBridge = checkRegisteredAndGetTokenBridge(token);
        if (!tokenBridge.paused) abort("Token bridge already unpaused.");
        tokenBridge.paused = false;
        new StorageMap(BridgeContract.ctx, PREFIX_TOKEN_BRIDGES).put(token, new StdLib().serialize(tokenBridge));
    }

    // endregion
    // region deposit

    static void depositToken(Hash160 token, Hash160 from, Hash160 to, int amount) {
        if (to == null || !Hash160.isValid(to) || to.isZero()) abort("Invalid to parameter.");
        if (from == null || !Hash160.isValid(from) || from.isZero()) abort("Invalid from parameter.");
        Hash160 executingScriptHash = getExecutingScriptHash();
        if (executingScriptHash.equals(from)) abort("Invalid from parameter.");

        TokenBridge tokenBridge = checkRegisteredAndGetTokenBridge(token);
        if (amount < tokenBridge.config.minAmount) abort("Amount below minimum.");
        if (amount > tokenBridge.config.maxAmount) abort("Amount above maximum.");

        // Pay the fee and transfer the token
        if (!BridgeContract.gasToken.transfer(from, executingScriptHash, tokenBridge.config.fee, null)) {
            abort("Fee transfer failed.");
        }
        if (!new FungibleToken(token).transfer(from, executingScriptHash, amount, null)) {
            abort("Token transfer failed.");
        }
        // Update the token state
        tokenBridge.depositState.nonce++;
        ByteString depositHash =
                TokenBridgeLib.hashTokenBridgeOp(BridgeContract.cryptoLib, token, tokenBridge.config.neoXTokenHash,
                        tokenBridge.depositState.nonce, amount, to);
        ByteString newRoot =
                BridgeLib.computeNewRoot(BridgeContract.cryptoLib, tokenBridge.depositState.root, depositHash);
        tokenBridge.depositState.root = newRoot;
        assert tokenBridge.depositState.root == newRoot : "Root was not set correctly.";
        new StorageMap(BridgeContract.ctx, PREFIX_TOKEN_BRIDGES).put(token, new StdLib().serialize(tokenBridge));
        BridgeContract.onTokenDeposit.fire(token, tokenBridge.depositState.nonce, to, amount, from, depositHash,
                newRoot);
    }

    // endregion
    // region withdrawal

    static void withdrawToken(Hash160 token, ByteString withdrawalRoot, Map<ECPoint, ByteString> signatures,
            List<Withdrawal> withdrawals) {
        // Token registration is checked within getTokenBridge
        TokenBridge tokenBridge = checkRegisteredAndGetTokenBridge(token);
        int withdrawalsSize = withdrawals.size();
        if (withdrawalsSize <= 0) abort("At least one withdrawal is required.");
        if (!subsequentNonces(withdrawals, tokenBridge.withdrawalState.nonce)) {
            abort("Provided withdrawals are not subsequent.");
        }
        if (!TokenBridgeLib.computeNewTopRoot(BridgeContract.cryptoLib, tokenBridge.withdrawalState.root, token,
                tokenBridge.config.neoXTokenHash, withdrawals).equals(withdrawalRoot)) {
            abort("Invalid root.");
        }
        if (!managementContract().verifyValidatorSignatures(signatures, withdrawalRoot)) {
            abort("Invalid validator signatures provided.");
        }
        // Update the token state
        tokenBridge.withdrawalState.nonce = withdrawals.get(withdrawalsSize - 1).nonce;
        tokenBridge.withdrawalState.root = withdrawalRoot;
        assert tokenBridge.withdrawalState.root == withdrawalRoot : "Root was not set correctly.";
        new StorageMap(BridgeContract.ctx, PREFIX_TOKEN_BRIDGES).put(token, new StdLib().serialize(tokenBridge));
        // Execute the token transfers
        executeTokenTransfers(token, tokenBridge.config.tokenType, withdrawals);
    }

    // endregion
    // region claim

    private static void addTokenClaimable(Hash160 token, Withdrawal withdrawal) {
        new StorageMap(BridgeContract.ctx, concat(BridgeContract.PREFIX_TOKEN_CLAIMABLES, token.toByteString()))
                .put(withdrawal.nonce, new StdLib().serialize(new Claimable(withdrawal.to, withdrawal.amount)));
    }

    static void claimToken(Hash160 token, int nonce) {
        StorageMap tokenClaimableMap = new StorageMap(BridgeContract.ctx,
                concat(BridgeContract.PREFIX_TOKEN_CLAIMABLES, token.toByteString()));
        ByteString claimableEntry = tokenClaimableMap.get(nonce);
        if (claimableEntry == null) abort("No claim for this nonce.");
        Claimable claimable = (Claimable) new StdLib().deserialize(claimableEntry);
        Hash160 to = claimable.to;
        int amount = claimable.amount;

        tokenClaimableMap.delete(nonce);
        assert token != BridgeContract.gasToken.getHash() : "Token cannot be the gas token.";
        if (new FungibleToken(token).transfer(getExecutingScriptHash(), to, amount, null)) {
            BridgeContract.onTokenClaim.fire(token, nonce, amount, to);
        } else {
            abort("Claim transfer failed.");
        }
    }

    // endregion
    // region transfer execution

    private static void executeTokenTransfers(Hash160 token, int tokenType, List<Withdrawal> withdrawals) {
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

    // endregion

}
