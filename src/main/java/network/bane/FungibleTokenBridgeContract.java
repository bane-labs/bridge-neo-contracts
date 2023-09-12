package network.bane;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Helper;
import io.neow3j.devpack.Iterator;
import io.neow3j.devpack.Runtime;
import io.neow3j.devpack.Storage;
import io.neow3j.devpack.StorageContext;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.StringLiteralHelper;
import io.neow3j.devpack.annotations.DisplayName;
import io.neow3j.devpack.annotations.ManifestExtra;
import io.neow3j.devpack.annotations.OnDeployment;
import io.neow3j.devpack.annotations.OnNEP17Payment;
import io.neow3j.devpack.annotations.Safe;
import io.neow3j.devpack.constants.FindOptions;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.contracts.FungibleToken;
import io.neow3j.devpack.contracts.GasToken;
import io.neow3j.devpack.contracts.StdLib;
import io.neow3j.devpack.events.Event1Arg;
import io.neow3j.devpack.events.Event2Args;
import io.neow3j.devpack.events.Event5Args;
import network.bane.interfaces.BridgeManagement;
import network.bane.structs.FungibleTokenData;
import network.bane.structs.TokenInfo;

import static io.neow3j.devpack.Runtime.checkWitness;
import static io.neow3j.devpack.Runtime.getExecutingScriptHash;

@DisplayName("FungibleTokenBridge")
@ManifestExtra(key = "author", value = "BaneLabs")
@ManifestExtra(key = "description", value = "Contract for bridging fungible tokens from Neo mainnet to Bane and back.")
public class FungibleTokenBridgeContract {

    private static final StorageContext ctx = Storage.getStorageContext();
    private static final StorageContext ctx_readOnly = Storage.getReadOnlyContext();

    private static final int prefix_base = 0xf0;
    private static final int prefix_whitelistedTokens = 0xf1;

    private static final int key_locked = 0x10;
    private static final int key_bridgeManagement = 0x11;

    // region deploy and update

    @OnDeployment
    public static void _deploy(Object data, boolean isUpdate) throws Exception {
        if (!isUpdate) {
//            assert data != null;
//            Hash160 managementContractHash = (Hash160) data;
            Hash160 dummyScriptHash_management = StringLiteralHelper.addressToScriptHash(
                    "NM7Aky765FG8NhhwtxjXRx7jEL1cnw7PBP");
            assert Hash160.isValid(dummyScriptHash_management);

            StorageMap baseMap = new StorageMap(ctx, prefix_base);
            baseMap.put(key_locked, 0);
            baseMap.put(key_bridgeManagement, dummyScriptHash_management);
            initializeGasTokenBridge();
        } else {
            onlyLocked();

            if ((boolean) data) {
                unlockBridge();
            }
        }
    }

    private static void initializeGasTokenBridge() {
        // todo: Provide Bane's GasToken TokenInfo in data
        Hash160 dummyScriptHash_gasTokenOnBane = StringLiteralHelper.addressToScriptHash(
                "NM7Aky765FG8NhhwtxjXRx7jEL1cnw7PBP");
        TokenInfo gasTokenInfo = new TokenInfo("GasToken", dummyScriptHash_gasTokenOnBane, 1_0000_0000);
        ByteString serializedToken = new StdLib().serialize(gasTokenInfo);
        new StorageMap(ctx, prefix_whitelistedTokens).put(new GasToken().getHash().toByteString(), serializedToken);
        onNewTokenBridge.fire(new GasToken().getHash(), gasTokenInfo.remoteToken);
    }

    public static void update(ByteString nefFile, String manifest, boolean unlockAfterUpdate) throws Exception {
        onlyLocked();
        onlyOwner();

        new ContractManagement().update(nefFile, manifest, unlockAfterUpdate);
    }

    private static BridgeManagement getBridgeManagement() {
        return new BridgeManagement(getBridgeManagementHash());
    }

    private static Hash160 getBridgeManagementHash() {
        return new StorageMap(ctx_readOnly, prefix_base).getHash160(key_bridgeManagement);
    }

    // endregion
    // region register token bridge

    public static void setTokenBridge(Hash160 localToken, TokenInfo tokenInfo) throws Exception {
        assert Hash160.isValid(localToken);
        assert tokenInfo.isValid();
        // todo: check for existing token storage entry required when adding a token?
        onlyUnlocked();
        onlyOwner();

        ByteString serializedToken = new StdLib().serialize(tokenInfo);
        new StorageMap(ctx, prefix_whitelistedTokens).put(localToken.toByteString(), serializedToken);
        onNewTokenBridge.fire(localToken, tokenInfo.remoteToken);
    }

    @Safe
    public static TokenInfo getTokenInfo(Hash160 localToken) throws Exception {
        ByteString serializedTokenInfo = new StorageMap(ctx_readOnly, prefix_whitelistedTokens)
                .get(localToken.toByteString());
        if (serializedTokenInfo == null) {
            throw new Exception("no token bridge registered with the provided local token script hash");
        }
        return (TokenInfo) new StdLib().deserialize(serializedTokenInfo);
    }

    public static void removeTokenBridge(Hash160 localToken) throws Exception {
        assert Hash160.isValid(localToken);
        onlyUnlocked();
        onlyOwner();

        new StorageMap(ctx, prefix_whitelistedTokens).delete(localToken.toByteString());
        onTokenBridgeRemoval.fire(localToken);
    }

    @Safe
    public static Iterator<Iterator.Struct<ByteString, ByteString>> getAllTokenBridges() {
        return new StorageMap(ctx_readOnly, prefix_whitelistedTokens).find((byte) (FindOptions.None |
                FindOptions.DeserializeValues));
    }

    // endregion
    // region deposit

    /**
     * OnNEP17Payment method that accepts whitelisted NEP-17 tokens.
     * <p>
     * {@code data} must be in correct form, i.e., it must match {@link FungibleTokenData}.
     *
     * @param from   the sender.
     * @param amount the amount.
     * @param data   the data used to represent additional necessary information to process the token bridging.
     */
    @OnNEP17Payment
    public static void depositTo(Hash160 from, int amount, Object data) {
        Hash160 callingScriptHash = Runtime.getCallingScriptHash();
        checkCallingScriptHash(callingScriptHash);

        FungibleTokenData tokenData = (FungibleTokenData) data;
        if (!tokenData.isValid()) {
            Helper.abort();
        }
        onlyUnlocked();

        // todo: Decide where to hold the tokens. (1) Keep them in this contract and the deposit is done with the
        //  following event fired, OR (2) only use this contract to do checks and hold the tokens in a separate
        //  contract with less logic?
        onDeposit.fire(callingScriptHash, from, tokenData.to, amount, tokenData.minGasLimit);
    }

    /**
     * Aborts if calling script hash is not accepted.
     *
     * @param callingScriptHash the script hash of the caller.
     */
    private static void checkCallingScriptHash(Hash160 callingScriptHash) {
        ByteString serializedTokenInfo = new StorageMap(ctx_readOnly, prefix_whitelistedTokens)
                .get(callingScriptHash.toByteString());

        if (serializedTokenInfo == null) {
            Helper.abort();
        }
    }

    // endregion
    // region withdraw

    public static boolean withdraw(Hash160 localToken, Hash160 to, int amount) throws Exception {
        assert Hash160.isValid(localToken);
        assert Hash160.isValid(to);
        assert amount >= 0;
        onlyUnlocked();
        onlyRelayer();

        return new FungibleToken(localToken).transfer(getExecutingScriptHash(), to, amount, null);
    }

    // endregion
    // region lock contract

    public static void lockBridge() throws Exception {
        onlyUnlocked();
        onlySecurityCouncil();
        new StorageMap(ctx, prefix_base).put(key_locked, 1);
    }

    public static void unlockBridge() throws Exception {
        onlyLocked();
        onlySecurityCouncil();
        new StorageMap(ctx, prefix_base).put(key_locked, 0);
    }

    private static void onlyUnlocked() {
        if (isLocked()) {
            Helper.abort();
        }
    }

    private static void onlyLocked() {
        if (!isLocked()) {
            Helper.abort();
        }
    }

    private static boolean isLocked() {
        return new StorageMap(ctx_readOnly, prefix_base).getBoolean(key_locked);
    }

    // endregion
    // region restrictions / modifiers

    private static void onlyOwner() throws Exception {
        if (!checkWitness(getBridgeManagement().getOwner())) {
            // todo: abort or throw exception?
            throw new Exception("No authorization");
        }
    }

    private static void onlyRelayer() throws Exception {
        if (!checkWitness(getBridgeManagement().getRelayer())) {
            // todo: abort or throw exception?
            throw new Exception("No authorization");
        }
    }

    private static void onlySecurityCouncil() throws Exception {
        // bridge owners should be enabled to overrule security council
        if (!checkWitness(getBridgeManagement().getSecurityCouncil())) {
            if (checkWitness(getBridgeManagement().getOwner())) {
                return;
            }
            // todo: abort or throw exception?
            throw new Exception("No authorization");
        }
    }

    // endregion
    // region events

    /**
     * Parameters:
     * <l>
     * <li>Local Token ScriptHash</li>
     * <li>Remote Token ScriptHash</li>
     * <li>Receiving Address</li>
     * <li>Amount</li>
     * <li>Min Gas Limit</li>
     * </l>
     */
    @DisplayName("OnDeposit")
    public static Event5Args<Hash160, Hash160, Hash160, Integer, Integer> onDeposit;

    /**
     * Parameters:
     * <l>
     * <li>Local Token ScriptHash</li>
     * <li>Token Info</li>
     * </l>
     * <p>
     * Where TokenInfo follows the struct {@link TokenInfo}.
     */
    @DisplayName("OnWhitelistAdd")
    public static Event2Args<Hash160, Hash160> onNewTokenBridge;

    /**
     * Parameter:
     * <l>
     * <li>Local Token ScriptHash</li>
     * </l>
     */
    @DisplayName("OnWhitelistRemove")
    public static Event1Arg<Hash160> onTokenBridgeRemoval;

    // endregion

}
