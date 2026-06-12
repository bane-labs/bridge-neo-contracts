package network.bane.scripts.token.configure;

import io.neow3j.contract.FungibleToken;
import io.neow3j.contract.GasToken;
import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.stackitem.StackItem;
import network.bane.dto.bridge.NativeBridge;

import java.io.IOException;
import java.math.BigDecimal;

import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.env.EnvVariables.BRIDGE_HASH;
import static network.bane.utils.env.EnvVariables.getHash160FromEnvVar;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;

public class NativeGetState {

    public static void main(String[] args) throws IOException {
        Neow3j neow3j = getNeow3jFromEnv();
        SmartContract bridge = new SmartContract(getHash160FromEnvVar(BRIDGE_HASH), neow3j);

        System.out.println("Get native bridge state...");
        printNetwork(neow3j);

        StackItem nativeBridgeStateItem = bridge.callInvokeFunction("getNativeBridge").getInvocationResult()
                .getFirstStackItem();
        NativeBridge nativeBridgeState = NativeBridge.fromStackItem(nativeBridgeStateItem);

        GasToken gasToken = new GasToken(neow3j);
        BigDecimal feeDecimal = gasToken.toDecimals(nativeBridgeState.config.fee);

        FungibleToken token = new FungibleToken(nativeBridgeState.config.nativeToken, neow3j);
        String tokenSymbol = token.getSymbol();
        BigDecimal totDepDecimal = token.toDecimals(nativeBridgeState.totalDeposited);
        BigDecimal minDecimal = token.toDecimals(nativeBridgeState.config.minAmount);
        BigDecimal maxDecimal = token.toDecimals(nativeBridgeState.config.maxAmount);
        BigDecimal maxTotDecimal = token.toDecimals(nativeBridgeState.config.maxTotalDeposited);

        System.out.println();
        System.out.printf("Native Bridge: %s (%s)\n", tokenSymbol, token.getScriptHash());
        System.out.printf(" paused: %s\n", nativeBridgeState.paused);
        System.out.printf(" total deposited: %s (%s)\n", nativeBridgeState.totalDeposited, totDepDecimal);
        System.out.printf(" deposit state:\n");
        System.out.printf("  nonce: %s\n", nativeBridgeState.depositState.nonce);
        System.out.printf("  root:  %s\n", nativeBridgeState.depositState.root);
        System.out.printf(" withdrawal state:\n");
        System.out.printf("  nonce: %s\n", nativeBridgeState.withdrawalState.nonce);
        System.out.printf("  root:  %s\n", nativeBridgeState.withdrawalState.root);
        System.out.printf(" config:\n");
        System.out.printf("  deposit fee:            %s (%s $GAS)\n", nativeBridgeState.config.fee, feeDecimal);
        System.out.printf("  min amount:             %s (%s $%s)\n", nativeBridgeState.config.minAmount, minDecimal,
                tokenSymbol);
        System.out.printf("  max amount:             %s (%s $%s)\n", nativeBridgeState.config.maxAmount, maxDecimal,
                tokenSymbol);
        System.out.printf("  max withdrawals:        %s\n", nativeBridgeState.config.maxWithdrawals);
        System.out.printf("  max total deposited:    %s (%s $%s)\n", nativeBridgeState.config.maxTotalDeposited,
                maxTotDecimal, tokenSymbol);
        System.out.printf("  native token:           %s\n", nativeBridgeState.config.nativeToken);
        System.out.printf("  decimal scaling factor: %s\n", nativeBridgeState.config.decimalScalingFactor);
    }

}
