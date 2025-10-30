package network.bane.scripts.token;

import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;

import java.math.BigInteger;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static network.bane.utils.env.EnvVariables.BRIDGE_HASH;
import static network.bane.utils.env.EnvVariables.N3_JSON_RPC;
import static network.bane.utils.env.EnvVariables.WALLET_PASSWORD_GOVERNOR;
import static network.bane.utils.env.EnvVariables.WALLET_FILEPATH_GOVERNOR;
import static network.bane.utils.env.EnvVariables.TOKEN_REGISTRATION_DECIMAL_SCALING_FACTOR;
import static network.bane.utils.env.EnvVariables.TOKEN_REGISTRATION_DEPOSIT_FEE;
import static network.bane.utils.env.EnvVariables.TOKEN_REGISTRATION_TOKEN_CONTRACT_HASH_ON_EVM;
import static network.bane.utils.env.EnvVariables.TOKEN_REGISTRATION_MAX_AMOUNT;
import static network.bane.utils.env.EnvVariables.TOKEN_REGISTRATION_MAX_WITHDRAWALS;
import static network.bane.utils.env.EnvVariables.TOKEN_REGISTRATION_MIN_AMOUNT;
import static network.bane.utils.env.EnvVariables.TOKEN_REGISTRATION_TOKEN_CONTRACT_HASH_ON_N3;
import static network.bane.utils.env.EnvVariables.getBigIntegerFromEnvVar;
import static network.bane.utils.env.EnvVariables.getEnvVariable;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnvVar;
import static network.bane.utils.wallet.LoadWallet.getAccountFromWallet;

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
        Neow3j neow3j = getNeow3jFromEnvVar(N3_JSON_RPC);
        SmartContract bridge = new SmartContract(getHash160FromEnvVar(BRIDGE_HASH), neow3j);
        String governorWalletPath = getEnvVariable(WALLET_FILEPATH_GOVERNOR);
        String governorWalletPassword = getEnvVariable(WALLET_PASSWORD_GOVERNOR);
        Hash160 tokenHashOnN3 = getHash160FromEnvVar(TOKEN_REGISTRATION_TOKEN_CONTRACT_HASH_ON_N3);
        Hash160 tokenHashOnEvm = getHash160FromEnvVar(TOKEN_REGISTRATION_TOKEN_CONTRACT_HASH_ON_EVM);
        BigInteger depositFee = getBigIntegerFromEnvVar(TOKEN_REGISTRATION_DEPOSIT_FEE);
        BigInteger minAmount = getBigIntegerFromEnvVar(TOKEN_REGISTRATION_MIN_AMOUNT);
        BigInteger maxAmount = getBigIntegerFromEnvVar(TOKEN_REGISTRATION_MAX_AMOUNT);
        BigInteger maxWithdrawals = getBigIntegerFromEnvVar(TOKEN_REGISTRATION_MAX_WITHDRAWALS);
        BigInteger decimalScalingFactor = getBigIntegerFromEnvVar(TOKEN_REGISTRATION_DECIMAL_SCALING_FACTOR);

        Account governorAcc = getAccountFromWallet(governorWalletPath, governorWalletPassword);

        Transaction tx = bridge.invokeFunction("registerToken",
                        hash160(tokenHashOnN3),
                        array(
                                hash160(tokenHashOnEvm),
                                integer(depositFee),
                                integer(minAmount),
                                integer(maxAmount),
                                integer(maxWithdrawals),
                                integer(decimalScalingFactor)
                        )
                )
                .signers(calledByEntry(governorAcc))
                .sign();

        NeoSendRawTransaction rawTxResponse = tx.send();
        if (rawTxResponse.hasError()) {
            throw new Exception("Error registering token: " + rawTxResponse.getError().getMessage());
        }

        Hash256 txHash = rawTxResponse.getSendRawTransaction().getHash();
        waitUntilTransactionIsExecuted(txHash, neow3j);
        System.out.println("Token registered successfully");
        System.out.println("Transaction hash: " + txHash);
    }

}
