package network.bane.scripts.token.configure;

import io.neow3j.contract.FungibleToken;
import io.neow3j.protocol.Neow3j;
import io.neow3j.wallet.Account;
import network.bane.client.BridgeClient;

import java.math.BigInteger;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.PrintHelper.printSender;
import static network.bane.utils.env.EnvVariables.NATIVE_SET_MAX_TOTAL_DEPOSITED;
import static network.bane.utils.env.EnvVariables.getBigIntegerFromEnvVar;
import static network.bane.utils.env.EnvVariables.getBridgeClientFromEnv;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.env.EnvWallets.getGovernorAccountFromEnv;

public class NativeSetMaxTotalDeposited {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        BridgeClient bridge = getBridgeClientFromEnv(neow3j);
        BigInteger maxTotalDeposited = getBigIntegerFromEnvVar(NATIVE_SET_MAX_TOTAL_DEPOSITED);
        Account governor = getGovernorAccountFromEnv();

        FungibleToken token = new FungibleToken(bridge.nativeToken(), neow3j);

        System.out.println("Setting native max total deposited...");
        System.out.printf("New value: %s (%s $%s)%n", maxTotalDeposited, token.toDecimals(maxTotalDeposited),
                token.getSymbol());
        printNetwork(neow3j);
        printSender(governor.getScriptHash());

        bridge.setMaxTotalDepositedNative(maxTotalDeposited).withSigners(calledByEntry(governor)).signSendAndAwait();

        System.out.println("Native max total deposited set successfully");
        System.out.printf("New native max total deposited: %s $%s%n",
                token.toDecimals(bridge.maxTotalDepositedNative()), token.getSymbol());
    }

}
