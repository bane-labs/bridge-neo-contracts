package network.bane.structs;

import io.neow3j.devpack.ECPoint;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.List;
import io.neow3j.devpack.annotations.Struct;

@Struct
public class ManagementDeploymentData {

    public Hash160 owner;
    public Hash160 relayer;
    public List<ECPoint> validators;
    public int validatorThreshold;
    public Hash160 governor;
    public Hash160 securityGuard;

}
