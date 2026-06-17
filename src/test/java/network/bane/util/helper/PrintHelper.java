package network.bane.util.helper;

import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.Notification;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import network.bane.util.BridgeTestClient;
import network.bane.util.TestHelper;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.neow3j.utils.Numeric.prependHexPrefix;

public class PrintHelper {

    public static void printTransactionFee(Neow3j neow3j, String description, Hash256 txHash) throws IOException {
        io.neow3j.protocol.core.response.Transaction tx =
                neow3j.getTransaction(txHash).send().getTransaction();
        BigInteger totalFee = new BigInteger(tx.getSysFee()).add(new BigInteger(tx.getNetFee()));
        System.out.printf("Transaction fee (%s): %s\n", description, totalFee);
    }

    public static void printDepositEvent(BridgeTestClient bridge, Neow3j neow3j, Hash256 txHash, List<String> proof) throws IOException {
        Optional<Notification> onDepositOpt =
                neow3j.getApplicationLog(txHash).send().getApplicationLog().getFirstExecution()
                        .getNotifications().stream()
                        .filter(n -> n.getContract().equals(bridge.getScriptHash()) &&
                                n.getEventName().equals("OnNativeDeposit"))
                        .findFirst();
        if (onDepositOpt.isPresent()) {
            Notification depositNotification = onDepositOpt.get();
            TestHelper.DepositEvent depositEvent = TestHelper.depositEventFromNotification(depositNotification);
            BigInteger nonce = depositEvent.nonce;
            Hash160 to = depositEvent.to;
            BigInteger amount = depositEvent.amount;
            String root = depositEvent.rootHashHex;
            System.out.printf("new DepositProof(" +
                            "%sn," +
                            "\"%s\"," +
                            "%sn," +
                            "[%s]," +
                            "\"%s\"" +
                            ")%n",
                    nonce, prependHexPrefix(to.toString()), amount, wrapWithQuotesAndJoin(proof), root);
        }
    }

    private static String wrapWithQuotesAndJoin(List<String> strings) {
        String joined = strings.stream().collect(Collectors.joining("\", \"", "\"", "\""));
        if (joined.length() == 2) {
            return "";
        } else {
            return joined;
        }
    }

}
