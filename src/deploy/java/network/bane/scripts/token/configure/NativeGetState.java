package network.bane.scripts.token.configure;

import io.neow3j.contract.FungibleToken;
import io.neow3j.contract.GasToken;
import io.neow3j.protocol.Neow3j;
import network.bane.client.BridgeClient;
import network.bane.dto.bridge.NativeBridge;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import static network.bane.utils.PrintHelper.printNetwork;
import static network.bane.utils.env.EnvVariables.getBridgeClientFromEnv;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnv;

public class NativeGetState {

    public static void main(String[] args) throws IOException {
        Neow3j neow3j = getNeow3jFromEnv();
        BridgeClient bridge = getBridgeClientFromEnv(neow3j);

        System.out.println("Get native bridge state...");
        printNetwork(neow3j);

        GasToken gasToken = new GasToken(neow3j);
        BigInteger fee = bridge.nativeDepositFee();
        BigDecimal feeDecimal = gasToken.toDecimals(fee);

        FungibleToken nativeToken = new FungibleToken(bridge.nativeToken(), neow3j);
        NativeBridge nativeBridge = bridge.getNativeBridge();
        String tokenSymbol = nativeToken.getSymbol();
        BigInteger totDeposited = nativeBridge.totalDeposited;
        BigDecimal totDepositedDecimal = nativeToken.toDecimals(totDeposited);
        BigInteger min = bridge.minNativeDeposit();
        BigDecimal minDecimal = nativeToken.toDecimals(min);
        BigInteger max = bridge.maxNativeDeposit();
        BigDecimal maxDecimal = nativeToken.toDecimals(max);
        BigInteger maxTotDeposited = bridge.maxTotalDepositedNative();
        BigDecimal maxTotDecimal = nativeToken.toDecimals(maxTotDeposited);
        int maxWithdrawals = nativeBridge.config.maxWithdrawals;

        System.out.println();
        System.out.printf("Native Bridge: %s (%s)\n", tokenSymbol, nativeToken.getScriptHash());
        System.out.printf(" paused: %s\n", nativeBridge.paused);
        System.out.printf(" total deposited: %s (%s)\n", totDeposited, totDepositedDecimal);
        System.out.printf(" deposit state:\n");
        System.out.printf("  nonce: %s\n", bridge.nativeDepositNonce());
        System.out.printf("  root:  %s\n", bridge.nativeDepositRoot());
        System.out.printf(" withdrawal state:\n");
        System.out.printf("  nonce: %s\n", bridge.nativeWithdrawalNonce());
        System.out.printf("  root:  %s\n", bridge.nativeWithdrawalRoot());
        System.out.printf(" config:\n");
        System.out.printf("  deposit fee:            %s (%s $GAS)\n", fee, feeDecimal);
        System.out.printf("  min amount:             %s (%s $%s)\n", min, minDecimal, tokenSymbol);
        System.out.printf("  max amount:             %s (%s $%s)\n", max, maxDecimal, tokenSymbol);
        System.out.printf("  max withdrawals:        %s\n", maxWithdrawals);
        System.out.printf("  max total deposited:    %s (%s $%s)\n", maxTotDeposited, maxTotDecimal, tokenSymbol);
        System.out.printf("  native token:           %s\n", nativeToken.getScriptHash());
        System.out.printf("  decimal scaling factor: %s\n", nativeBridge.config.decimalScalingFactor);
    }

}
