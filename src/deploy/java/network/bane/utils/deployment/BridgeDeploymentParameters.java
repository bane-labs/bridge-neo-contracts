package network.bane.utils.deployment;

import io.neow3j.crypto.ECKeyPair;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;

import java.math.BigInteger;
import java.util.List;

import static io.neow3j.types.ContractParameter.*;

public class BridgeDeploymentParameters {

    public static ContractParameter prepareManagementDeployParameter(Hash160 owner, Hash160 relayer,
            List<ECKeyPair.ECPublicKey> validators, Integer threshold, Hash160 governor, Hash160 securityGuard) {
        return array(hash160(owner), hash160(relayer), array(validators), integer(threshold), hash160(governor),
                hash160(securityGuard));
    }

    public static ContractParameter prepareBridgeDeployParameter(Hash160 managementContractHash,
            BigInteger depositFee, BigInteger minDeposit, BigInteger maxDeposit, BigInteger maxTotalDeposited) {
        return array(hash160(managementContractHash), array(integer(depositFee), integer(minDeposit),
                integer(maxDeposit), integer(100), integer(maxTotalDeposited)));
    }

    public static ContractParameter prepareMessageBridgeDeployParameter(
            BigInteger linkedChainId,
            Hash160 managementContractHash,
            Hash160 executionManagerContractHash
    ) {
        return array(
                integer(linkedChainId), hash160(managementContractHash), array(hash160(executionManagerContractHash))
        );
    }

    public static ContractParameter prepareExecutionManagerDeployParameter(
            Hash160 managementContractHash,
            Hash160 messageBridgeContractHash
    ) {
        return array(hash160(managementContractHash), hash160(messageBridgeContractHash));
    }

}
