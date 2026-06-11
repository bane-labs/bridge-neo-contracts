package network.bane.scripts.token.configure;

import io.neow3j.contract.FungibleToken;
import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;

import java.math.BigInteger;
import java.util.HashMap;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.map;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.PrintHelper.printSender;
import static network.bane.utils.env.EnvVariables.BRIDGE_HASH;
import static network.bane.utils.env.EnvVariables.SETTING_TOKEN_HASH;
import static network.bane.utils.env.EnvVariables.TOKEN_DEPOSIT_MIN_AMOUNT;
import static network.bane.utils.env.EnvVariables.getBigIntegerFromEnvVar;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.env.EnvWallets.getGovernorAccountFromEnv;

public class TokenSetMinAmount {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        SmartContract bridge = new SmartContract(getHash160FromEnvVar(BRIDGE_HASH), neow3j);
        Hash160 tokenHash = getHash160FromEnvVar(SETTING_TOKEN_HASH);
        FungibleToken token = new FungibleToken(tokenHash, neow3j);
        BigInteger minDepositAmount = getBigIntegerFromEnvVar(TOKEN_DEPOSIT_MIN_AMOUNT);

        BigInteger currentMinDeposit = bridge.callFunctionReturningInt("minTokenDeposit", hash160(tokenHash));
        if (minDepositAmount.equals(currentMinDeposit)) {
            throw new IllegalStateException(
                    "Min token deposit amount is already set to the desired value: " + minDepositAmount);
        }

        Account governorAcc = getGovernorAccountFromEnv();

        System.out.println("Setting token min deposit amount...");
        printNetwork(neow3j);
        printSender(governorAcc.getScriptHash());
        System.out.println("Token:     " + tokenHash);
        System.out.printf("New value: %s (%s $%s)%n", minDepositAmount, token.toDecimals(minDepositAmount),
                token.getSymbol());

        HashMap<Hash160, BigInteger> minAmountMap = new HashMap<>();
        minAmountMap.put(tokenHash, minDepositAmount);

        Transaction tx = bridge.invokeFunction("setMinTokenDeposit", map(minAmountMap))
                .signers(calledByEntry(governorAcc))
                .sign();

        NeoSendRawTransaction rawTxResponse = tx.send();
        if (rawTxResponse.hasError()) {
            throw new Exception("Error setting token min deposit amount: " + rawTxResponse.getError().getMessage());
        }

        Hash256 txHash = rawTxResponse.getSendRawTransaction().getHash();
        waitUntilTransactionIsExecuted(txHash, neow3j);

        BigInteger actualDepositFee = bridge.callFunctionReturningInt("minTokenDeposit", hash160(tokenHash));
        System.out.printf("\nMin token deposit set successfully for token %s (%s)\n", token.getSymbol(), tokenHash);
        System.out.printf("New min deposit: %s $%s%n", token.toDecimals(actualDepositFee), token.getSymbol());
        System.out.println("Transaction hash: " + txHash);
    }

}
