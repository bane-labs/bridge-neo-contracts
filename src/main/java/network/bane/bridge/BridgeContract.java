package network.bane.bridge;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.ECPoint;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Hash256;
import io.neow3j.devpack.List;
import io.neow3j.devpack.Map;
import io.neow3j.devpack.Storage;
import io.neow3j.devpack.StorageContext;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.annotations.DisplayName;
import io.neow3j.devpack.annotations.EventParameterNames;
import io.neow3j.devpack.annotations.ManifestExtra;
import io.neow3j.devpack.annotations.OnDeployment;
import io.neow3j.devpack.annotations.OnNEP17Payment;
import io.neow3j.devpack.annotations.Permission;
import io.neow3j.devpack.annotations.Safe;
import io.neow3j.devpack.constants.NativeContract;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.contracts.CryptoLib;
import io.neow3j.devpack.contracts.FungibleToken;
import io.neow3j.devpack.contracts.GasToken;
import io.neow3j.devpack.contracts.StdLib;
import io.neow3j.devpack.events.Event1Arg;
import io.neow3j.devpack.events.Event2Args;
import io.neow3j.devpack.events.Event3Args;
import io.neow3j.devpack.events.Event4Args;
import io.neow3j.devpack.events.Event6Args;
import io.neow3j.devpack.events.Event7Args;
import network.bane.lib.GasBridgeLib;
import network.bane.lib.TokenBridgeLib;
import network.bane.structs.BridgeDeploymentData;
import network.bane.structs.Claimable;
import network.bane.structs.TokenBridge;
import network.bane.structs.Withdrawal;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Helper.concat;
import static io.neow3j.devpack.Runtime.checkWitness;
import static io.neow3j.devpack.Runtime.getCallingScriptHash;
import static io.neow3j.devpack.Runtime.getExecutingScriptHash;
import static network.bane.bridge.BridgeHelper.managementContract;
import static network.bane.bridge.BridgeHelper.onlyGovernor;
import static network.bane.bridge.BridgeHelper.onlyPaused;
import static network.bane.bridge.BridgeHelper.onlyRelayer;
import static network.bane.bridge.BridgeHelper.onlySecurityGuard;
import static network.bane.bridge.BridgeHelper.onlyUnpaused;
import static network.bane.bridge.StorageConstants.PREFIX_TOKEN_BRIDGES;
import static network.bane.bridge.StorageConstants.PREFIX_TOKEN_CLAIMABLES;
import static network.bane.bridge.TokenBridgeImpl.onlyTokenBridgePaused;
import static network.bane.bridge.TokenBridgeImpl.onlyTokenBridgeUnpaused;
import static network.bane.lib.BridgeLib.computeNewRoot;
import static network.bane.lib.BridgeLib.subsequentNonces;
import static network.bane.lib.GasBridgeLib.hashGasBridgeOp;
import static network.bane.bridge.StorageConstants.KEY_BRIDGE_MANAGEMENT;
import static network.bane.bridge.StorageConstants.KEY_GAS_DEPOSIT_FEE;
import static network.bane.bridge.StorageConstants.KEY_GAS_DEPOSIT_MAX_AMOUNT;
import static network.bane.bridge.StorageConstants.KEY_GAS_DEPOSIT_MIN_AMOUNT;
import static network.bane.bridge.StorageConstants.KEY_GAS_DEPOSIT_NONCE;
import static network.bane.bridge.StorageConstants.KEY_GAS_DEPOSIT_ROOT;
import static network.bane.bridge.StorageConstants.KEY_GAS_WITHDRAWAL_NONCE;
import static network.bane.bridge.StorageConstants.KEY_GAS_WITHDRAWAL_ROOT;
import static network.bane.bridge.StorageConstants.KEY_PAUSED;
import static network.bane.bridge.StorageConstants.PREFIX_BASE;
import static network.bane.bridge.StorageConstants.PREFIX_GAS_CLAIMABLES;

@DisplayName("NeoXBridge")
@Permission(nativeContract = NativeContract.GasToken, methods = "transfer")
@Permission(nativeContract = NativeContract.ContractManagement, methods = "update")
@ManifestExtra(key = "Author", value = "BaneLabs")
@ManifestExtra(key = "Target", value = "Neo X TestNet T3")
@ManifestExtra(key = "Description", value = "Contract for bridging GAS and tokens between Neo N3 and Neo X.")
public class BridgeContract {
    static final StorageContext ctx = Storage.getStorageContext();
    private static final CryptoLib cryptoLib = new CryptoLib();
    static final GasToken gasToken = new GasToken();

    // baseMap is used to store gas-related state and general contract information, i.e., management contract and
    // pause status.
    static final StorageMap baseMap = new StorageMap(ctx, PREFIX_BASE);

    // region events
    // region gas bridge events

    @DisplayName("GasDeposit")
    @EventParameterNames({"Nonce", "Amount", "Recipient", "Depositor", "DepositHash", "NewDepositRoot"})
    static Event6Args<Integer, Integer, Hash160, Hash160, ByteString, ByteString> onGasDeposit;

    @DisplayName("GasClaimable")
    @EventParameterNames({"Nonce", "Amount", "Recipient"})
    static Event3Args<Integer, Integer, Hash160> onGasClaimable;

    @DisplayName("GasClaim")
    @EventParameterNames({"Nonce", "Amount", "Recipient"})
    static Event3Args<Integer, Integer, Hash160> onGasClaim;

    @DisplayName("GasWithdrawal")
    @EventParameterNames({"Nonce", "Amount", "Recipient"})
    static Event3Args<Integer, Integer, Hash160> onGasWithdrawal;

    @DisplayName("GasDepositFeeChange")
    @EventParameterNames({"NewFee"})
    static Event1Arg<Integer> onGasDepositFeeChange;

    @DisplayName("MinGasDepositChange")
    @EventParameterNames({"NewMinDeposit"})
    static Event1Arg<Integer> onMinGasDepositChange;

    @DisplayName("MaxGasDepositChange")
    @EventParameterNames({"NewMaxDeposit"})
    static Event1Arg<Integer> onMaxGasDepositChange;

    // endregion
    // region token bridge events

    @DisplayName("TokenRegister")
    @EventParameterNames({"TokenHash", "TokenConfig"})
    static Event2Args<Hash160, TokenBridge.TokenConfig> onTokenRegister;

    @DisplayName("TokenUnregister")
    @EventParameterNames({"TokenHash"})
    static Event1Arg<Hash160> onTokenUnregister;

    @DisplayName("TokenBridgePause")
    @EventParameterNames({"TokenHash"})
    static Event1Arg<Hash160> onTokenBridgePause;

    @DisplayName("TokenBridgeUnpause")
    @EventParameterNames({"TokenHash"})
    static Event1Arg<Hash160> onTokenBridgeUnpause;

    @DisplayName("TokenDeposit")
    @EventParameterNames({"TokenHash", "Nonce", "To", "Value", "Depositor", "DepositHash", "NewDepositRoot"})
    static Event7Args<Hash160, Integer, Hash160, Integer, Hash160, ByteString, ByteString> onTokenDeposit;

    @DisplayName("TokenClaimable")
    @EventParameterNames({"TokenHash", "Nonce", "To", "Value"})
    static Event4Args<Hash160, Integer, Hash160, Integer> onTokenClaimable;

    @DisplayName("TokenClaim")
    @EventParameterNames({"TokenHash", "Nonce", "To", "Value"})
    static Event4Args<Hash160, Integer, Integer, Hash160> onTokenClaim;

    @DisplayName("TokenWithdrawal")
    @EventParameterNames({"TokenHash", "Nonce", "To", "Value"})
    static Event4Args<Hash160, Integer, Hash160, Integer> onTokenWithdrawal;

    @DisplayName("TokenDepositFeeChange")
    @EventParameterNames({"TokenHash", "NewFee"})
    static Event2Args<Hash160, Integer> onTokenDepositFeeChange;

    @DisplayName("MinTokenDepositChange")
    @EventParameterNames({"TokenHash", "NewMinDeposit"})
    static Event2Args<Hash160, Integer> onMinTokenDepositChange;

    @DisplayName("MaxTokenDepositChange")
    @EventParameterNames({"TokenHash", "NewMaxDeposit"})
    static Event2Args<Hash160, Integer> onMaxTokenDepositChange;

    // endregion
    // endregion
    // region deployment/update

    @OnDeployment
    public static void deploy(Object data, boolean isUpdate) {
        if (!isUpdate) {
            BridgeDeploymentData deploymentData = (BridgeDeploymentData) data;
            if (deploymentData.bridgeManagementContract == null ||
                    !Hash160.isValid(deploymentData.bridgeManagementContract))
                abort("Invalid bridge management contract hash.");
            if (deploymentData.gasDepositFee < 0) abort("Deposit fee must be nonnegative.");
            if (deploymentData.minGasDeposit < 0) abort("Minimum deposit must be nonnegative.");
            if (deploymentData.maxGasDeposit < deploymentData.minGasDeposit)
                abort("Maximum deposit must be greater than the minimum deposit.");

            baseMap.put(KEY_BRIDGE_MANAGEMENT, deploymentData.bridgeManagementContract);
            baseMap.put(KEY_GAS_DEPOSIT_FEE, deploymentData.gasDepositFee);
            baseMap.put(KEY_GAS_DEPOSIT_MIN_AMOUNT, deploymentData.minGasDeposit);
            baseMap.put(KEY_GAS_DEPOSIT_MAX_AMOUNT, deploymentData.maxGasDeposit);

            // Initial deposit and withdrawal roots will be zero hashes
            baseMap.put(KEY_GAS_DEPOSIT_ROOT, Hash256.zero());
            baseMap.put(KEY_GAS_WITHDRAWAL_ROOT, Hash256.zero());

            baseMap.put(KEY_GAS_DEPOSIT_NONCE, 0);
            baseMap.put(KEY_GAS_WITHDRAWAL_NONCE, 0);
            baseMap.put(KEY_PAUSED, false);

            if (!checkWitness(managementContract().owner())) {
                abort("Owner must witness the deployment.");
            }
        }
    }

    public static void update(ByteString nef, String manifest) {
        onlyPaused();
        if (!checkWitness(managementContract().owner())) abort("Only the owner can update this contract.");
        new ContractManagement().update(nef, manifest);
    }

    // endregion
    // region pause/unpause

    public static void pause() {
        onlyUnpaused();
        onlySecurityGuard();
        baseMap.put(KEY_PAUSED, true);
    }

    public static void unpause() {
        onlyPaused();
        onlyGovernor();
        baseMap.put(KEY_PAUSED, false);
    }

    @Safe
    public static boolean isPaused() {
        return baseMap.getBoolean(KEY_PAUSED);
    }

    // endregion
    // region OnNEP17Payment

    @OnNEP17Payment
    public static void onNep17Payment(Hash160 from, int amountWithFee, Object data) {
        if (isPaused()) abort("Contract is paused.");
        if (getCallingScriptHash() != gasToken.getHash()) abort("Only GAS is accepted.");
        Hash160 to = (Hash160) data;
        if (to == null || !Hash160.isValid(to)) abort("Invalid recipient data.");
        if (to.isZero()) abort("Recipient must not be zero.");

        int depositFee = gasDepositFee();
        if (amountWithFee < minGasDeposit() + depositFee) abort("Deposit amount is too low.");
        if (amountWithFee > maxGasDeposit() + depositFee) abort("Deposit amount is too high.");
        int depositAmount = amountWithFee - depositFee;

        int nonce = GasBridge.incrementGasDepositNonce();
        ByteString depositHash = hashGasBridgeOp(cryptoLib, nonce, depositAmount, to);
        ByteString newRoot = computeNewRoot(cryptoLib, baseMap.get(KEY_GAS_DEPOSIT_ROOT), depositHash);
        baseMap.put(KEY_GAS_DEPOSIT_ROOT, newRoot);
        onGasDeposit.fire(nonce, depositAmount, to, from, depositHash, newRoot);
    }

    // endregion
    // region gas bridge
    // region gas deposit/claim/withdrawal

    public static void depositGas(Hash160 from, Hash160 to, int depositAmount) {
        Hash160 executingScriptHash = getExecutingScriptHash();
        if (executingScriptHash.equals(from)) abort("Invalid 'from' parameter.");
        if (!gasToken.transfer(from, executingScriptHash, depositAmount + gasDepositFee(), to)) {
            abort("Transfer failed.");
        }
    }

    public static void claimGas(int nonce) {
        if (isPaused()) abort("Contract is paused.");
        StorageMap gasClaimableMap = new StorageMap(ctx, PREFIX_GAS_CLAIMABLES);
        ByteString claimableEntry = gasClaimableMap.get(nonce);
        if (claimableEntry == null) abort("No claim for this nonce.");
        Claimable claimable = (Claimable) new StdLib().deserialize(claimableEntry);
        Hash160 to = claimable.to;
        int amount = claimable.amount;

        gasClaimableMap.delete(nonce);

        if (gasToken.transfer(getExecutingScriptHash(), to, amount, null)) {
            onGasClaim.fire(nonce, amount, to);
        } else {
            abort("Claim transfer failed.");
        }
    }

    public static void withdrawGas(ByteString withdrawalRoot, Map<ECPoint, ByteString> signatures,
            List<Withdrawal> withdrawals) {
        if (isPaused()) abort("Contract is paused.");
        onlyRelayer();
        int withdrawalsSize = withdrawals.size();
        if (withdrawalsSize <= 0) abort("At least one withdrawal is required.");
        if (!subsequentNonces(withdrawals, gasWithdrawalNonce())) abort("Provided withdrawals are not subsequent.");
        if (!GasBridgeLib.computeNewTopRoot(cryptoLib, gasWithdrawalRoot(), withdrawals).equals(withdrawalRoot)) {
            abort("Invalid root.");
        }
        if (!managementContract().verifyValidatorSignatures(signatures, withdrawalRoot)) {
            abort("Invalid validator signatures provided.");
        }

        baseMap.put(KEY_GAS_WITHDRAWAL_NONCE, withdrawals.get(withdrawalsSize - 1).nonce);
        baseMap.put(KEY_GAS_WITHDRAWAL_ROOT, withdrawalRoot);
        GasBridge.executeGasTransfers(withdrawals);
    }

    // endregion
    // region gas bridge configuration

    @Safe
    public static int gasDepositFee() {
        return baseMap.getInt(KEY_GAS_DEPOSIT_FEE);
    }

    public static void setGasDepositFee(int fee) {
        onlyGovernor();
        if (fee < 0) abort("Deposit fee must be nonnegative.");
        baseMap.put(KEY_GAS_DEPOSIT_FEE, fee);
        onGasDepositFeeChange.fire(fee);
    }

    public static void setMinGasDeposit(int newMinDeposit) {
        onlyGovernor();
        if (newMinDeposit < 0) abort("Minimum deposit must be nonnegative.");
        if (newMinDeposit > maxGasDeposit()) abort("Minimum deposit must be less than the maximum deposit.");
        baseMap.put(KEY_GAS_DEPOSIT_MIN_AMOUNT, newMinDeposit);
        onMinGasDepositChange.fire(newMinDeposit);
    }

    public static void setMaxGasDeposit(int newMaxDeposit) {
        onlyGovernor();
        if (newMaxDeposit < minGasDeposit()) abort("Maximum deposit must be greater than the minimum deposit.");
        baseMap.put(KEY_GAS_DEPOSIT_MAX_AMOUNT, newMaxDeposit);
        onMaxGasDepositChange.fire(newMaxDeposit);
    }

    // endregion
    // endregion
    // region token bridge
    // region token register

    public static void registerToken(Hash160 token, TokenBridge.TokenConfig tokenConfig) {
        onlyGovernor();
        if (!TokenBridge.TokenConfig.isValid(tokenConfig)) abort("Invalid token configuration.");
        _registerToken(token, tokenConfig);
        onTokenRegister.fire(token, tokenConfig);
    }

    private static void _registerToken(Hash160 token, TokenBridge.TokenConfig tokenConfig) {
        StorageMap tokenBridges = new StorageMap(ctx, PREFIX_TOKEN_BRIDGES);
        if (tokenBridges.get(token) != null) abort("Token already registered.");
        ByteString zeroHash = Hash256.zero().toByteString();
        new TokenBridge(false, new TokenBridge.State(0, zeroHash),
                new TokenBridge.State(0, zeroHash), tokenConfig);
    }

    public static void unregisterToken(Hash160 token) {
        onlyGovernor();
        onlyTokenBridgePaused(token);
        new StorageMap(ctx, PREFIX_TOKEN_BRIDGES).delete(token);
        onTokenUnregister.fire(token);
    }

    @Safe
    public static TokenBridge getTokenBridge(Hash160 token) {
        ByteString serializedTokenBridge = new StorageMap(ctx, PREFIX_TOKEN_BRIDGES).get(token);
        if (serializedTokenBridge == null) abort("Token not registered.");
        return (TokenBridge) new StdLib().deserialize(serializedTokenBridge);
    }

    // endregion token register
    // region token pausing

    public static void pauseTokenBridge(Hash160 token) {
        onlyGovernor();
        TokenBridge tokenBridge = getTokenBridge(token);
        if (tokenBridge.paused) abort("Token bridge already paused.");
        tokenBridge.paused = true;
        new StorageMap(ctx, PREFIX_TOKEN_BRIDGES).put(token, new StdLib().serialize(tokenBridge));
        onTokenBridgePause.fire(token);
    }

    public static void unpauseTokenBridge(Hash160 token) {
        onlyGovernor();
        TokenBridge tokenBridge = getTokenBridge(token);
        if (!tokenBridge.paused) abort("Token bridge already unpaused.");
        tokenBridge.paused = false;
        new StorageMap(ctx, PREFIX_TOKEN_BRIDGES).put(token, new StdLib().serialize(tokenBridge));
        onTokenBridgeUnpause.fire(token);
    }

    // endregion
    // region token deposit

    // Todo: Implement token deposit

    // endregion
    // region token withdrawal

    public static void withdrawToken(Hash160 token, ByteString withdrawalRoot, Map<ECPoint, ByteString> signatures,
            List<Withdrawal> withdrawals) {
        onlyUnpaused();
        onlyTokenBridgeUnpaused(token);
        // Token registration is checked within getTokenBridge
        TokenBridge tokenBridge = getTokenBridge(token);
        int withdrawalsSize = withdrawals.size();
        if (withdrawalsSize <= 0) abort("At least one withdrawal is required.");
        if (!subsequentNonces(withdrawals, tokenBridge.withdrawalState.nonce)) {
            abort("Provided withdrawals are not subsequent.");
        }
        if (!TokenBridgeLib.computeNewTopRoot(cryptoLib, tokenBridge.withdrawalState.root, token,
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
        new StorageMap(ctx, PREFIX_TOKEN_BRIDGES).put(token, new StdLib().serialize(tokenBridge));
        // Execute the token transfers
        TokenBridgeImpl.executeTokenTransfers(token, tokenBridge.config.tokenType, withdrawals);
    }

    // endregion
    // region token claim

    public static void claimToken(Hash160 token, int nonce) {
        onlyUnpaused();
        onlyTokenBridgeUnpaused(token);
        StorageMap tokenClaimableMap = new StorageMap(ctx, concat(PREFIX_TOKEN_CLAIMABLES, token.toByteString()));
        ByteString claimableEntry = tokenClaimableMap.get(nonce);
        if (claimableEntry == null) abort("No claim for this nonce.");
        Claimable claimable = (Claimable) new StdLib().deserialize(claimableEntry);
        Hash160 to = claimable.to;
        int amount = claimable.amount;

        tokenClaimableMap.delete(nonce);

        assert token != gasToken.getHash();
        if (new FungibleToken(token).transfer(getExecutingScriptHash(), to, amount, null)) {
            onTokenClaim.fire(token, nonce, amount, to);
        } else {
            abort("Claim transfer failed.");
        }
    }

    // endregion
    // endregion token bridge
    // region token setters

    // Todo: Implement token setters

    // endregion
    // region getters
    // region management getters

    // endregion
    // region bridge getters

    @Safe
    public static Hash160 management() {
        return baseMap.getHash160(KEY_BRIDGE_MANAGEMENT);
    }

    @Safe
    public static int minGasDeposit() {
        return baseMap.getInt(KEY_GAS_DEPOSIT_MIN_AMOUNT);
    }

    @Safe
    public static int maxGasDeposit() {
        return baseMap.getInt(KEY_GAS_DEPOSIT_MAX_AMOUNT);
    }

    @Safe
    public static ByteString gasDepositRoot() {
        return baseMap.get(KEY_GAS_DEPOSIT_ROOT);
    }

    @Safe
    public static ByteString gasWithdrawalRoot() {
        return baseMap.get(KEY_GAS_WITHDRAWAL_ROOT);
    }

    @Safe
    public static int gasDepositNonce() {
        return baseMap.getInt(KEY_GAS_DEPOSIT_NONCE);
    }

    @Safe
    public static int gasWithdrawalNonce() {
        return baseMap.getInt(KEY_GAS_WITHDRAWAL_NONCE);
    }

    // endregion
    // endregion

}
