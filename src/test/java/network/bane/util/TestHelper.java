package network.bane.util;

import io.neow3j.crypto.ECKeyPair;
import io.neow3j.crypto.ECKeyPair.ECPublicKey;
import io.neow3j.crypto.Hash;
import io.neow3j.crypto.Sign;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.protocol.core.response.Notification;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.utils.ArrayUtils;
import io.neow3j.utils.Await;
import io.neow3j.utils.BigIntegers;
import io.neow3j.wallet.Account;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.neow3j.devpack.Helper.concat;
import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.types.ContractParameter.publicKey;
import static io.neow3j.types.ContractParameter.signature;
import static io.neow3j.utils.ArrayUtils.concatenate;
import static io.neow3j.utils.Numeric.cleanHexPrefix;
import static io.neow3j.utils.Numeric.hexStringToByteArray;
import static io.neow3j.utils.Numeric.prependHexPrefix;
import static io.neow3j.utils.Numeric.toHexString;
import static java.lang.String.format;
import static java.util.Arrays.asList;

public class TestHelper {

    private static final byte UINT256_SIZE = 32;

    // Account names available in the neo-express config file.
    public static final String ALICE = "NM7Aky765FG8NhhwtxjXRx7jEL1cnw7PBP";
    public static final String BOB = "NZpsgXn9VQQoLexpuXJsrX8BsoyAhKUyiX";
    public static final String CHARLIE = "NdbtgSku2qLuwsBBzLx3FLtmmMdm32Ktor";
    public static final String DENISE = "NerDv9t8exrQRrP11jjvZKXzSXvTnmfDTo";
    public static final String EVE = "NZ539Rd57v5NEtAdkHyFGaWj1uGt2DecUL";
    public static final String FLORIAN = "NRy5bp81kScYFZHLfMBXuubFfRyboVyu7G";
    public static final String GABRIEL = "Ne9bqEY829xTM5gUxiaZQ9SuvvyE7dAvC3";
    public static final String HENRY = "NUWnAzbtHRGaBJWvMag9BR5fo6hG4so87Y";
    public static final String ISABELLA = "NQ71wQ2GJbQhQr9Je66YVPmAwckkMGRu74";

    public static final Account owner = Account.fromWIF("KxmdmDryNcxiAjk4QTVgei11251NxJhZmN25q5H3QYrwfJJ1Tyrs");
    public static final ECPublicKey ownerPubKey = owner.getECKeyPair().getPublicKey();
    public static final Hash160 ownerScriptHash = owner.getScriptHash();

    public static final Account relayer = Account.fromWIF("L5iiAW1NicU3znJfcBAbDgFyVy9dudd1HjBLeNtacaG73JcjMymU");
    public static final ECPublicKey relayerPubKey = relayer.getECKeyPair().getPublicKey();
    public static final Hash160 relayerScriptHash = relayer.getScriptHash();

    public static final Account validator1 = Account.fromWIF("L46dW4Z8KEvURXaE1YrqsgNSaQ4G2B4uN97uNeyhJp1VL7UjLUpb");
    public static final ECPublicKey validator1PubKey = validator1.getECKeyPair().getPublicKey();
    public static final Hash160 validator1ScriptHash = validator1.getScriptHash();
    public static final Account validator2 = Account.fromWIF("Kz9FY9FcmP1HpKPBNACYeZGY2dRv5DKDMgSrM3DA3ZhxmQXduqCj");
    public static final ECPublicKey validator2PubKey = validator2.getECKeyPair().getPublicKey();
    public static final Hash160 validator2ScriptHash = validator2.getScriptHash();
    public static final Account validator3 = Account.fromWIF("KzrpcAzUr8QFkE83rhGb55XNq5bP6Nze63J54GG61zbu5Un62HjZ");
    public static final ECPublicKey validator3PubKey = validator3.getECKeyPair().getPublicKey();
    public static final Hash160 validator3ScriptHash = validator3.getScriptHash();
    public static final Account validator4 = Account.fromWIF("Kyc8rihRGy3pT8z24zMdUV7zPgNaCtrLhmpFTJEEk8QkASnzp2o6");
    public static final ECPublicKey validator4PubKey = validator4.getECKeyPair().getPublicKey();
    public static final Hash160 validator4ScriptHash = validator4.getScriptHash();
    public static final Account validator5 = Account.fromWIF("KxaXJG4Vt6hbZrLdjCdHG1LJYUJaE8knPkefxw8uyDVuNXh4RYXM");
    public static final ECPublicKey validator5PubKey = validator5.getECKeyPair().getPublicKey();
    public static final Hash160 validator5ScriptHash = validator5.getScriptHash();
    public static final Account validator6 = Account.fromWIF("KyvW6BeMagHm1jUnBytHEsZNvZU7mmJejoybK9RWzzyjxGeRJbdj");
    public static final ECPublicKey validator6PubKey = validator6.getECKeyPair().getPublicKey();
    public static final Hash160 validator6ScriptHash = validator6.getScriptHash();
    public static final Account validator7 = Account.fromWIF("L5kcMEa2zFagyEF9TnVGi3Y5uP4NeCaEnQyZGo93wiv8aAAPTVWk");
    public static final ECPublicKey validator7PubKey = validator7.getECKeyPair().getPublicKey();
    public static final Hash160 validator7ScriptHash = validator7.getScriptHash();

    public static final List<ECPublicKey> defaultValidators = asList(
            validator1PubKey,
            validator2PubKey,
            validator3PubKey,
            validator4PubKey,
            validator5PubKey,
            validator6PubKey,
            validator7PubKey
    );
    public static int defaultValidatorThreshold = 5;

    public static final Account governor = Account.fromWIF("L31FLxpHiSuZLzjzJAVY5z9gSu25pZxRB2yBCzqjc3JDUz2GkLBH");
    public static final ECPublicKey governorPubKey = governor.getECKeyPair().getPublicKey();
    public static final Hash160 governorScriptHash = governor.getScriptHash();

    public static final Account securityGuard = Account.fromWIF("L287EDF2W9Eq7ojFtC5dWvnsLDNGCjMR2hBX3qa1FkfWt5Uzkf9Y");
    public static final ECPublicKey securityGuardPubKey = securityGuard.getECKeyPair().getPublicKey();
    public static final Hash160 securityGuardScriptHash = securityGuard.getScriptHash();

    public static final Account account0 = Account.fromWIF("Kzczq8Bd3h6ukXs4tgTkGd6jDeETcm18VTx3gS8ZcC5DobB25sGm");
    public static final Account account1 = Account.fromWIF("L49FvTGZTdSJbck687Kg9wrjBzQBJ1asnvKWqcvJ2sJcyP9U9rbJ");
    public static final Account account2 = Account.fromWIF("KwPnYQg2VFMq2Jn62DTVKYCg7TGdzFWvsfKkzKYgPi2LSJcxEW7D");
    public static final Account account3 = Account.fromWIF("KzLcmDDahZdkZPgbwgicjMEASo6qLP5B6TtpvdDT9hd4pgjmSMms");
    public static final Account account4 = Account.fromWIF("KxpH1KLjefnEt7Xr5wkDonQimg4DvjXGh5FWVbgDgmtp9FJSkui9");
    public static final Account account5 = Account.fromWIF("KwuK42Lwmkv3hhTSWoiUfrbUMzkQ4DZqbcQPf9C3X8DxZSXvKmia");
    public static final Account account6 = Account.fromWIF("KxvThFfpAQjo74b2onr9DuNRSp7J4PDiihdkKGRutn51foSA6yHs");
    public static final Account account7 = Account.fromWIF("L2Kmh1imZCNiDBgLztASH8NR4ydBG1eu5jSvqzJSybAvxh7v2GBs");
    public static final Account account8 = Account.fromWIF("KzTvj76TPsaxww9zW5xWncnPrSnBNkbuKvYS7j7TxnG1JMS141oB");
    public static final Account account9 = Account.fromWIF("L5W8RKdjDWXeBuyTpLNv5yquGphkS6m3YHSKGhBmXB1itz2Kxo3Q");

    // Hardhat signers 10-19
    public static final Hash160 recipient0 = new Hash160("0xBcd4042DE499D14e55001CcbB24a551F3b954096");
    public static final Hash160 recipient1 = new Hash160("0x71bE63f3384f5fb98995898A86B02Fb2426c5788");
    public static final Hash160 recipient2 = new Hash160("0xFABB0ac9d68B0B445fB7357272Ff202C5651694a");
    public static final Hash160 recipient3 = new Hash160("0x1CBd3b2770909D4e10f157cABC84C7264073C9Ec");
    public static final Hash160 recipient4 = new Hash160("0xdF3e18d64BC6A983f673Ab319CCaE4f1a57C7097");
    public static final Hash160 recipient5 = new Hash160("0xcd3B766CCDd6AE721141F452C550Ca635964ce71");
    public static final Hash160 recipient6 = new Hash160("0x2546BcD3c84621e976D8185a91A922aE77ECEc30");
    public static final Hash160 recipient7 = new Hash160("0xbDA5747bFD65F08deb54cb465eB87D40e51B197E");
    public static final Hash160 recipient8 = new Hash160("0xdD2FD4581271e230360230F9337D5c0430Bf44C0");
    public static final Hash160 recipient9 = new Hash160("0x8626f6940E2eb28930eFb4CeF49B2d1F2C9C1199");

    public static ContractParameter prepareManagementDeployParameter(
            ECPublicKey owner,
            ECPublicKey relayer,
            List<ECPublicKey> validators,
            Integer threshold,
            ECPublicKey governor,
            ECPublicKey securityGuard
    ) {
        return array(
                publicKey(owner),
                publicKey(relayer),
                array(
                        publicKey(validators.get(0)),
                        publicKey(validators.get(1)),
                        publicKey(validators.get(2)),
                        publicKey(validators.get(3)),
                        publicKey(validators.get(4)),
                        publicKey(validators.get(5)),
                        publicKey(validators.get(6))
                ),
                integer(threshold),
                publicKey(governor),
                publicKey(securityGuard)
        );
    }

    public static void setDefaultValidators(Management management, Neow3j neow3j) throws Throwable {
        NeoSendRawTransaction response =
                management.invokeFunction("setValidators", array(defaultValidators), integer(5))
                        .signers(calledByEntry(owner))
                        .sign()
                        .send();
        waitUntilTransactionIsExecuted(response, neow3j);
    }

    public static Hash256 setDepositFee(Bridge bridge, Neow3j neow3j, BigInteger fee) throws Throwable {
        Transaction transaction =
                bridge.invokeFunction("setGasDepositFee", integer(fee))
                        .signers(calledByEntry(governor))
                        .sign();
        Hash256 txHash = transaction.getTxId();
        NeoSendRawTransaction response = transaction.send();
        waitUntilTransactionIsExecuted(response, neow3j);
        return txHash;
    }

    public static Hash256 setMinDeposit(Bridge bridge, Neow3j neow3j, BigInteger minDeposit) throws Throwable {
        Transaction transaction =
                bridge.invokeFunction("setMinGasDeposit", integer(minDeposit))
                        .signers(calledByEntry(governor))
                        .sign();
        Hash256 txHash = transaction.getTxId();
        NeoSendRawTransaction response = transaction.send();
        waitUntilTransactionIsExecuted(response, neow3j);
        return txHash;
    }

    public static Hash256 setMaxGasDeposit(Bridge bridge, Neow3j neow3j, BigInteger maxDeposit) throws Throwable {
        Transaction transaction =
                bridge.invokeFunction("setMaxGasDeposit", integer(maxDeposit))
                        .signers(calledByEntry(governor))
                        .sign();
        Hash256 txHash = transaction.getTxId();
        NeoSendRawTransaction response = transaction.send();
        waitUntilTransactionIsExecuted(response, neow3j);
        return txHash;
    }

    public static void waitUntilTransactionIsExecuted(NeoSendRawTransaction response, Neow3j neow3j) {
        Await.waitUntilTransactionIsExecuted(response.getSendRawTransaction().getHash(), neow3j);
    }

    public static boolean hasFiredEvent(Neow3j neow3j, Hash256 txHash, Hash160 contract, String eventName,
            StackItem state) throws IOException {
        return getEvents(txHash, neow3j).stream()
                .filter(e -> e.getEventName().equals(eventName))
                .filter(e -> e.getContract().equals(contract))
                .anyMatch(e -> e.getState().equals(state));
    }

    // region concat and sha256 functions

    public static byte[] concatLeftRight(String leftHex, String rightHex) {
        return concatenate(hexStringToByteArray(leftHex), hexStringToByteArray(rightHex));
    }

    public static String sha256Hex(byte[] input) {
        return toHexString(Hash.sha256(input));
    }

    public static String sha256HexNoPrefix(byte[] input) {
        return cleanHexPrefix(sha256Hex(input));
    }

    public static String concatAndSha256(String leftHex, String rightHex) {
        return sha256Hex(concatLeftRight(leftHex, rightHex));
    }

    public static Map<ContractParameter, ContractParameter> signMsg(List<Account> validators, String root) {
        String msg = sha256Hex(hexStringToByteArray(root));
        Map<ContractParameter, ContractParameter> signatures = new HashMap<>();
        for (int i = 0; i < validators.size(); i++) {
            ECKeyPair validator = validators.get(i).getECKeyPair();
            signatures.put(publicKey(validator.getPublicKey()), signature(Sign.signHexMessage(msg, validator)));
        }
        return signatures;
    }

    public static String createDepositHash(BigInteger nonce, Hash160 to, BigInteger amount) {
        return sha256Hex(concatDepositData(nonce, to, amount));
    }

    public static byte[] concatDepositData(BigInteger nonce, Hash160 recipient, BigInteger amount) {
        byte[] noncePadded = BigIntegers.toLittleEndianByteArrayZeroPadded(nonce, UINT256_SIZE);
        byte[] recipientArray = ArrayUtils.reverseArray(recipient.toArray());
        byte[] amountPadded = BigIntegers.toLittleEndianByteArrayZeroPadded(amount, UINT256_SIZE);
        byte[] concatenated = concatenate(concatenate(amountPadded, recipientArray), noncePadded);
        concatenated = ArrayUtils.reverseArray(concatenated);
        return concatenated;
    }

    public static String createDepositHashNoPrefix(BigInteger nonce, Hash160 to, BigInteger amount) {
        return cleanHexPrefix(createDepositHash(nonce, to, amount));
    }

    private static byte[] padToBytes(byte[] data, int padToSize) {
        int dataSize = data.length;
        int toPad = padToSize - dataSize;
        assert toPad >= 0 : "Data is too long.";
        byte[] padding = new byte[toPad];
        return concat(data, padding);
    }

    // big-endian modification of io.neow3j.utils.BigIntegers.toLittleEndianByteArrayZeroPadded()
    public static byte[] toBigEndianByteArrayZeroPadded(BigInteger value, int length) {
        // BigInteger.toByteArray() returns the two's complement of the number in big-endian order.
        byte[] bytes = value.toByteArray();
        if (bytes.length > length) {
            throw new IllegalArgumentException(format("given integer needs more space (%s bytes) than the given " +
                    "minimum length (%s bytes).", bytes.length, length));
        }
        if (bytes.length < length) {
            byte[] temp = new byte[length];
            System.arraycopy(bytes, 0, temp, length - bytes.length, bytes.length);
            return temp;
        }
        return bytes;
    }

    // endregion

    private static List<Notification> getEvents(Hash256 txHash, Neow3j neow3j) throws IOException {
        return neow3j.getApplicationLog(txHash).send().getApplicationLog().getFirstExecution().getNotifications();
    }

    public static List<DepositEvent> getDepositEvents(Hash256 txHash, Neow3j neow3j, Hash160 bridge) throws IOException {
        // GasToken Transfer is first notification, OnDeposit is second notification.
        return neow3j.getApplicationLog(txHash).send().getApplicationLog()
                .getFirstExecution().getNotifications().stream()
                .filter(n -> n.getContract().equals(bridge) && n.getEventName().equals("GasDeposit"))
                .map(TestHelper::depositEventFromNotification)
                .collect(Collectors.toList());
    }

    public static List<WithdrawEvent> getWithdrawEvents(Hash256 txHash, Neow3j neow3j, Hash160 bridge) throws IOException {
        // GasToken Transfer is first notification, onWithdrawal is second notification.
        return neow3j.getApplicationLog(txHash).send().getApplicationLog()
                .getFirstExecution().getNotifications().stream()
                .filter(n -> n.getContract().equals(bridge) && n.getEventName().equals("GasWithdrawal"))
                .map(TestHelper::getWithdrawEventFromNotification)
                .collect(Collectors.toList());
    }

    public static List<ClaimableEvent> getClaimableEvents(Hash256 txHash, Neow3j neow3j, Hash160 bridge) throws IOException {
        // GasToken Transfer is first notification, onClaimable is second notification.
        return neow3j.getApplicationLog(txHash).send().getApplicationLog()
                .getFirstExecution().getNotifications().stream()
                .filter(n -> n.getContract().equals(bridge) && n.getEventName().equals("GasClaimable"))
                .map(TestHelper::claimableEventFromNotification)
                .collect(Collectors.toList());
    }

    public static List<ClaimEvent> getClaimEvents(Hash256 txHash, Neow3j neow3j, Hash160 bridge) throws IOException {
        // GasToken Transfer is first notification, onClaimable is second notification.
        return neow3j.getApplicationLog(txHash).send().getApplicationLog()
                .getFirstExecution().getNotifications().stream()
                .filter(n -> n.getContract().equals(bridge) && n.getEventName().equals("GasClaim"))
                .map(TestHelper::claimEventFromNotification)
                .collect(Collectors.toList());
    }

    public static DepositEvent depositEventFromNotification(Notification depositEvent) {
        List<StackItem> state = depositEvent.getState().getList();
        BigInteger nonce = state.get(0).getInteger();
        Hash160 to = Hash160.fromAddress(state.get(1).getAddress());
        BigInteger amount = state.get(2).getInteger();
        Hash160 from = Hash160.fromAddress(state.get(3).getAddress());
        if (state.size() > 4) {
            String depositHash = prependHexPrefix(state.get(4).getHexString());
            String rootHash = prependHexPrefix(state.get(5).getHexString());
            return new DepositEvent(nonce, to, amount, from, depositHash, rootHash);
        }
        return new DepositEvent(nonce, to, amount, from);
    }

    private static WithdrawEvent getWithdrawEventFromNotification(Notification depositEvent) {
        List<StackItem> state = depositEvent.getState().getList();
        BigInteger nonce = state.get(0).getInteger();
        Hash160 to = Hash160.fromAddress(state.get(1).getAddress());
        BigInteger amount = state.get(2).getInteger();
        return new WithdrawEvent(nonce, to, amount);
    }

    private static ClaimableEvent claimableEventFromNotification(Notification claimableEvent) {
        List<StackItem> state = claimableEvent.getState().getList();
        BigInteger nonce = state.get(0).getInteger();
        Hash160 to = Hash160.fromAddress(state.get(1).getAddress());
        BigInteger amount = state.get(2).getInteger();
        return new ClaimableEvent(nonce, to, amount);
    }

    private static ClaimEvent claimEventFromNotification(Notification claimEvent) {
        List<StackItem> state = claimEvent.getState().getList();
        BigInteger nonce = state.get(0).getInteger();
        Hash160 to = Hash160.fromAddress(state.get(1).getAddress());
        BigInteger amount = state.get(2).getInteger();
        return new ClaimEvent(nonce, to, amount);
    }

    public static class DepositEvent {
        public BigInteger nonce;
        public Hash160 to;
        public BigInteger amount;
        public Hash160 from;
        public String depositHashHex;
        public String rootHashHex;

        public DepositEvent(BigInteger nonce, Hash160 to, BigInteger amount, Hash160 from, String depositHashHex,
                String rootHashHex) {
            this.nonce = nonce;
            this.to = to;
            this.amount = amount;
            this.from = from;
            this.depositHashHex = depositHashHex;
            this.rootHashHex = rootHashHex;
        }

        public DepositEvent(BigInteger nonce, Hash160 to, BigInteger amount, Hash160 from) {
            this.nonce = nonce;
            this.to = to;
            this.amount = amount;
            this.from = from;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DepositEvent)) return false;
            DepositEvent that = (DepositEvent) o;
            return nonce.equals(that.nonce) &&
                    to.equals(that.to) &&
                    amount.equals(that.amount) &&
                    from.equals(that.from) &&
                    depositHashHex.equals(that.depositHashHex) &&
                    rootHashHex.equals(that.rootHashHex);
        }

        @Override
        public String toString() {
            return "\nDepositEvent\n" +
                    "\n  nonce=" + nonce +
                    "\n  from=" + from +
                    "\n  to=" + to +
                    "\n  amount=" + amount +
                    "\n  depositHash=" + depositHashHex +
                    "\n  rootHash=" + rootHashHex +
                    "\n";
        }
    }

    public static class WithdrawEvent {
        public BigInteger nonce;
        public Hash160 to;
        public BigInteger amount;

        public WithdrawEvent(BigInteger nonce, Hash160 to, BigInteger amount) {
            this.nonce = nonce;
            this.to = to;
            this.amount = amount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof WithdrawEvent)) return false;
            WithdrawEvent that = (WithdrawEvent) o;
            return nonce.equals(that.nonce) &&
                    to.equals(that.to) &&
                    amount.equals(that.amount);
        }

        @Override
        public String toString() {
            return "\nWithdrawEvent\n" +
                    "\n  nonce=" + nonce +
                    "\n  to=" + to +
                    "\n  amount=" + amount +
                    "\n";
        }
    }

    public static class TransferEvent {
        public Hash160 from;
        public Hash160 to;
        public BigInteger amount;

        public TransferEvent(Hash160 from, Hash160 to, BigInteger amount) {
            this.from = from;
            this.to = to;
            this.amount = amount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TransferEvent)) return false;
            TransferEvent that = (TransferEvent) o;
            return from.equals(that.from) &&
                    to.equals(that.to) &&
                    amount.equals(that.amount);
        }

        @Override
        public String toString() {
            return "\nTransferEvent\n" +
                    "\n  from=" + from +
                    "\n  to=" + to +
                    "\n  amount=" + amount +
                    "\n";
        }
    }

    public static class ClaimableEvent {
        public BigInteger nonce;
        public Hash160 to;
        public BigInteger amount;

        public ClaimableEvent(BigInteger nonce, Hash160 to, BigInteger amount) {
            this.nonce = nonce;
            this.to = to;
            this.amount = amount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ClaimableEvent)) return false;
            ClaimableEvent that = (ClaimableEvent) o;
            return nonce.equals(that.nonce) &&
                    to.equals(that.to) &&
                    amount.equals(that.amount);
        }

        @Override
        public String toString() {
            return "\nClaimableEvent\n" +
                    "\n  nonce=" + nonce +
                    "\n  to=" + to +
                    "\n  amount=" + amount +
                    "\n";
        }
    }

    public static class ClaimEvent {
        public BigInteger nonce;
        public Hash160 to;
        public BigInteger amount;

        public ClaimEvent(BigInteger nonce, Hash160 to, BigInteger amount) {
            this.nonce = nonce;
            this.to = to;
            this.amount = amount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ClaimEvent)) return false;
            ClaimEvent that = (ClaimEvent) o;
            return nonce.equals(that.nonce) &&
                    to.equals(that.to) &&
                    amount.equals(that.amount);
        }

        @Override
        public String toString() {
            return "\nClaimEvent\n" +
                    "\n  nonce=" + nonce +
                    "\n  to=" + to +
                    "\n  amount=" + amount +
                    "\n";
        }
    }
}
