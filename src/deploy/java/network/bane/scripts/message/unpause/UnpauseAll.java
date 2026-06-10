package network.bane.scripts.message.unpause;

import io.neow3j.protocol.Neow3j;
import io.neow3j.types.Hash160;
import io.neow3j.wallet.Account;

import static network.bane.scripts.message.unpause.Unpause.unpause;
import static network.bane.scripts.message.unpause.UnpauseExecuting.unpauseExecuting;
import static network.bane.scripts.message.unpause.UnpauseSending.unpauseSending;
import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.PrintHelper.printSender;
import static network.bane.utils.env.EnvVariables.MESSAGE_BRIDGE_HASH;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.env.EnvWallets.getGovernorAccountFromEnv;

public class UnpauseAll {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        Hash160 bridgeHash = getHash160FromEnvVar(MESSAGE_BRIDGE_HASH);

        Account governorAcc = getGovernorAccountFromEnv();

        System.out.println("Unpause all in message bridge...");
        printNetwork(neow3j);
        printSender(governorAcc.getScriptHash());

        unpause(neow3j, bridgeHash, governorAcc);
        unpauseSending(neow3j, bridgeHash, governorAcc);
        unpauseExecuting(neow3j, bridgeHash, governorAcc);
    }

}
