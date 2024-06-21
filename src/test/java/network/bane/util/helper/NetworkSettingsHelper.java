package network.bane.util.helper;

import io.neow3j.contract.PolicyContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.Hash256;
import io.neow3j.utils.Await;

import java.io.IOException;
import java.math.BigInteger;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static network.bane.bridge.BridgeTest.alice;
import static network.bane.bridge.BridgeTest.committee;

public class NetworkSettingsHelper {

    // The current network settings on mainnet
    public static final BigInteger networkFeePerByte = new BigInteger("100");
    public static final BigInteger storageFeeFactor = new BigInteger("10000");
    public static final BigInteger executionFeeFactor = new BigInteger("3");

    public static void updateNetworkSettings(Neow3j neow3j) throws Throwable {
        setNetworkFeePerByte(neow3j);
        setStorageFeeFactor(neow3j);
        setExecutionFeeFactor(neow3j);
        printNetworkSettings(neow3j);
    }

    private static void printNetworkSettings(Neow3j neow3j) throws IOException {
        PolicyContract policyContract = new PolicyContract(neow3j);
        System.out.println("\n################");
        System.out.println("Network Settings");
        System.out.println("----------------");
        System.out.println("Network fee per byte: " + policyContract.getFeePerByte());
        System.out.println("Storage fee factor:   " + policyContract.getStoragePrice());
        System.out.println("Execution fee factor: " + policyContract.getExecFeeFactor());
        System.out.println("################\n");
    }

    private static void setNetworkFeePerByte(Neow3j neow3j) throws Throwable {
        PolicyContract policyContract = new PolicyContract(neow3j);
        Transaction tx = policyContract.setFeePerByte(networkFeePerByte)
                .signers(calledByEntry(committee))
                .getUnsignedTransaction();
        tx.addMultiSigWitness(committee.getVerificationScript(), alice);
        Hash256 txHash = tx.send().getSendRawTransaction().getHash();
        Await.waitUntilTransactionIsExecuted(txHash, neow3j);
    }

    private static void setStorageFeeFactor(Neow3j neow3j) throws Throwable {
        PolicyContract policyContract = new PolicyContract(neow3j);
        Transaction tx = policyContract.setStoragePrice(storageFeeFactor)
                .signers(calledByEntry(committee))
                .getUnsignedTransaction();
        tx.addMultiSigWitness(committee.getVerificationScript(), alice);
        Hash256 txHash = tx.send().getSendRawTransaction().getHash();
        Await.waitUntilTransactionIsExecuted(txHash, neow3j);
    }

    private static void setExecutionFeeFactor(Neow3j neow3j) throws Throwable {
        PolicyContract policyContract = new PolicyContract(neow3j);
        Transaction tx = policyContract.setExecFeeFactor(executionFeeFactor)
                .signers(calledByEntry(committee))
                .getUnsignedTransaction();
        tx.addMultiSigWitness(committee.getVerificationScript(), alice);
        Hash256 txHash = tx.send().getSendRawTransaction().getHash();
        Await.waitUntilTransactionIsExecuted(txHash, neow3j);
    }

}
