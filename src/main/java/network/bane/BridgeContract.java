package network.bane;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Helper;
import io.neow3j.devpack.Runtime;
import io.neow3j.devpack.Storage;
import io.neow3j.devpack.StorageContext;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.annotations.DisplayName;
import io.neow3j.devpack.annotations.ManifestExtra;
import io.neow3j.devpack.annotations.OnDeployment;
import io.neow3j.devpack.annotations.OnNEP17Payment;
import io.neow3j.devpack.annotations.Safe;
import io.neow3j.devpack.contracts.CryptoLib;
import io.neow3j.devpack.contracts.GasToken;
import io.neow3j.devpack.events.Event6Args;
import network.bane.interfaces.BridgeManagement;
import network.bane.structs.BridgeDeploymentData;

import static io.neow3j.devpack.Helper.concat;
import static io.neow3j.devpack.Helper.toByteArray;

@DisplayName("BaneBridge")
@ManifestExtra(key = "author", value = "BaneLabs")
@ManifestExtra(key = "description", value = "Contract for bridging GAS tokens from Neo N3 to Bane.")
public class BridgeContract {

    private static final StorageContext ctx = Storage.getStorageContext();

    // region storage keys

    private static final int prefix_base = 0xf0;
    private static final StorageMap baseMap = new StorageMap(ctx, prefix_base);

    private static final int key_bridgeManagement = 0x00;

    private static final int key_locked = 0x01;
    private static final int key_minDeposit = 0x02;
    private static final int key_depositPrice = 0x03;

    private static final int key_deposit_root = 0x10;
    private static final int key_deposit_nonce = 0x11;
    private static final int key_deposit_maxIndex = 0x12;

    // Used to store incomplete subtree hashes
    private static final int prefix_deposit_root = 0xf1;

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
    public static Event6Args<Integer, Hash160, Hash160, Integer, ByteString, ByteString> onDeposit;

    // endregion
    // region deployment

    @OnDeployment
    public static void deploy(Object data, boolean isUpdate) {
        if (!isUpdate) {
            BridgeDeploymentData deploymentData = (BridgeDeploymentData) data;
            assert deploymentData.isValid();
            baseMap.put(key_bridgeManagement, deploymentData.bridgeManagementContractHash);
            baseMap.put(key_minDeposit, deploymentData.minDeposit);
            baseMap.put(key_depositPrice, deploymentData.depositPrice);

            baseMap.put(key_deposit_nonce, 0);
            baseMap.put(key_deposit_maxIndex, 0);
        }
    }

    // endregion
    // region deposit

    @OnNEP17Payment
    public static void deposit(Hash160 from, int amount, Object data) {
        if (Runtime.getCallingScriptHash() != new GasToken().getHash()) {
            Helper.abort();
            //Helper.abort("Only GAS is accepted.");
        }
        Hash160 to = (Hash160) data;
        assert Hash160.isValid(to);
        //assert Hash160.isValid(to) : "Provided data has invalid format.";

        int nonce = newNonce();
        ByteString depositHash = hashDeposit(nonce, to, amount);
        ByteString root = updateMerkleTree(depositHash);
        onDeposit.fire(nonce, from, to, amount, depositHash, root);
    }

    private static ByteString hashDeposit(int nonce, Hash160 to, int amount) {
        byte[] concatenatedDepositData =
                concat(
                        concat(
                                toByteArray(nonce),
                                to.toByteString()
                        ),
                        toByteArray(amount)
                );
        return new CryptoLib().sha256(new ByteString(concatenatedDepositData));
    }

    private static ByteString updateMerkleTree(ByteString depositHash) {
        StorageMap rootMap = new StorageMap(ctx, prefix_deposit_root);

        int maxIndex = baseMap.getInt(key_deposit_maxIndex);
        boolean carry = true;
        ByteString right = depositHash;
        for (int i = 0; i <= maxIndex; i++) {
            ByteString entryAti = rootMap.get(i);
            boolean stored = entryAti != null;
            if (stored) {
                right = computeParentHash(rootMap.get(i), right);
                if (carry) {
                    rootMap.delete(i);
                    if (i == maxIndex) {
                        baseMap.put(key_deposit_maxIndex, maxIndex + 1);
                        rootMap.put(maxIndex + 1, right);
                    }
                }
            } else {
                if (carry) {
                    rootMap.put(i, right);
                }
                carry = false;
            }
        }
        return right;
    }

    private static int newNonce() {
        int nextNonce = baseMap.getInt(key_deposit_nonce) + 1;
        baseMap.put(key_deposit_nonce, nextNonce);
        return nextNonce;
    }

    // endregion
    // region verify withdrawal

    // Todo: Verify withdrawal

    // endregion
    // region helpers

    private static ByteString computeParentHash(ByteString left, ByteString right) {
        return new CryptoLib().sha256(new ByteString(concat(left.toByteArray(), right)));
    }

    // endregion
    // region getters

    @Safe
    public static ByteString getDepositRoot() {
        return baseMap.get(key_deposit_root);
    }

    // endregion
    // region bridge management

    private static BridgeManagement getManagement() {
        return new BridgeManagement(baseMap.getHash160(key_bridgeManagement));
    }

    // endregion

}
