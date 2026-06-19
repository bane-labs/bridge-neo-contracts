package network.bane.client;

import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.transaction.Signer;
import io.neow3j.transaction.Transaction;
import io.neow3j.transaction.TransactionBuilder;
import io.neow3j.types.Hash256;
import network.bane.client.interfaces.IWriteCaller;

import java.io.PrintStream;

import static io.neow3j.utils.Await.waitUntilTransactionIsExecuted;

/**
 * Default {@link IWriteCaller} implementation backed by a neow3j {@link TransactionBuilder}.
 * <p>
 * The wrapped builder is mutable. Fluent configuration methods mutate the underlying builder and return this same
 * instance.
 */
public class WriteCall implements IWriteCaller {

    private Neow3j neow3j;
    private TransactionBuilder transactionBuilder;

    /**
     * Creates a prepared write call around the given transaction builder.
     * <p>
     * The builder is expected to already contain the target contract invocation and its contract parameters.
     *
     * @param neow3j             the {@link Neow3j} instance to use for sending the transaction and waiting for its
     *                           execution.
     * @param transactionBuilder the transaction builder representing the prepared contract invocation.
     */
    public WriteCall(Neow3j neow3j, TransactionBuilder transactionBuilder) {
        this.neow3j = neow3j;
        this.transactionBuilder = transactionBuilder;
    }

    /**
     * Factory method for creating a prepared write call.
     *
     * @param neow3j             the {@link Neow3j} instance to use for sending the transaction and waiting for its
     *                           execution.
     * @param transactionBuilder the transaction builder representing the prepared contract invocation.
     * @return a write call wrapping the given transaction builder.
     */
    public static WriteCall newWriteCall(Neow3j neow3j, TransactionBuilder transactionBuilder) {
        return new WriteCall(neow3j, transactionBuilder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransactionBuilder getTransactionBuilder() {
        return transactionBuilder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWriteCaller withSigners(Signer... signers) {
        transactionBuilder.signers(signers);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Hash256 signSendAndAwait() throws Throwable {
        return signSendAndAwait(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Hash256 signSendAndAwait(PrintStream out) throws Throwable {
        Transaction tx = transactionBuilder.sign();
        NeoSendRawTransaction response = tx.send();
        if (response.hasError()) {
            throw new RuntimeException("Error sending transaction: " + response.getError().getMessage());
        }
        Hash256 txHash = response.getSendRawTransaction().getHash();
        if (out != null) {
            out.println("Transaction sent: " + txHash);
        }
        waitUntilTransactionIsExecuted(txHash, neow3j);
        if (out != null) {
            out.println("Transaction confirmed in block: " + neow3j.getTransactionHeight(txHash).send().getHeight());
        }
        return txHash;
    }
}
