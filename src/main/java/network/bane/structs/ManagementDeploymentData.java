package network.bane.structs;

import io.neow3j.devpack.ECPoint;
import io.neow3j.devpack.annotations.Struct;

@Struct
public class ManagementDeploymentData {

    public ECPoint[] owners;
    public ECPoint[] relayers;

}
