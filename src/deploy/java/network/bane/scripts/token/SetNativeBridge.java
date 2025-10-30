package network.bane.scripts.token;

import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;

import java.math.BigInteger;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static network.bane.utils.env.EnvVariables.BRIDGE_HASH;
import static network.bane.utils.env.EnvVariables.WALLET_PASSWORD_GOVERNOR;
import static network.bane.utils.env.EnvVariables.WALLET_FILEPATH_GOVERNOR;
import static network.bane.utils.env.EnvVariables.NATIVE_SET_DECIMALS_ON_LINKED_CHAIN;
import static network.bane.utils.env.EnvVariables.NATIVE_SET_DEPOSIT_FEE;
import static network.bane.utils.env.EnvVariables.NATIVE_SET_MAX_AMOUNT;
import static network.bane.utils.env.EnvVariables.NATIVE_SET_MAX_TOTAL_DEPOSITED;
import static network.bane.utils.env.EnvVariables.NATIVE_SET_MAX_WITHDRAWALS;
import static network.bane.utils.env.EnvVariables.NATIVE_SET_MIN_AMOUNT;
import static network.bane.utils.env.EnvVariables.NATIVE_SET_TOKEN_FOR_NATIVE_BRIDGE;
import static network.bane.utils.env.EnvVariables.N3_JSON_RPC;
import static network.bane.utils.wallet.LoadWallet.getAccountFromWallet;

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

    // The following are the required env variables for running this script
    private static final Neow3j neow3j = Neow3j.build(new HttpService(N3_JSON_RPC));
    private static final SmartContract bridge = new SmartContract(BRIDGE_HASH, neow3j);
    private static final String governorWalletPath = WALLET_FILEPATH_GOVERNOR;
    private static final String governorWalletPassword = WALLET_PASSWORD_GOVERNOR;
    private static final Hash160 tokenForNativeBridge = NATIVE_SET_TOKEN_FOR_NATIVE_BRIDGE;
    private static final BigInteger decimalsOnLinkedChain = NATIVE_SET_DECIMALS_ON_LINKED_CHAIN;
    private static final BigInteger depositFee = NATIVE_SET_DEPOSIT_FEE;
    private static final BigInteger minAmount = NATIVE_SET_MIN_AMOUNT;
    private static final BigInteger maxAmount = NATIVE_SET_MAX_AMOUNT;
    private static final BigInteger maxWithdrawals = NATIVE_SET_MAX_WITHDRAWALS;
    private static final BigInteger maxTotalDeposited = NATIVE_SET_MAX_TOTAL_DEPOSITED;

    public static void main(String[] args) throws Throwable {
        Account governorAcc = getAccountFromWallet(governorWalletPath, governorWalletPassword);

        Transaction tx = bridge.invokeFunction("setNativeBridge",
                        hash160(tokenForNativeBridge),
                        integer(decimalsOnLinkedChain),
                        integer(depositFee),
                        integer(minAmount),
                        integer(maxAmount),
                        integer(maxWithdrawals),
                        integer(maxTotalDeposited)
                )
                .signers(calledByEntry(governorAcc))
                .sign();

        NeoSendRawTransaction response = tx.send();
        if (response.hasError()) {
            throw new Exception(response.getError().getMessage());
        }

        Hash256 txHash = response.getSendRawTransaction().getHash();
        waitUntilTransactionIsExecuted(txHash, neow3j);
        System.out.println("Native bridge set successfully");
        System.out.println("Transaction hash: " + txHash);
    }

}
