package network.bane.structs;

import io.neow3j.devpack.ECPoint;
import io.neow3j.devpack.List;
import io.neow3j.devpack.annotations.Struct;

@Struct
public class ManagementDeploymentData {

    public ECPoint owner;
    public ECPoint relayer;
    public List<ECPoint> validators;
    public int validatorThreshold;
    public ECPoint governor;

}
