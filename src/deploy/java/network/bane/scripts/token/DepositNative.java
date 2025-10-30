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

import static io.neow3j.transaction.AccountSigner.global;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;
import static network.bane.utils.env.EnvVariables.BRIDGE_HASH;
import static network.bane.utils.env.EnvVariables.N3_JSON_RPC;
import static network.bane.utils.env.EnvVariables.NATIVE_DEPOSIT_AMOUNT;
import static network.bane.utils.env.EnvVariables.NATIVE_DEPOSIT_RECIPIENT_ON_EVM;
import static network.bane.utils.env.EnvVariables.WALLET_PASSWORD_PERSONAL;
import static network.bane.utils.env.EnvVariables.WALLET_FILEPATH_PERSONAL;
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

    // The following are the required env variables for running this script
    private static final Neow3j neow3j = Neow3j.build(new HttpService(N3_JSON_RPC));
    private static final SmartContract bridge = new SmartContract(BRIDGE_HASH, neow3j);
    private static final String personalWalletPath = WALLET_FILEPATH_PERSONAL;
    private static final String personalWalletPassword = WALLET_PASSWORD_PERSONAL;
    private static final Hash160 recipientOnEvm = NATIVE_DEPOSIT_RECIPIENT_ON_EVM;
    private static final BigInteger amount = NATIVE_DEPOSIT_AMOUNT;

    public static void main(String[] args) throws Throwable {
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
