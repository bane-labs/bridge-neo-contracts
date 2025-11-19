package network.bane.scripts.token;

import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;

import java.math.BigInteger;

import static io.neow3j.transaction.AccountSigner.global;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static network.bane.utils.env.EnvVariables.BRIDGE_HASH;
import static network.bane.utils.env.EnvVariables.NATIVE_DEPOSIT_AMOUNT;
import static network.bane.utils.env.EnvVariables.NATIVE_DEPOSIT_RECIPIENT_ON_EVM;
import static network.bane.utils.env.EnvVariables.WALLET_PASSWORD_PERSONAL;
import static network.bane.utils.env.EnvVariables.WALLET_FILEPATH_PERSONAL;
import static network.bane.utils.env.EnvVariables.getBigIntegerFromEnvVar;
import static network.bane.utils.env.EnvVariables.getEnvVariable;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.wallet.LoadWallet.getAccountFromWallet;

/**
 * This class performs a native token deposit on the bridge contract.
 * <p>
 * Requires the following environment variables to be set:
 * - N3_JSON_RPC: The RPC endpoint of the N3 node
 * - BRIDGE_HASH: Hash of the deployed bridge contract
 * - WALLET_FILEPATH_PERSONAL: the filepath to the personal wallet. This wallet is used to send the deposit.
 * - WALLET_PASSWORD_PERSONAL: the password for the personal wallet
 * - NATIVE_DEPOSIT_RECIPIENT_ON_EVM: The recipient address on the EVM chain as a Hash160 (e.g., 0x...)
 * - NATIVE_DEPOSIT_AMOUNT: The amount of native tokens to deposit as a BigInteger
 * <p>
 * Run with: gradle run -PmainClass=network.bane.scripts.token.DepositNative
 */
public class DepositNative {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnv();
        SmartContract bridge = new SmartContract(getHash160FromEnvVar(BRIDGE_HASH), neow3j);
        String personalWalletPath = getEnvVariable(WALLET_FILEPATH_PERSONAL);
        String personalWalletPassword = getEnvVariable(WALLET_PASSWORD_PERSONAL);
        Hash160 recipientOnEvm = getHash160FromEnvVar(NATIVE_DEPOSIT_RECIPIENT_ON_EVM);
        BigInteger amount = getBigIntegerFromEnvVar(NATIVE_DEPOSIT_AMOUNT);

        Account from = getAccountFromWallet(personalWalletPath, personalWalletPassword);
        Hash160 to = recipientOnEvm;

        BigInteger depositFee = bridge.callFunctionReturningInt("nativeDepositFee");
        BigInteger maxFee = depositFee;

        Transaction tx = bridge.invokeFunction("depositNative",
                        hash160(from),
                        hash160(to),
                        integer(amount),
                        integer(maxFee)
                )
                .signers(global(from))
                .sign();

        NeoSendRawTransaction rawTxResponse = tx.send();
        if (rawTxResponse.hasError()) {
            throw new Exception("Error sending native deposit: " + rawTxResponse.getError().getMessage());
        }

        Hash256 txHash = rawTxResponse.getSendRawTransaction().getHash();
        waitUntilTransactionIsExecuted(txHash, neow3j);
        System.out.println("Native deposit sent successfully");
        System.out.println("Transaction hash: " + txHash);
    }
}
