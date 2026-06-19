package network.bane.scripts.token;

import io.neow3j.contract.FungibleToken;
import io.neow3j.contract.GasToken;
import io.neow3j.protocol.Neow3j;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;
import network.bane.client.BridgeClient;

import java.math.BigInteger;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.PrintHelper.printSender;
import static network.bane.utils.TransactionHelper.validateTransactionHalted;
import static network.bane.utils.env.EnvVariables.NATIVE_SET_DECIMALS_ON_LINKED_CHAIN;
import static network.bane.utils.env.EnvVariables.NATIVE_SET_DEPOSIT_FEE;
import static network.bane.utils.env.EnvVariables.NATIVE_SET_MAX_AMOUNT;
import static network.bane.utils.env.EnvVariables.NATIVE_SET_MAX_TOTAL_DEPOSITED;
import static network.bane.utils.env.EnvVariables.NATIVE_SET_MAX_WITHDRAWALS;
import static network.bane.utils.env.EnvVariables.NATIVE_SET_MIN_AMOUNT;
import static network.bane.utils.env.EnvVariables.NATIVE_SET_TOKEN_FOR_NATIVE_BRIDGE;
import static network.bane.utils.env.EnvVariables.getBigIntegerFromEnvVar;
import static network.bane.utils.env.EnvVariables.getBridgeClientFromEnv;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.env.EnvWallets.getGovernorAccountFromEnv;

/**
 * This script sets the native token bridge in the bridge contract.
 * <p>
 * Requires the following environment variables to be set:
 * - N3_JSON_RPC: The RPC endpoint of the N3 node
 * - BRIDGE_HASH: Hash of the deployed bridge contract
 * - WALLET_FILEPATH_GOVERNOR: the filepath to the governor wallet.
 * - WALLET_PASSWORD_GOVERNOR: the password for the governor wallet
 * - NATIVE_SET_TOKEN_FOR_NATIVE_BRIDGE: The native token hash for the native bridge
 * - NATIVE_SET_DECIMALS_ON_LINKED_CHAIN: The decimals on the linked chain
 * - NATIVE_SET_DEPOSIT_FEE: The deposit fee for the native token
 * - NATIVE_SET_MIN_AMOUNT: The minimum amount for deposits and withdrawals
 * - NATIVE_SET_MAX_AMOUNT: The maximum amount for deposits and withdrawals
 * - NATIVE_SET_MAX_WITHDRAWALS: The maximum number of withdrawals allowed
 * - NATIVE_SET_MAX_TOTAL_DEPOSITED: The maximum total deposited amount allowed
 * <p>
 * Run with: gradle run -PmainClass=network.bane.scripts.token.SetNativeBridge
 */
public class SetNativeBridge {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        BridgeClient bridge = getBridgeClientFromEnv(neow3j);
        Hash160 tokenForNativeBridge = getHash160FromEnvVar(NATIVE_SET_TOKEN_FOR_NATIVE_BRIDGE);
        int decimalsOnLinkedChain = getBigIntegerFromEnvVar(NATIVE_SET_DECIMALS_ON_LINKED_CHAIN).intValueExact();
        BigInteger depositFee = getBigIntegerFromEnvVar(NATIVE_SET_DEPOSIT_FEE);
        BigInteger minAmount = getBigIntegerFromEnvVar(NATIVE_SET_MIN_AMOUNT);
        BigInteger maxAmount = getBigIntegerFromEnvVar(NATIVE_SET_MAX_AMOUNT);
        int maxWithdrawals = getBigIntegerFromEnvVar(NATIVE_SET_MAX_WITHDRAWALS).intValueExact();
        BigInteger maxTotalDeposited = getBigIntegerFromEnvVar(NATIVE_SET_MAX_TOTAL_DEPOSITED);
        Account governor = getGovernorAccountFromEnv();

        GasToken gasToken = new GasToken(neow3j);
        FungibleToken token = new FungibleToken(tokenForNativeBridge, neow3j);

        System.out.println("Set native bridge...");
        printNetwork(neow3j);
        printSender(governor.getScriptHash());
        System.out.println("Token:               " + tokenForNativeBridge);
        System.out.println("Decimals on EVM:     " + decimalsOnLinkedChain);
        System.out.printf("Deposit fee:         %s (%s %s)%n", depositFee, gasToken.toDecimals(depositFee),
                gasToken.getSymbol());
        System.out.printf("Min amount:          %s (%s %s)%n", minAmount, token.toDecimals(minAmount),
                token.getSymbol());
        System.out.printf("Max amount:          %s (%s %s)%n", maxAmount, token.toDecimals(maxAmount),
                token.getSymbol());
        System.out.println("Max withdrawals:     " + maxWithdrawals);
        System.out.printf("Max total deposited: %s (%s %s)%n", maxTotalDeposited, token.toDecimals(maxTotalDeposited),
                token.getSymbol());

        Hash256 txHash = bridge.setNativeBridge(tokenForNativeBridge, decimalsOnLinkedChain, depositFee, minAmount,
                        maxAmount, maxWithdrawals, maxTotalDeposited)
                .withSigners(calledByEntry(governor)).signSendAndAwait(System.out);

        validateTransactionHalted(neow3j, txHash);
        System.out.println("Native bridge set successfully");
    }

}
