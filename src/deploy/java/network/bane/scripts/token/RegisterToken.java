package network.bane.scripts.token;

import io.neow3j.contract.FungibleToken;
import io.neow3j.contract.GasToken;
import io.neow3j.protocol.Neow3j;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;
import network.bane.client.BridgeClient;
import network.bane.dto.bridge.TokenBridge;

import java.math.BigInteger;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.PrintHelper.printSender;
import static network.bane.utils.TransactionHelper.validateTransactionHalted;
import static network.bane.utils.env.EnvVariables.TOKEN_REGISTRATION_DECIMAL_SCALING_FACTOR;
import static network.bane.utils.env.EnvVariables.TOKEN_REGISTRATION_DEPOSIT_FEE;
import static network.bane.utils.env.EnvVariables.TOKEN_REGISTRATION_TOKEN_CONTRACT_HASH_ON_EVM;
import static network.bane.utils.env.EnvVariables.TOKEN_REGISTRATION_MAX_AMOUNT;
import static network.bane.utils.env.EnvVariables.TOKEN_REGISTRATION_MAX_WITHDRAWALS;
import static network.bane.utils.env.EnvVariables.TOKEN_REGISTRATION_MIN_AMOUNT;
import static network.bane.utils.env.EnvVariables.TOKEN_REGISTRATION_TOKEN_CONTRACT_HASH_ON_N3;
import static network.bane.utils.env.EnvVariables.getBigIntegerFromEnvVar;
import static network.bane.utils.env.EnvVariables.getBridgeClientFromEnv;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.env.EnvWallets.getGovernorAccountFromEnv;

/**
 * This script registers a token in the bridge contract.
 * <p>
 * Requires the following environment variables to be set:
 * - N3_JSON_RPC: The RPC endpoint of the N3 node
 * - BRIDGE_HASH: Hash of the deployed bridge contract
 * - WALLET_FILEPATH_GOVERNOR: the filepath to the governor wallet.
 * - WALLET_PASSWORD_GOVERNOR: the password for the governor wallet
 * - TOKEN_REGISTRATION_TOKEN_CONTRACT_HASH_ON_N3: The token contract hash on N3 to register
 * - TOKEN_REGISTRATION_TOKEN_CONTRACT_HASH_ON_EVM: The token contract hash on EVM to register
 * - TOKEN_REGISTRATION_DEPOSIT_FEE: The deposit fee for the token
 * - TOKEN_REGISTRATION_MIN_AMOUNT: The minimum amount for deposits and withdrawals
 * - TOKEN_REGISTRATION_MAX_AMOUNT: The maximum amount for deposits and withdrawals
 * - TOKEN_REGISTRATION_MAX_WITHDRAWALS: The maximum number of withdrawals allowed
 * - TOKEN_REGISTRATION_DECIMAL_SCALING_FACTOR: The decimal scaling factor between N3 and EVM
 * <p>
 * Run with: gradle run -PmainClass=network.bane.scripts.token.RegisterToken
 */
public class RegisterToken {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        BridgeClient bridge = getBridgeClientFromEnv(neow3j);
        Hash160 tokenHashOnN3 = getHash160FromEnvVar(TOKEN_REGISTRATION_TOKEN_CONTRACT_HASH_ON_N3);
        Hash160 tokenHashOnEvm = getHash160FromEnvVar(TOKEN_REGISTRATION_TOKEN_CONTRACT_HASH_ON_EVM);
        BigInteger depositFee = getBigIntegerFromEnvVar(TOKEN_REGISTRATION_DEPOSIT_FEE);
        BigInteger minAmount = getBigIntegerFromEnvVar(TOKEN_REGISTRATION_MIN_AMOUNT);
        BigInteger maxAmount = getBigIntegerFromEnvVar(TOKEN_REGISTRATION_MAX_AMOUNT);
        int maxWithdrawals = getBigIntegerFromEnvVar(TOKEN_REGISTRATION_MAX_WITHDRAWALS).intValueExact();
        int decimalScalingFactor = getBigIntegerFromEnvVar(TOKEN_REGISTRATION_DECIMAL_SCALING_FACTOR).intValueExact();
        Account governor = getGovernorAccountFromEnv();

        GasToken gasToken = new GasToken(neow3j);
        FungibleToken token = new FungibleToken(tokenHashOnN3, neow3j);

        TokenBridge.TokenConfig config = new TokenBridge.TokenConfig(
                tokenHashOnEvm,
                depositFee,
                minAmount,
                maxAmount,
                maxWithdrawals,
                decimalScalingFactor
        );

        System.out.println("Register token in bridge...");
        printNetwork(neow3j);
        printSender(governor.getScriptHash());
        System.out.println("Token:                  " + tokenHashOnN3);
        System.out.println("Token on EVM:           " + config.tokenOnDestination);
        System.out.printf("Deposit Fee:            %s (%s %s)%n", config.fee, gasToken.toDecimals(config.fee),
                gasToken.getSymbol());
        System.out.printf("Min Amount:             %s (%s %s)%n", config.minAmount, token.toDecimals(config.minAmount),
                token.getSymbol());
        System.out.printf("Max Amount:             %s (%s %s)%n", config.maxAmount, token.toDecimals(config.maxAmount),
                token.getSymbol());
        System.out.println("Max Withdrawals:        " + config.maxWithdrawals);
        System.out.println("Decimal Scaling Factor: " + config.decimalScalingFactor);

        Hash256 txHash = bridge.registerToken(tokenHashOnN3, config).withSigners(calledByEntry(governor))
                .signSendAndAwait(System.out);

        validateTransactionHalted(neow3j, txHash);
        System.out.println("Token registered successfully");
    }

}
