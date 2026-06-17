package network.bane.support.event;

import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.Notification;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.neow3j.utils.Numeric.prependHexPrefix;

public class TokenBridgeEventHelper {

    public static List<DepositEvent> getDepositEvents(Hash256 txHash, Neow3j neow3j, Hash160 bridge)
            throws IOException {
        // GasToken Transfer is first notification, OnDeposit is second notification.
        return neow3j.getApplicationLog(txHash).send().getApplicationLog().getFirstExecution().getNotifications()
                .stream().filter(n -> n.getContract().equals(bridge) && n.getEventName().equals("NativeDeposit"))
                .map(TokenBridgeEventHelper::depositEventFromNotification).collect(Collectors.toList());
    }

    public static List<WithdrawEvent> getWithdrawEvents(Hash256 txHash, Neow3j neow3j, Hash160 bridge)
            throws IOException {
        // GasToken Transfer is first notification, onWithdrawal is second notification.
        return neow3j.getApplicationLog(txHash).send().getApplicationLog().getFirstExecution().getNotifications()
                .stream().filter(n -> n.getContract().equals(bridge) && n.getEventName().equals("NativeWithdrawal"))
                .map(TokenBridgeEventHelper::getWithdrawEventFromNotification).collect(Collectors.toList());
    }

    public static List<ClaimableEvent> getClaimableEvents(Hash256 txHash, Neow3j neow3j, Hash160 bridge)
            throws IOException {
        // GasToken Transfer is first notification, onClaimable is second notification.
        return neow3j.getApplicationLog(txHash).send().getApplicationLog().getFirstExecution().getNotifications()
                .stream().filter(n -> n.getContract().equals(bridge) && n.getEventName().equals("NativeClaimable"))
                .map(TokenBridgeEventHelper::claimableEventFromNotification).collect(Collectors.toList());
    }

    public static List<ClaimEvent> getClaimEvents(Hash256 txHash, Neow3j neow3j, Hash160 bridge) throws IOException {
        // GasToken Transfer is first notification, onClaimable is second notification.
        return neow3j.getApplicationLog(txHash).send().getApplicationLog().getFirstExecution().getNotifications()
                .stream().filter(n -> n.getContract().equals(bridge) && n.getEventName().equals("NativeClaim"))
                .map(TokenBridgeEventHelper::claimEventFromNotification).collect(Collectors.toList());
    }

    public static DepositEvent depositEventFromNotification(Notification depositEvent) {
        List<StackItem> state = depositEvent.getState().getList();
        BigInteger nonce = state.get(0).getInteger();
        Hash160 to = Hash160.fromAddress(state.get(1).getAddress());
        BigInteger amount = state.get(2).getInteger();
        Hash160 from = Hash160.fromAddress(state.get(3).getAddress());
        if (state.size() >= 6) {
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
            return nonce.equals(that.nonce) && to.equals(that.to) && amount.equals(that.amount) &&
                    from.equals(that.from) && Objects.equals(depositHashHex, that.depositHashHex) &&
                    Objects.equals(rootHashHex, that.rootHashHex);
        }

        @Override
        public String toString() {
            return "\nDepositEvent\n" + "\n  nonce=" + nonce + "\n  from=" + from + "\n  to=" + to + "\n  amount=" +
                    amount + "\n  depositHash=" + depositHashHex + "\n  rootHash=" + rootHashHex + "\n";
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
            return nonce.equals(that.nonce) && to.equals(that.to) && amount.equals(that.amount);
        }

        @Override
        public String toString() {
            return "\nWithdrawEvent\n" + "\n  nonce=" + nonce + "\n  to=" + to + "\n  amount=" + amount + "\n";
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
            return from.equals(that.from) && to.equals(that.to) && amount.equals(that.amount);
        }

        @Override
        public String toString() {
            return "\nTransferEvent\n" + "\n  from=" + from + "\n  to=" + to + "\n  amount=" + amount + "\n";
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
            return nonce.equals(that.nonce) && to.equals(that.to) && amount.equals(that.amount);
        }

        @Override
        public String toString() {
            return "\nClaimableEvent\n" + "\n  nonce=" + nonce + "\n  to=" + to + "\n  amount=" + amount + "\n";
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
            return nonce.equals(that.nonce) && to.equals(that.to) && amount.equals(that.amount);
        }

        @Override
        public String toString() {
            return "\nClaimEvent\n" + "\n  nonce=" + nonce + "\n  to=" + to + "\n  amount=" + amount + "\n";
        }
    }

}
