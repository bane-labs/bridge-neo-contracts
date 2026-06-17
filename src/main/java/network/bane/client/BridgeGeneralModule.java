package network.bane.client;

import io.neow3j.types.Hash160;
import network.bane.client.interfaces.WriteCaller;

import java.io.IOException;
import java.math.BigInteger;

import static io.neow3j.types.ContractParameter.any;
import static io.neow3j.types.ContractParameter.byteArray;
import static java.nio.charset.StandardCharsets.UTF_8;

class BridgeGeneralModule implements BridgeGeneralOps {

    private final BridgeClientBase base;

    BridgeGeneralModule(BridgeClientBase base) {
        this.base = base;
    }

    @Override
    public WriteCaller update(byte[] nefBytes, String manifestJson, Object data) {
        return base.invokeWrite("update", byteArray(nefBytes), byteArray(manifestJson.getBytes(UTF_8)), any(data));
    }

    @Override
    public WriteCaller pauseBridge() {
        return base.invokeWrite("pauseBridge");
    }

    @Override
    public WriteCaller unpauseBridge() {
        return base.invokeWrite("unpauseBridge");
    }

    @Override
    public boolean isPaused() throws IOException {
        return base.callFunctionReturningBool("isPaused");
    }

    @Override
    public WriteCaller pauseDeposits() {
        return base.invokeWrite("pauseDeposits");
    }

    @Override
    public WriteCaller unpauseDeposits() {
        return base.invokeWrite("unpauseDeposits");
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
