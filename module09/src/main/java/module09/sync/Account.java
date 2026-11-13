package module09.sync;

import java.math.BigDecimal;

/**
 * synchronized on a private lock object (not `this`) so external code cannot interfere,
 * and `transfer` locks both accounts in a global order to avoid deadlock (see module11).
 */
public final class Account {
    private final String id;
    private final Object lock = new Object();
    private BigDecimal balance;

    public Account(String id, BigDecimal balance) {
        this.id = id;
        this.balance = balance;
    }

    public String id() {
        return id;
    }

    public BigDecimal balance() {
        synchronized (lock) {
            return balance;
        }
    }

    public void deposit(BigDecimal amount) {
        requirePositive(amount);
        synchronized (lock) {
            balance = balance.add(amount);
        }
    }

    public boolean withdraw(BigDecimal amount) {
        requirePositive(amount);
        synchronized (lock) {
            if (balance.compareTo(amount) < 0) return false;
            balance = balance.subtract(amount);
            return true;
        }
    }

    /** Lock ordering by id makes concurrent A->B and B->A transfers deadlock-free. */
    public static boolean transfer(Account from, Account to, BigDecimal amount) {
        requirePositive(amount);
        if (from == to) throw new IllegalArgumentException("same account");
        Account first = from.id.compareTo(to.id) < 0 ? from : to;
        Account second = first == from ? to : from;
        synchronized (first.lock) {
            synchronized (second.lock) {
                if (from.balance.compareTo(amount) < 0) return false;
                from.balance = from.balance.subtract(amount);
                to.balance = to.balance.add(amount);
                return true;
            }
        }
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("amount must be positive");
    }
}
