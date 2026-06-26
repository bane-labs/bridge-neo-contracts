package network.bane.scripts.token.configure;

import io.neow3j.contract.FungibleToken;
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
import static network.bane.utils.env.EnvVariables.TOKEN_DEPOSIT_MIN_AMOUNT;
import static network.bane.utils.env.EnvVariables.getBigIntegerFromEnvVar;
import static network.bane.utils.env.EnvVariables.getBridgeClientFromEnv;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.env.EnvWallets.getGovernorAccountFromEnv;

public class TokenSetMinAmount {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        BridgeClient bridge = getBridgeClientFromEnv(neow3j);
        Hash160 tokenHash = getHash160FromEnvVar(SETTING_TOKEN_HASH);
        BigInteger minDepositAmount = getBigIntegerFromEnvVar(TOKEN_DEPOSIT_MIN_AMOUNT);
        Account governor = getGovernorAccountFromEnv();

        BigInteger currentMinDeposit = bridge.minTokenDeposit(tokenHash);
        if (minDepositAmount.equals(currentMinDeposit)) {
            throw new IllegalStateException(
                    "Min token deposit amount is already set to the desired value: " + minDepositAmount);
        }

        FungibleToken token = new FungibleToken(tokenHash, neow3j);

        System.out.println("Setting token min deposit amount...");
        printNetwork(neow3j);
        printSender(governor.getScriptHash());
        System.out.println("Token:     " + tokenHash);
        System.out.printf("New value: %s (%s $%s)%n", minDepositAmount, token.toDecimals(minDepositAmount),
                token.getSymbol());

        HashMap<Hash160, BigInteger> minAmountMap = new HashMap<>();
        minAmountMap.put(tokenHash, minDepositAmount);

        bridge.setMinTokenDeposit(minAmountMap).withSigners(calledByEntry(governor)).signSendAndAwait(System.out);

        System.out.printf("\nMin token deposit set successfully for token %s (%s)\n", token.getSymbol(), tokenHash);
        System.out.printf("New min deposit: %s $%s%n", token.toDecimals(bridge.minTokenDeposit(tokenHash)),
                token.getSymbol());
    }

}
