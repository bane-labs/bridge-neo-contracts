package network.bane.client;

import io.neow3j.contract.GasToken;
import io.neow3j.contract.NefFile;
import io.neow3j.crypto.Base64;
import io.neow3j.crypto.ECKeyPair;
import io.neow3j.crypto.Sign;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.ContractManifest;
import io.neow3j.protocol.core.response.ContractStorageEntry;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;
import network.bane.dto.message.MessageEnvelope;
import network.bane.dto.message.N3Message;
import network.bane.dto.message.N3MessageMetadataExec;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.transaction.AccountSigner.none;
import static io.neow3j.utils.Numeric.toHexString;
import static java.util.Arrays.asList;
import static network.bane.support.hash.HashChainHelper.concatAndKeccak256;
import static network.bane.support.hash.MessageBridgeHashChainHelper.createN3MessageHash;
import static network.bane.support.crypto.SignHelper.signMsg;
import static network.bane.support.TestConstants.validator1;
import static network.bane.support.TestConstants.validator2;
import static network.bane.support.TestConstants.validator3;
import static network.bane.support.TestConstants.validator4;
import static network.bane.support.TestConstants.validator5;

public class MessageBridgeTestClient extends MessageBridgeClient {

    public MessageBridgeTestClient(Hash160 scriptHash, Neow3j neow3j) {
        super(scriptHash, neow3j);
    }

    public Hash256 update(Account sender, NefFile nefFile, ContractManifest manifest, Object data) throws Throwable {
        return update(nefFile, manifest, data).withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 pause(Account sender) throws Throwable {
        return pause().withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 unpause(Account sender) throws Throwable {
        return unpause().withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 pauseSending(Account sender) throws Throwable {
        return pauseSending().withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 unpauseSending(Account sender) throws Throwable {
        return unpauseSending().withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 pauseExecuting(Account sender) throws Throwable {
        return pauseExecuting().withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 unpauseExecuting(Account sender) throws Throwable {
        return unpauseExecuting().withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 sendExecutableMessage(Account sender, byte[] rawMessage, boolean storeResult) throws Throwable {
        return sendExecutableMessage(rawMessage, storeResult, sender.getScriptHash(), sendingFee())
                .withSigners(none(sender).setAllowedContracts(GasToken.SCRIPT_HASH))
                .signSendAndAwait();
    }

    public Hash256 sendStoreOnlyMessage(Account sender, byte[] rawMessage) throws Throwable {
        return sendStoreOnlyMessage(rawMessage, sender.getScriptHash(), sendingFee())
                .withSigners(none(sender).setAllowedContracts(GasToken.SCRIPT_HASH))
                .signSendAndAwait();
    }

    public Hash256 sendResultMessage(Account sender, BigInteger relatedMessageNonce) throws Throwable {
        return sendResultMessage(relatedMessageNonce, sender.getScriptHash(), sendingFee())
                .withSigners(none(sender).setAllowedContracts(GasToken.SCRIPT_HASH))
                .signSendAndAwait();
    }

    public Hash256 storeMessages(Account sender, String evmToNeoRoot,
            Map<ECKeyPair.ECPublicKey, Sign.SignatureData> signatures, List<MessageEnvelope> messages)
            throws Throwable {
        return storeMessages(evmToNeoRoot, signatures, messages)
                .withSigners(calledByEntry(sender))
                .signSendAndAwait();
    }

    public BigInteger storeMessageAndGetNonce(Account sender, byte[] n3FuncCall) throws Throwable {
        Hash256 bestBlockHash = neow3j.getBestBlockHash().send().getBlockHash();
        long bestBlockTime = neow3j.getBlockHeader(bestBlockHash).send().getBlock().getTime();
        BigInteger timestamp = BigInteger.valueOf(bestBlockTime);

        BigInteger nonce = evmToNeoNonce().add(BigInteger.ONE);
        N3MessageMetadataExec metadata = new N3MessageMetadataExec(timestamp, sender.getScriptHash(), true);
        N3Message n3Message = new N3Message(metadata, n3FuncCall);

        String hash = createN3MessageHash(nonce, metadata, n3FuncCall);
        String root = concatAndKeccak256(evmToNeoRoot(), hash);
        MessageEnvelope messageEnvelope = new MessageEnvelope(nonce, n3Message);
        List<Account> validators = asList(validator1, validator2, validator3, validator4, validator5);
        storeMessages(sender, root, signMsg(validators, root), asList(messageEnvelope));
        return messageEnvelope.getNonce();
    }

    public Hash256 executeMessage(Account sender, BigInteger nonce) throws Throwable {
        return executeMessage(nonce).withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 setSendingFee(Account sender, BigInteger newFee) throws Throwable {
        return setSendingFee(newFee).withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 setMaxMessageSize(Account sender, BigInteger newMaxBytes) throws Throwable {
        return setMaxMessageSize(newMaxBytes).withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 setMaxNrMessages(Account sender, BigInteger newMaxNrMessages) throws Throwable {
        return setMaxNrMessages(newMaxNrMessages).withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 setExecutionManager(Account sender, Hash160 newExecManager) throws Throwable {
        return setExecutionManager(newExecManager).withSigners(calledByEntry(sender)).signSendAndAwait();
    }

    public Hash256 setExecutionWindowMilliseconds(Account sender, BigInteger newExecWindowMillis) throws Throwable {
        return setExecutionWindowMilliseconds(newExecWindowMillis)
                .withSigners(calledByEntry(sender))
                .signSendAndAwait();
    }

    // Generic

    public List<ContractStorageEntry> findStorage(String prefixHex) throws IOException {
        return neow3j.findStorage(scriptHash, prefixHex, BigInteger.ZERO).send().getFoundStorage().getStorageEntries();
    }

    public String getStorage(String keyHex) throws IOException {
        byte[] storageBytes = Base64.decode(neow3j.getStorage(scriptHash, keyHex).send().getStorage());
        return toHexString(storageBytes);
    }

    public BigInteger getNextNeoToEvmNonce() throws IOException {
        return neoToEvmNonce().add(BigInteger.ONE);
    }

    public BigInteger getNextEvmToNeoNonce() throws IOException {
        return evmToNeoNonce().add(BigInteger.ONE);
    }

}
