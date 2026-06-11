package network.bane.utils.bridge;

import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;

import java.io.IOException;
import java.math.BigInteger;

class BridgeGeneralModule implements BridgeGeneralOps {

    private final BridgeClientBase base;

    BridgeGeneralModule(BridgeClientBase base) {
        this.base = base;
    }

    @Override
    public Hash256 update(Account sender, byte[] nefBytes, String manifestJson, Object data) throws Throwable {
        return base.update(sender, nefBytes, manifestJson, data);
    }

    @Override
    public Hash256 pauseBridge(Account sender) throws Throwable {
        return base.invokeWrite(sender, "pauseBridge");
    }

    @Override
    public Hash256 unpauseBridge(Account sender) throws Throwable {
        return base.invokeWrite(sender, "unpauseBridge");
    }

    @Override
    public boolean isPaused() throws IOException {
        return base.callFunctionReturningBool("isPaused");
    }

    @Override
    public Hash256 pauseDeposits(Account sender) throws Throwable {
        return base.invokeWrite(sender, "pauseDeposits");
    }

    @Override
    public Hash256 unpauseDeposits(Account sender) throws Throwable {
        return base.invokeWrite(sender, "unpauseDeposits");
    }

    @Override
    public boolean depositsArePaused() throws IOException {
        return base.callFunctionReturningBool("depositsArePaused");
    }

    @Override
    public BigInteger linkedChainId() throws IOException {
        return base.callFunctionReturningInt("linkedChainId");
    }

    @Override
    public Hash160 management() throws IOException {
        return base.callFunctionReturningScriptHash("management");
    }

    @Override
    public BigInteger unclaimedRewards() throws IOException {
        return base.callFunctionReturningInt("unclaimedRewards");
    }

    @Override
    public BigInteger neoHoldingGasRewards() throws IOException {
        return base.callFunctionReturningInt("neoHoldingGasRewards");
    }
}
