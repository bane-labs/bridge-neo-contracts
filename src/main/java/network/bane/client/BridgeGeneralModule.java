package network.bane.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.neow3j.contract.NefFile;
import io.neow3j.protocol.core.response.ContractManifest;
import io.neow3j.types.Hash160;
import network.bane.client.interfaces.IBridgeGeneralOps;
import network.bane.client.interfaces.IWriteCaller;

import java.io.IOException;
import java.math.BigInteger;

import static io.neow3j.protocol.ObjectMapperFactory.getObjectMapper;
import static io.neow3j.types.ContractParameter.any;
import static io.neow3j.types.ContractParameter.byteArray;

class BridgeGeneralModule implements IBridgeGeneralOps {

    private final BridgeClientBase base;

    BridgeGeneralModule(BridgeClientBase base) {
        this.base = base;
    }

    @Override
    public IWriteCaller update(NefFile nefFile, ContractManifest manifest, Object data) throws JsonProcessingException {
        byte[] nefBytes = nefFile.toArray();
        byte[] manifestBytes = getObjectMapper().writeValueAsBytes(manifest);
        return base.invokeWrite("update", byteArray(nefBytes), byteArray(manifestBytes), any(data));
    }

    @Override
    public IWriteCaller pauseBridge() {
        return base.invokeWrite("pauseBridge");
    }

    @Override
    public IWriteCaller unpauseBridge() {
        return base.invokeWrite("unpauseBridge");
    }

    @Override
    public boolean isPaused() throws IOException {
        return base.callFunctionReturningBool("isPaused");
    }

    @Override
    public IWriteCaller pauseDeposits() {
        return base.invokeWrite("pauseDeposits");
    }

    @Override
    public IWriteCaller unpauseDeposits() {
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
