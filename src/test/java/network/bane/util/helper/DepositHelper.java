package network.bane.util.helper;

import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;

import java.math.BigInteger;

import static network.bane.util.helper.DefaultTestValues.DEFAULT_DEPOSIT_FEE;
import static network.bane.util.helper.PrintHelper.printTransactionFee;
import static network.bane.util.helper.TestHelper.bridge;
import static network.bane.util.helper.TestHelper.neow3j;

public class DepositHelper {

    public static Hash256 depositNative(Account from, Hash160 to, BigInteger amount) throws Throwable {
        return depositNative(from, to, amount, DEFAULT_DEPOSIT_FEE);
    }

    public static Hash256 depositNative(Account from, Hash160 to, BigInteger amount, BigInteger maxFee)
            throws Throwable {
        Hash256 txHash = bridge.depositNative(from, to, amount, maxFee);
        printTransactionFee(neow3j, "deposit", txHash);
        return txHash;
    }

}
