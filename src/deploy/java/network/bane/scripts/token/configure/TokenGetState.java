package network.bane.scripts.token.configure;

import io.neow3j.contract.FungibleToken;
import io.neow3j.contract.GasToken;
import io.neow3j.protocol.Neow3j;
import io.neow3j.types.Hash160;
import network.bane.client.BridgeClient;
import network.bane.dto.bridge.TokenBridge;

import java.io.IOException;
import java.math.BigDecimal;

import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.env.EnvVariables.SETTING_TOKEN_HASH;
import static network.bane.utils.env.EnvVariables.getBridgeClientFromEnv;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;

public class TokenGetState {

    public static void main(String[] args) throws IOException {
        Neow3j neow3j = getNeow3jFromEnv();
        BridgeClient bridge = getBridgeClientFromEnv(neow3j);
        Hash160 tokenHash = getHash160FromEnvVar(SETTING_TOKEN_HASH);

        System.out.println("Get token bridge state...");
        printNetwork(neow3j);
        System.out.println("Token: " + tokenHash);

        TokenBridge tokenBridge = bridge.getTokenBridge(tokenHash);

        GasToken gasToken = new GasToken(neow3j);
        FungibleToken token = new FungibleToken(tokenHash, neow3j);
        String tokenSymbol = token.getSymbol();

        BigDecimal feeDecimal = gasToken.toDecimals(tokenBridge.config.fee);
        BigDecimal minDecimal = token.toDecimals(tokenBridge.config.minAmount);
        BigDecimal maxDecimal = token.toDecimals(tokenBridge.config.maxAmount);

        System.out.println();
        System.out.printf("Token Bridge: %s (%s)\n", tokenSymbol, tokenHash);
        System.out.printf(" paused: %s\n", tokenBridge.paused);
        System.out.printf(" deposit state:\n");
        System.out.printf("  nonce: %s\n", tokenBridge.depositState.nonce);
        System.out.printf("  root:  %s\n", tokenBridge.depositState.root);
        System.out.printf(" withdrawal state:\n");
        System.out.printf("  nonce: %s\n", tokenBridge.withdrawalState.nonce);
        System.out.printf("  root:  %s\n", tokenBridge.withdrawalState.root);
        System.out.printf(" config:\n");
        System.out.printf("  token on destination:   %s\n", tokenBridge.config.tokenOnDestination);
        System.out.printf("  deposit fee:            %s (%s $GAS)\n", tokenBridge.config.fee, feeDecimal);
        System.out.printf("  min amount:             %s (%s $%s)\n", tokenBridge.config.minAmount, minDecimal,
                tokenSymbol);
        System.out.printf("  max amount:             %s (%s $%s)\n", tokenBridge.config.maxAmount, maxDecimal,
                tokenSymbol);
        System.out.printf("  max withdrawals:        %s\n", tokenBridge.config.maxWithdrawals);
        System.out.printf("  decimal scaling factor: %s\n", tokenBridge.config.decimalScalingFactor);
    }

}
