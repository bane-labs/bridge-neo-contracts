package network.bane.utils;

import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoGetVersion;
import io.neow3j.types.Hash160;

import java.io.IOException;

public class PrintHelper {

    private static final Long MAINNET_MAGIC = 860833102L;
    private static final Long TESTNET_MAGIC = 894710606L;

    public static void printNetwork(Neow3j neow3j) throws IOException {
        NeoGetVersion.NeoVersion.Protocol protocol = neow3j.getVersion().send().getVersion().getProtocol();
        if (protocol.getNetwork().equals(MAINNET_MAGIC)) {
            System.out.printf("Connected to Neo Mainnet (%s)%n", MAINNET_MAGIC);
        } else if (protocol.getNetwork().equals(TESTNET_MAGIC)) {
            System.out.printf("Connected to Neo Testnet (%s)%n", TESTNET_MAGIC);
        } else {
            System.out.printf("Connected to private network (%s)", protocol.getNetwork());
        }
    }

    public static void printSender(Hash160 scriptHash) {
        System.out.printf("Using sender %s (%s)%n", scriptHash.toAddress(), scriptHash);
    }

}
