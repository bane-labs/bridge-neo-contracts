package network.bane.bridge;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.ECPoint;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Hash256;
import io.neow3j.devpack.Helper;
import io.neow3j.devpack.Iterator;
import io.neow3j.devpack.List;
import io.neow3j.devpack.Map;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.constants.FindOptions;
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
        if (token == null || !Hash160.isValid(token) || token.isZero()) abort("Invalid token");
        if (isRegisteredToken(token)) abort("Token already registered");
        StorageMap tokenBridges = new StorageMap(BridgeContract.ctx, PREFIX_TOKEN_BRIDGES);
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

    static boolean isRegisteredToken(Hash160 token) {
        StorageMap tokenBridges = new StorageMap(BridgeContract.ctx, PREFIX_TOKEN_BRIDGES);
        return tokenBridges.get(token) != null;
    }

    static TokenBridge checkRegisteredAndGetTokenBridge(Hash160 token) {
        ByteString serializedTokenBridge = new StorageMap(BridgeContract.ctx, PREFIX_TOKEN_BRIDGES).get(token);
        if (serializedTokenBridge == null) abort("Token not registered");
        return (TokenBridge) new StdLib().deserialize(serializedTokenBridge);
    }

    // endregion
    // region pausing

    static void onlyWhenTokenBridgePaused(Hash160 token) {
        if (!BridgeContract.getTokenBridge(token).paused) abort("Token bridge not paused");
    }

    static void onlyWhenTokenBridgeNotPaused(Hash160 token) {
        if (BridgeContract.getTokenBridge(token).paused) abort("Token bridge paused");
    }

    static void pauseTokenBridge(Hash160 token) {
        TokenBridge tokenBridge = checkRegisteredAndGetTokenBridge(token);
        tokenBridge.paused = true;
        new StorageMap(BridgeContract.ctx, PREFIX_TOKEN_BRIDGES).put(token, new StdLib().serialize(tokenBridge));
    }

    static void unpauseTokenBridge(Hash160 token) {
        TokenBridge tokenBridge = checkRegisteredAndGetTokenBridge(token);
        tokenBridge.paused = false;
        new StorageMap(BridgeContract.ctx, PREFIX_TOKEN_BRIDGES).put(token, new StdLib().serialize(tokenBridge));
    }

    // endregion
    // region deposit

    static void depositToken(Hash160 neoN3Token, Hash160 from, Hash160 to, int amount, int maxFee) {
        Hash160 executingScriptHash = getExecutingScriptHash();
        BridgeImpl.checkDepositParameters(executingScriptHash, from, to);

        TokenBridge tokenBridge = checkRegisteredAndGetTokenBridge(neoN3Token);

        // If the deposit fee is higher than the specified max fee, abort.
        int depositFee = tokenBridge.config.fee;
        if (depositFee > maxFee) abort("Max fee exceeded");

        // Fee payment
        BridgeImpl.payFee(from, depositFee);

        // Token deposit transfer
        FungibleToken tokenContract = new FungibleToken(neoN3Token);
        int receivedAmount = BridgeImpl.transferDepositToken(tokenContract, from, executingScriptHash, amount);

        // Check the received amount with the configured min and max amounts.
        if (receivedAmount < tokenBridge.config.minAmount) abort("Amount below minimum");
        if (receivedAmount > tokenBridge.config.maxAmount) abort("Amount above maximum");

        // Decimal scaling factor calculation
        int decimalScalingFactor = tokenBridge.config.decimalScalingFactor;
        int amountForHashing = BridgeImpl.divideByDecimalFactor(receivedAmount, decimalScalingFactor);

        // Update the token bridge state.
        updateTokenDepositState(tokenBridge, neoN3Token, from, to, amountForHashing);
    }

    static void updateTokenDepositState(TokenBridge tokenBridge, Hash160 neoN3Token, Hash160 from, Hash160 to, int amount) {
        tokenBridge.depositState.nonce++;
        ByteString depositHash =
                TokenBridgeLib.hashTokenBridgeOp(BridgeContract.cryptoLib, neoN3Token, tokenBridge.config.neoXToken,
                        tokenBridge.depositState.nonce, to, amount);
        ByteString newRoot =
                BridgeLib.computeNewRoot(BridgeContract.cryptoLib, tokenBridge.depositState.root, depositHash);
        tokenBridge.depositState.root = newRoot;
        assert tokenBridge.depositState.root == newRoot : "Root not set correctly";
        new StorageMap(BridgeContract.ctx, PREFIX_TOKEN_BRIDGES).put(neoN3Token, new StdLib().serialize(tokenBridge));
        BridgeContract.onTokenDeposit.fire(neoN3Token, tokenBridge.config.neoXToken, tokenBridge.depositState.nonce,
                to, amount, from, depositHash, newRoot);
    }

    // endregion
    // region withdrawal

    static void withdrawToken(Hash160 neoN3Token, ByteString withdrawalRoot, Map<ECPoint, ByteString> signatures,
            List<Withdrawal> withdrawals) {
        // Token registration is checked within getTokenBridge
        TokenBridge tokenBridge = checkRegisteredAndGetTokenBridge(neoN3Token);
        int withdrawalsSize = withdrawals.size();
        if (withdrawalsSize <= 0) abort("At least one withdrawal required");
        if (!subsequentNonces(withdrawals, tokenBridge.withdrawalState.nonce)) {
            abort("Provided withdrawals not subsequent");
        }
        if (!TokenBridgeLib.computeNewTopRoot(BridgeContract.cryptoLib, tokenBridge.withdrawalState.root, neoN3Token,
                tokenBridge.config.neoXToken, withdrawals).equals(withdrawalRoot)) {
            abort("Invalid root");
        }
        if (!managementContract().verifyValidatorSignatures(BridgeContract.linkedChainId(), withdrawalRoot, signatures)) {
            abort("Invalid validator signatures");
        }
        // Update the token state
        tokenBridge.withdrawalState.nonce = withdrawals.get(withdrawalsSize - 1).nonce;
        tokenBridge.withdrawalState.root = withdrawalRoot;
        assert tokenBridge.withdrawalState.root == withdrawalRoot : "Root not set correctly";
        new StorageMap(BridgeContract.ctx, PREFIX_TOKEN_BRIDGES).put(neoN3Token, new StdLib().serialize(tokenBridge));
        BridgeContract.onTokenWithdrawalRootUpdate.fire(neoN3Token, tokenBridge.config.neoXToken,
                tokenBridge.withdrawalState.nonce, tokenBridge.withdrawalState.root);
        // Execute the token transfers
        executeTokenTransfers(neoN3Token, tokenBridge.config.decimalScalingFactor, withdrawals);
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
        if (claimableEntry == null) abort("No claim found");
        Claimable claimable = (Claimable) new StdLib().deserialize(claimableEntry);
        Hash160 to = claimable.to;
        int amount = claimable.amount;

        tokenClaimableMap.delete(nonce);

        if (new FungibleToken(token).transfer(getExecutingScriptHash(), to, amount, null)) {
            BridgeContract.onTokenClaim.fire(token, nonce, to, amount);
        } else {
            abort("Claim transfer failed");
        }
    }

    // endregion
    // region transfer execution

    private static void executeTokenTransfers(Hash160 neoN3Token, int decimalScalingFactor,
            List<Withdrawal> withdrawals) {

        Hash160 executingScriptHash = getExecutingScriptHash();
        int withdrawalsSize = withdrawals.size();
        int scalingFactor = Helper.pow(10, decimalScalingFactor);
        for (int i = 0; i < withdrawalsSize; i++) {
            Withdrawal withdrawal = withdrawals.get(i);
            int transferAmount = withdrawal.amount * scalingFactor;
            withdrawal.amount = transferAmount;
            // If to is a contract, add the withdrawal to the claimable map, otherwise exeucte the transfer.
            if (BridgeHelper.isContract(withdrawal.to)) {
                addTokenClaimable(neoN3Token, withdrawal);
                BridgeContract.onTokenClaimable.fire(neoN3Token, withdrawal.nonce, withdrawal.to, withdrawal.amount);
            } else {
                if (new FungibleToken(neoN3Token).transfer(executingScriptHash, withdrawal.to, transferAmount, null)) {
                    BridgeContract.onTokenWithdrawal.fire(neoN3Token, withdrawal.nonce, withdrawal.to, withdrawal.amount);
                } else {
                    // If the transfer was unsuccessful, add the withdrawal to the claimable map.
                    addTokenClaimable(neoN3Token, withdrawal);
                    BridgeContract.onTokenClaimable.fire(neoN3Token, withdrawal.nonce, withdrawal.to, withdrawal.amount);
                }
            }
        }
    }

    public static Iterator<Hash160> getTokenBridgeTokensIterator() {
        return (Iterator<Hash160>) new StorageMap(BridgeContract.ctx, PREFIX_TOKEN_BRIDGES)
                .find(FindOptions.KeysOnly | FindOptions.RemovePrefix);
    }

    public static List<Hash160> getTokenBridgeTokens() {
        Iterator<Hash160> it = getTokenBridgeTokensIterator();
        List<Hash160> tokenList = new List<>();
        while (it.next()) {
            tokenList.add(it.get());
        }
        return tokenList;
    }

    // endregion

}
