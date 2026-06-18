package network.bane.scripts.token.configure;

import io.neow3j.contract.FungibleToken;
import io.neow3j.contract.GasToken;
import io.neow3j.protocol.Neow3j;
import io.neow3j.types.Hash160;
import io.neow3j.wallet.Account;
import network.bane.client.BridgeClient;

import java.math.BigInteger;
import java.util.HashMap;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.PrintHelper.printSender;
import static network.bane.utils.env.EnvVariables.SETTING_TOKEN_HASH;
import static network.bane.utils.env.EnvVariables.TOKEN_DEPOSIT_FEE;
import static network.bane.utils.env.EnvVariables.getBigIntegerFromEnvVar;
import static network.bane.utils.env.EnvVariables.getBridgeClientFromEnv;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.env.EnvWallets.getGovernorAccountFromEnv;

public class TokenSetDepositFee {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        BridgeClient bridge = getBridgeClientFromEnv(neow3j);
        Hash160 tokenHash = getHash160FromEnvVar(SETTING_TOKEN_HASH);
        BigInteger depositFee = getBigIntegerFromEnvVar(TOKEN_DEPOSIT_FEE);
        Account governor = getGovernorAccountFromEnv();

        FungibleToken gasToken = new GasToken(neow3j);

        System.out.println("Setting token deposit fee...");
        printNetwork(neow3j);
        printSender(governor.getScriptHash());
        System.out.println("Token:   " + tokenHash);
        System.out.printf("New fee: %s (%s $%s)%n", depositFee, gasToken.toDecimals(depositFee), gasToken.getSymbol());

        HashMap<Hash160, BigInteger> feeMap = new HashMap<>();
        feeMap.put(tokenHash, depositFee);

        bridge.setTokenDepositFee(feeMap).withSigners(calledByEntry(governor)).signSendAndAwait();

        System.out.println("Token deposit fee set successfully");
        System.out.printf("New token deposit fee of token %s: %s $GAS%n", tokenHash,
                gasToken.toDecimals(bridge.tokenDepositFee(tokenHash)));
    }

}
