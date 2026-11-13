package module15.p03transfer;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * P03 - Transfer between accounts: atomic, no negative balances, no deadlock (lock ordering by id), money conserved.
 */
public final class P03MoneyTransfer {
    public enum Result { OK, INSUFFICIENT_FUNDS, UNKNOWN_ACCOUNT, SAME_ACCOUNT, INVALID_AMOUNT }

    private static final class Account {
        final ReentrantLock lock = new ReentrantLock();
        BigDecimal balance;

        Account(BigDecimal balance) {
            this.balance = balance;
        }
    }

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    public void open(String id, BigDecimal opening) {
        Objects.requireNonNull(id);
        if (opening.signum() < 0) throw new IllegalArgumentException("opening must be >= 0");
        if (accounts.putIfAbsent(id, new Account(opening)) != null) throw new IllegalStateException("exists: " + id);
    }

    public Result transfer(String fromId, String toId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) return Result.INVALID_AMOUNT;
        if (fromId.equals(toId)) return Result.SAME_ACCOUNT;
        Account from = accounts.get(fromId), to = accounts.get(toId);
        if (from == null || to == null) return Result.UNKNOWN_ACCOUNT;

        Account first = fromId.compareTo(toId) < 0 ? from : to;
        Account second = first == from ? to : from;
        first.lock.lock();
        try {
            second.lock.lock();
            try {
                if (from.balance.compareTo(amount) < 0) return Result.INSUFFICIENT_FUNDS;
                from.balance = from.balance.subtract(amount);
                to.balance = to.balance.add(amount);
                return Result.OK;
            } finally {
                second.lock.unlock();
            }
        } finally {
            first.lock.unlock();
        }
    }

    public BigDecimal balance(String id) {
        Account a = accounts.get(id);
        if (a == null) throw new IllegalArgumentException("unknown " + id);
        a.lock.lock();
        try {
            return a.balance;
        } finally {
            a.lock.unlock();
        }
    }

    public BigDecimal totalMoney() {
        BigDecimal t = BigDecimal.ZERO;
        for (String id : accounts.keySet()) t = t.add(balance(id));
        return t;
    }
}
