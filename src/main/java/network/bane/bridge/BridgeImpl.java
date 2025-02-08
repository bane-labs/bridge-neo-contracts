package network.bane.bridge;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Helper;
import io.neow3j.devpack.contracts.FungibleToken;
import io.neow3j.devpack.contracts.GasToken;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Runtime.getExecutingScriptHash;

public class BridgeImpl {

    static void payFee(Hash160 from, int fee, Hash160 feeSponsor) {
        addToUnclaimedRewards(fee);
        Hash160 feePayer;
        if (feeSponsor == null) {
            feePayer = from;
        } else {
            if (!Hash160.isValid(feeSponsor) || feeSponsor.isZero()) abort("Invalid 'feeSponsor'");
            if (getExecutingScriptHash().equals(feeSponsor)) abort("Prohibited 'feeSponsor'");
            feePayer = feeSponsor;
        }
        // Pay the fee and transfer the token
        if (!new GasToken().transfer(feePayer, getExecutingScriptHash(), fee, null)) {
            abort("Fee transfer failed");
        }
    }

    static void checkDepositParameters(Hash160 executingScriptHash, Hash160 from, Hash160 to) {
        // Check from parameter validity and prohibition
        if (from == null || !Hash160.isValid(from) || from.isZero()) abort("Invalid 'from'");
        if (executingScriptHash.equals(from)) abort("Prohibited 'from'");

        // Check to parameter validity
        if (to == null || !Hash160.isValid(to) || to.isZero()) abort("Invalid 'to'");
    }

    /**
     * Transfers the given amount of tokens from the given from address to the executing script hash. The amount of
     * tokens actually received is returned.
     *
     * @param tokenContract the token contract to use for the transfer.
     * @param from          the address from which the tokens are transferred.
     * @param to            the address to which the tokens are transferred.
     * @param amount        the amount of tokens to transfer.
     * @return the received amount of tokens.
     */
    static int transferDepositToken(FungibleToken tokenContract, Hash160 from, Hash160 to, int amount) {
        int bridgeBalanceBefore = tokenContract.balanceOf(to);
        if (!tokenContract.transfer(from, to, amount, null)) {
            abort("Token transfer failed");
        }
        // Compare the balance before and after the transfer and use the difference as the depositing amount used for
        // the deposit hash computation.
        int bridgeBalanceAfter = tokenContract.balanceOf(to);
        if (bridgeBalanceAfter < bridgeBalanceBefore) abort("Invalid transfer");

        // Calculate the actually received amount and return it.
        return bridgeBalanceAfter - bridgeBalanceBefore;
    }

    /**
     * Divides the amount by the given decimal scaling factor and returns the result.
     *
     * @param amount               the amount to divide.
     * @param decimalScalingFactor the decimal scaling factor to use.
     * @return the divided amount.
     */
    static int divideByDecimalFactor(int amount, int decimalScalingFactor) {
        assert decimalScalingFactor >= 0 : "Invalid decimal scaling factor";

        int scalingFactor = Helper.pow(10, decimalScalingFactor);
        if (decimalScalingFactor > 0) {
            if (amount % scalingFactor != 0) abort("Amount not divisible by scaling factor");
        }
        return amount / scalingFactor;
    }

    static int getUnclaimedRewards() {
        return BridgeContract.baseMap.getInt(StorageConstants.KEY_UNCLAIMED_REWARDS);
    }

    static void addToUnclaimedRewards(int amount) {
        int currentRewards = getUnclaimedRewards();
        BridgeContract.baseMap.put(StorageConstants.KEY_UNCLAIMED_REWARDS, currentRewards + amount);
    }

    public static int getNeoHoldingGasRewards() {
        return BridgeContract.baseMap.getInt(StorageConstants.KEY_NEO_HOLDING_GAS_REWARDS);
    }

    static void addNeoHoldingGasRewards(int amount) {
        int currentRewards = getNeoHoldingGasRewards();
        BridgeContract.baseMap.put(StorageConstants.KEY_NEO_HOLDING_GAS_REWARDS, currentRewards + amount);
    }

    private static boolean entered() {
        return BridgeContract.baseMap.getBoolean(StorageConstants.KEY_ENTERED);
    }

    static void enteringNonReentrant() {
        if (entered()) abort("Reentrancy detected");
        BridgeContract.baseMap.put(StorageConstants.KEY_ENTERED, true);
    }

    static void exitingNonReentrant() {
        BridgeContract.baseMap.put(StorageConstants.KEY_ENTERED, false);
    }
}
