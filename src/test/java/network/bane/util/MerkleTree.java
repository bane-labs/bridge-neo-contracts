package network.bane.util;

import java.util.ArrayList;
import java.util.List;

import static io.neow3j.utils.Numeric.*;
import static network.bane.util.TestHelper.*;

public class MerkleTree {
    private List<String> leaves;
    private List<List<String>> layers;

    public MerkleTree(List<String> leafList) {
        this.leaves = leafList;
        this.layers = new ArrayList<>();
        processLeaves(this.leaves);
    }

    private void processLeaves(List<String> leafList){
            this.layers.add(leafList);
            List<String> nodeList = leafList;
            while (nodeList.size() > 1) {
                int layerIndex = this.layers.size();
                this.layers.add(new ArrayList<>());
                for (int i = 0; i < nodeList.size(); i += 2) {
                    if(i + 1 == nodeList.size()){
                        this.layers.get(layerIndex).add(nodeList.get(i));
                        continue;
                    }

                    String left = nodeList.get(i);
                    String right = nodeList.get(i + 1);

                    String combine = sha256Hex(concatLeftRight(left, right));
                    this.layers.get(layerIndex).add(combine);
                }
                nodeList = this.layers.get(layerIndex);
            }
    }

    public Integer findLeavesIndex(List<String> leaves, String hashLeaf){
        for (int i = 0; i < leaves.size(); i++) {
            if(cleanHexPrefix(hashLeaf).equals(cleanHexPrefix(leaves.get(i)))){
                return i;
            }
        }
        return -1;
    }

    public WithdrawalWithProof getProof(String leaf){
        Integer path = 0;
        List<byte[]> proofList = new ArrayList<>();
        Integer leavesIndex = findLeavesIndex(this.leaves, leaf);

        if(leavesIndex == -1){
            System.out.println("not found in leaves");
            return null;
        }
        for (int i = 0; i < this.layers.size() - 1; i++) {
            List<String> layer = this.layers.get(i);
            boolean isRightNode = leavesIndex % 2 == 1;
            int pairIndex = isRightNode? leavesIndex - 1 : (leavesIndex == layer.size()-1 ? leavesIndex: leavesIndex + 1);
            if(pairIndex < layer.size()){
                if(!(leavesIndex == (layer.size()-1))){
                    proofList.add(hexStringToByteArray(layer.get(pairIndex)));
                    if (!isRightNode) {
                        path = (path << 1) | 0x01;
                    } else {
                        path = path << 1;
                    }
                }
            }
            if(this.layers.size() != (i+1)){
                if(isRightNode){
                    leaf =  cleanHexPrefix(concatAndSha256(layer.get(pairIndex), layer.get(leavesIndex)));
                }else {
                    leaf =  cleanHexPrefix(concatAndSha256(layer.get(leavesIndex), layer.get(pairIndex)));
                }
                if(leavesIndex == (layer.size()-1)&&!isRightNode){
                    leaf = layer.get(leavesIndex);
                }
                leavesIndex = findLeavesIndex(this.layers.get(i+1), leaf);
            }

        }
        int base = (int)(Math.log(leaves.size()) / Math.log(2));
        if (path <= (Math.pow(2, base-1)-1)){
            path = path << 1 ;
        }
        return new WithdrawalWithProof(proofList, path);
    }

    public String getRoot() {
        if(this.layers.size() == 0){
            return new String();
        }
        return this.layers.get(this.layers.size()-1).get(0);
    }

    public boolean verify(WithdrawalWithProof withdrawalWithProof, String root, String leaf){
        int path = withdrawalWithProof.path;

        String parent = leaf;
        List<byte[]> proof = withdrawalWithProof.proof;
        int height = 0;
        for (byte[] proofEntry : proof) {
            if (((path >> height) & 0x01) == 1) {
                parent = concatAndSha256(parent, toHexString(proofEntry));
            } else {
                parent = concatAndSha256(toHexString(proofEntry), parent);
            }
            height += 1;
        }
        return parent.equals(root);
    }
}