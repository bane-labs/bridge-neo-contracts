package network.bane.utils.deployment;

import io.neow3j.crypto.ECKeyPair;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;

import java.math.BigInteger;
import java.util.List;

import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;

public class BridgeDeploymentParameters {

    public static ContractParameter prepareManagementDeployParameter(Hash160 owner, Hash160 relayer,
            List<ECKeyPair.ECPublicKey> validators, Integer threshold, Hash160 governor, Hash160 securityGuard) {
        return array(
                hash160(owner),
                hash160(relayer),
                array(validators),
                integer(threshold),
                hash160(governor),
                hash160(securityGuard)
        );
    }

    public static ContractParameter prepareBridgeDeployParameter(BigInteger linkedChain, Hash160 managementContract) {
        return array(integer(linkedChain), hash160(managementContract));
    }

    public static ContractParameter prepMsgBridgeDeployParam(BigInteger linkedChain, Hash160 managementContract,
            Hash160 execManagerHash) {
        return array(integer(linkedChain), hash160(managementContract), hash160(execManagerHash));
    }

    public static ContractParameter prepExecManagerDeployParam(Hash160 managementContract, Hash160 messageBridgeHash) {
        return array(hash160(managementContract), hash160(messageBridgeHash));
    }

}
