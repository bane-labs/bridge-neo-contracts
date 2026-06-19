package network.bane.client.interfaces;

import io.neow3j.transaction.Signer;
import io.neow3j.transaction.TransactionBuilder;
import io.neow3j.types.Hash256;

import java.io.PrintStream;

/**
 * Prepared write-call abstraction for an on-chain contract invocation.
 * <p>
 * A {@code WriteCaller} represents a contract write that has already been prepared with the target method and contract
 * parameters, but has not necessarily been configured with signers, signed, sent, or awaited yet.
 * <p>
 * This keeps client write methods aligned with the actual contract method signatures: contract arguments are passed to
 * the client method, while execution metadata such as signers is configured on the returned {@code WriteCaller}.
 */
public interface IWriteCaller {

    /**
     * Returns the underlying transaction builder.
     * <p>
     * This is an escape hatch for advanced transaction customization that is not represented directly on this
     * abstraction.
     *
     * @return this call's transaction builder.
     */
    TransactionBuilder getTransactionBuilder();

    /**
     * Configures the signers that should be attached to this write call.
     * <p>
     * Signers are execution metadata and are intentionally configured separately from the contract method arguments.
     *
     * @param signers signers to attach to the transaction builder.
     * @return this write-caller for fluent configuration.
     */
    IWriteCaller withSigners(Signer... signers);

    /**
     * Signs and sends the prepared transaction, waits until it is executed, and returns the transaction hash.
     * <p>
     * Implementations should centralize send-error handling and block-awaiting logic so deploy and test scripts do not
     * need to repeat it for every write call.
     *
     * @return the transaction hash.
     * @throws Throwable if signing, sending, send-error handling, or awaiting execution fails.
     */
    Hash256 signSendAndAwait() throws Throwable;

    /**
     * Signs and sends the prepared transaction, prints progress to the given stream, waits until it is executed, and
     * returns the transaction hash.
     * <p>
     * This is intended for CLI/deploy scripts that want immediate feedback after the transaction was accepted by the
     * RPC node and another message after block confirmation. Reusable client and test code should prefer the silent
     * {@link #signSendAndAwait()} overload unless it explicitly wants output.
     *
     * @param out stream to print transaction progress to.
     * @return the transaction hash.
     * @throws Throwable if signing, sending, send-error handling, printing confirmation details, or awaiting execution
     *                   fails.
     */
    default Hash256 signSendAndAwait(PrintStream out) throws Throwable {
        return signSendAndAwait();
    }
}
