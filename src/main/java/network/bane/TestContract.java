package network.bane;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.annotations.DisplayName;
import io.neow3j.devpack.annotations.ManifestExtra;
import io.neow3j.devpack.annotations.OnNEP17Payment;

@DisplayName("BaneBridge")
@ManifestExtra(key = "author", value = "BaneLabs")
@ManifestExtra(key = "description", value = "Test Contract for bridging GAS tokens from Neo N3 to Bane.")
public class TestContract {

    @OnNEP17Payment
    public static void onNep17Payment(Hash160 from, int amount, Object data) {
    }
}