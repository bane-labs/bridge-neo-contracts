package network.bane.scripts.token.configure;

import io.neow3j.contract.FungibleToken;
import io.neow3j.contract.GasToken;
import io.neow3j.protocol.Neow3j;
import io.neow3j.wallet.Account;
import network.bane.client.BridgeClient;

import java.math.BigInteger;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.PrintHelper.printSender;
import static network.bane.utils.env.EnvVariables.NATIVE_DEPOSIT_FEE;
import static network.bane.utils.env.EnvVariables.getBigIntegerFromEnvVar;
import static network.bane.utils.env.EnvVariables.getBridgeClientFromEnv;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.env.EnvWallets.getGovernorAccountFromEnv;

public class NativeSetDepositFee {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        BridgeClient bridge = getBridgeClientFromEnv(neow3j);
        BigInteger depositFee = getBigIntegerFromEnvVar(NATIVE_DEPOSIT_FEE);
        Account governor = getGovernorAccountFromEnv();

        GasToken gasToken = new GasToken(neow3j);
        FungibleToken nativeToken = new FungibleToken(bridge.nativeToken(), neow3j);

        System.out.println("Setting native deposit fee...");
        printNetwork(neow3j);
        printSender(governor.getScriptHash());
        System.out.printf("New fee: %s (%s $%s)%n", depositFee, gasToken.toDecimals(depositFee), gasToken.getSymbol());

        bridge.setNativeDepositFee(depositFee).withSigners(calledByEntry(governor)).signSendAndAwait();

        BigInteger actualDepositFee = bridge.nativeDepositFee();
        System.out.println("Native deposit fee set successfully");
        System.out.printf("New native deposit fee: %s $%s%n", nativeToken.toDecimals(actualDepositFee), nativeToken.getSymbol());
    }

}
