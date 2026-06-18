package network.bane.scripts.token.configure;

import io.neow3j.contract.FungibleToken;
import io.neow3j.protocol.Neow3j;
import io.neow3j.wallet.Account;
import network.bane.client.BridgeClient;

import java.math.BigInteger;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.PrintHelper.printSender;
import static network.bane.utils.env.EnvVariables.NATIVE_DEPOSIT_MIN_AMOUNT;
import static network.bane.utils.env.EnvVariables.getBigIntegerFromEnvVar;
import static network.bane.utils.env.EnvVariables.getBridgeClientFromEnv;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.env.EnvWallets.getGovernorAccountFromEnv;

public class NativeSetMinAmount {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        BridgeClient bridge = getBridgeClientFromEnv(neow3j);
        BigInteger minDepositAmount = getBigIntegerFromEnvVar(NATIVE_DEPOSIT_MIN_AMOUNT);
        Account governor = getGovernorAccountFromEnv();

        BigInteger currentMinDeposit = bridge.callFunctionReturningInt("minNativeDeposit");
        if (minDepositAmount.equals(currentMinDeposit)) {
            throw new IllegalStateException(
                    "Min native deposit amount is already set to the desired value: " + minDepositAmount);
        }

        FungibleToken nativeToken = new FungibleToken(bridge.nativeToken(), neow3j);

        System.out.println("Setting native min deposit amount...");
        System.out.printf("New value: %s (%s $%s)%n", minDepositAmount, nativeToken.toDecimals(minDepositAmount),
                nativeToken.getSymbol());
        printNetwork(neow3j);
        printSender(governor.getScriptHash());

        bridge.setMinNativeDeposit(minDepositAmount).withSigners(calledByEntry(governor)).signSendAndAwait();

        System.out.println("Min native deposit set successfully");
        System.out.printf("New min deposit: %s $%s%n", nativeToken.toDecimals(bridge.minNativeDeposit()),
                nativeToken.getSymbol());
    }

}
