package network.bane.util.helper;

import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.TransactionBuilder;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;
import network.bane.util.Bridge;

import java.math.BigInteger;

import static io.neow3j.types.ContractParameter.integer;

public class MigrationHelper {

    public static Hash256 migrate(Account sender, Bridge bridge, BigInteger maxGasWithdrawals) throws Throwable {
        TransactionBuilder migrateTxBuilder = bridge.invokeFunction("migrate", integer(maxGasWithdrawals))
                .signers(AccountSigner.none(sender));
        return bridge.sendAndAwaitExecution(migrateTxBuilder);
    }

}
