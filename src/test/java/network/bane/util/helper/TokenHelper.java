package network.bane.util.helper;

import network.bane.dto.bridge.TokenBridge;

import static network.bane.util.helper.DefaultTestValues.DEFAULT_DEPOSIT_FEE;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_MAX_DEPOSIT_GAS;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_MAX_WITHDRAWALS;
import static network.bane.util.helper.DefaultTestValues.DEFAULT_MIN_DEPOSIT_GAS;
import static network.bane.util.helper.DefaultTestValues.DUMMY_TARGET_CONTRACT_HASH;

public class TokenHelper {

    public static TokenBridge.TokenConfig dummyTokenConfig() {
        return new TokenBridge.TokenConfig(
                DUMMY_TARGET_CONTRACT_HASH,
                DEFAULT_DEPOSIT_FEE,
                DEFAULT_MIN_DEPOSIT_GAS,
                DEFAULT_MAX_DEPOSIT_GAS,
                DEFAULT_MAX_WITHDRAWALS,
                0);
    }
}
