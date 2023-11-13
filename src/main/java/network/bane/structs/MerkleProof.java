package network.bane.structs;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.List;
import io.neow3j.devpack.annotations.Struct;

@Struct
public class MerkleProof {
    public Integer nonce;
    public Hash160 recipient;
    public Integer amount;
    public Integer path;
    public List<ByteString> proof;

    public static boolean isValid(MerkleProof merkleProof) {
        return merkleProof.nonce >= 0 &&
                Hash160.isValid(merkleProof.recipient) &&
                merkleProof.amount >= 0 &&
                merkleProof.path != null &&
                merkleProof.proof != null;
    }

}
