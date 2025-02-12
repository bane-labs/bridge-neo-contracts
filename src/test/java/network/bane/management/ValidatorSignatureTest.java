package network.bane.management;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static network.bane.util.TestHelper.createWithdrawalMessageToSign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ValidatorSignatureTest {

    @Test
    public void testWithdrawalSigMessage() {
        String root = "295226ccd2cee4e7fef5bc9d18677c6228052b46109731fc25dbcd6982074a0e";

        BigInteger n3Network = new BigInteger("894710606");
        BigInteger sourceChainId = new BigInteger("12227332");

        String msg = createWithdrawalMessageToSign(n3Network, sourceChainId, root);
        assertThat(msg, is("4e3354350493ba00295226ccd2cee4e7fef5bc9d18677c6228052b46109731fc25dbcd6982074a0e"));
    }
}
