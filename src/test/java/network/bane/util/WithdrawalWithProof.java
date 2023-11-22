package network.bane.util;

import java.util.List;

public class WithdrawalWithProof {

    public List<byte[]> proof;
    public Integer path;

    public WithdrawalWithProof(List<byte[]> proof, Integer path) {
        this.proof = proof;
        this.path = path;
    }
}
