package network.bane.scripts.token;

import io.neow3j.contract.FungibleToken;
import io.neow3j.contract.GasToken;
import io.neow3j.protocol.Neow3j;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;
import network.bane.client.BridgeClient;

import java.math.BigInteger;

import static io.neow3j.transaction.AccountSigner.none;
import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.PrintHelper.printSender;
import static network.bane.utils.TransactionHelper.validateTransactionHalted;
import static network.bane.utils.env.EnvVariables.NATIVE_DEPOSIT_AMOUNT;
import static network.bane.utils.env.EnvVariables.NATIVE_DEPOSIT_RECIPIENT_ON_EVM;
import static network.bane.utils.env.EnvVariables.getBigIntegerFromEnvVar;
import static network.bane.utils.env.EnvVariables.getBridgeClientFromEnv;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;
import static network.bane.utils.env.EnvWallets.getPersonalAccountFromEnv;

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
        BridgeClient bridge = getBridgeClientFromEnv(neow3j);
        Hash160 recipientOnEvm = getHash160FromEnvVar(NATIVE_DEPOSIT_RECIPIENT_ON_EVM);
        BigInteger amount = getBigIntegerFromEnvVar(NATIVE_DEPOSIT_AMOUNT);
        Account personalAccount = getPersonalAccountFromEnv();

        GasToken gasToken = new GasToken(neow3j);
        FungibleToken token = new FungibleToken(bridge.nativeToken(), neow3j);

        Hash160 from = personalAccount.getScriptHash();
        Hash160 to = recipientOnEvm;
        BigInteger depositFee = bridge.nativeDepositFee();
        BigInteger maxFee = depositFee;

        System.out.println("Deposit native tokens...");
        printNetwork(neow3j);
        printSender(personalAccount.getScriptHash());
        System.out.println("From:        " + from.toAddress());
        System.out.println("To (on EVM): " + recipientOnEvm);
        System.out.printf("Amount:        %s (%s %s)%n", amount, token.toDecimals(amount), token.getSymbol());
        System.out.printf("MaxFee:        %s (%s %s)%n", maxFee, gasToken.toDecimals(maxFee), gasToken.getSymbol());

        Hash256 txHash = bridge.depositNative(from, to, amount, maxFee)
                .withSigners(none(personalAccount).setAllowedContracts(gasToken.getScriptHash()))
                .signSendAndAwait();

        validateTransactionHalted(neow3j, txHash);
        System.out.println("Native deposit sent");
    }

}
