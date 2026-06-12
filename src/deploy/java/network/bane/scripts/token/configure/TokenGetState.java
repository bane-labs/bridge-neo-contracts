package network.bane.scripts.token.configure;

import io.neow3j.contract.FungibleToken;
import io.neow3j.contract.GasToken;
import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.types.Hash160;
import network.bane.dto.bridge.TokenBridge;

import java.io.IOException;
import java.math.BigDecimal;

import static io.neow3j.types.ContractParameter.hash160;
import static java.util.Arrays.asList;
import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.env.EnvVariables.BRIDGE_HASH;
import static network.bane.utils.env.EnvVariables.SETTING_TOKEN_HASH;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;

public class TokenGetState {

    public static void main(String[] args) throws IOException {
        Neow3j neow3j = getNeow3jFromEnv();
        SmartContract bridge = new SmartContract(getHash160FromEnvVar(BRIDGE_HASH), neow3j);
        Hash160 tokenHash = getHash160FromEnvVar(SETTING_TOKEN_HASH);

        System.out.println("Get token bridge state...");
        printNetwork(neow3j);
        System.out.println("Token: " + tokenHash);

        StackItem tokenBridgeStateItem = bridge.callInvokeFunction("getTokenBridge", asList(hash160(tokenHash)))
                .getInvocationResult().getFirstStackItem();
        TokenBridge tokenBridgeState = TokenBridge.fromStackItem(tokenBridgeStateItem);

        GasToken gasToken = new GasToken(neow3j);
        BigDecimal feeDecimal = gasToken.toDecimals(tokenBridgeState.config.fee);

        FungibleToken token = new FungibleToken(tokenHash, neow3j);
        String tokenSymbol = token.getSymbol();
        BigDecimal minDecimal = token.toDecimals(tokenBridgeState.config.minAmount);
        BigDecimal maxDecimal = token.toDecimals(tokenBridgeState.config.maxAmount);

        System.out.println();
        System.out.printf("Token Bridge: %s (%s)\n", tokenSymbol, tokenHash);
        System.out.printf(" paused: %s\n", tokenBridgeState.paused);
        System.out.printf(" deposit state:\n");
        System.out.printf("  nonce: %s\n", tokenBridgeState.depositState.nonce);
        System.out.printf("  root:  %s\n", tokenBridgeState.depositState.root);
        System.out.printf(" withdrawal state:\n");
        System.out.printf("  nonce: %s\n", tokenBridgeState.withdrawalState.nonce);
        System.out.printf("  root:  %s\n", tokenBridgeState.withdrawalState.root);
        System.out.printf(" config:\n");
        System.out.printf("  token on destination:   %s\n", tokenBridgeState.config.tokenOnDestination);
        System.out.printf("  deposit fee:            %s (%s $GAS)\n", tokenBridgeState.config.fee, feeDecimal);
        System.out.printf("  min amount:             %s (%s $%s)\n", tokenBridgeState.config.minAmount, minDecimal,
                tokenSymbol);
        System.out.printf("  max amount:             %s (%s $%s)\n", tokenBridgeState.config.maxAmount, maxDecimal,
                tokenSymbol);
        System.out.printf("  max withdrawals:        %s\n", tokenBridgeState.config.maxWithdrawals);
        System.out.printf("  decimal scaling factor: %s\n", tokenBridgeState.config.decimalScalingFactor);
    }

}
