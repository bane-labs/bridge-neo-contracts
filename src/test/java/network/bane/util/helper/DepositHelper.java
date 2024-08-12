package network.bane.util.helper;

import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.utils.Await;
import io.neow3j.wallet.Account;

import java.math.BigInteger;

import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_GAS_DEPOSIT_FEE;
import static network.bane.util.helper.PrintHelper.printTransactionFee;
import static network.bane.util.helper.TestHelper.bridge;
import static network.bane.util.helper.TestHelper.gasToken;
import static network.bane.util.helper.TestHelper.neow3j;

public class DepositHelper {

    public static Hash256 depositGasUsingDepositMethod(Account from, Hash160 to, BigInteger amount) throws Throwable {
        return depositGasUsingDepositMethod(from, to, amount, DEFAULT_GAS_DEPOSIT_FEE);
    }

    public static Hash256 depositGasUsingDepositMethod(Account from, Hash160 to, BigInteger amount, BigInteger maxFee)
            throws Throwable {
        Hash256 txHash = bridge.depositGas(from, to, amount, maxFee);
        printTransactionFee(neow3j, "deposit", txHash);
        return txHash;
    }

    public static Hash256 depositGasWithDirectTransfer(Account from, Hash160 to, BigInteger amount,
            BigInteger minBridgeAmount) throws Throwable {

        NeoSendRawTransaction response = gasToken.transfer(from, bridge.getScriptHash(), amount, array(hash160(to),
                        integer(minBridgeAmount)))
                .sign()
                .send();
        Hash256 txHash = response.getSendRawTransaction().getHash();
        Await.waitUntilTransactionIsExecuted(txHash, neow3j);
        printTransactionFee(neow3j, "deposit direct", txHash);
        return txHash;
    }

}
