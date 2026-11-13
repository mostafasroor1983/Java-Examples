package module15.p02balance;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * P02 - Account balance with deposits, withdrawals, overdraft limit and an immutable statement.
 * Interview angle: BigDecimal, invariants, defensive copies, lock scope.
 */
public final class P02AccountBalance {
    public record Entry(String type, BigDecimal amount, BigDecimal balanceAfter) {
    }

    private final Object lock = new Object();
    private final BigDecimal overdraftLimit; // >= 0, how far below zero we may go
    private BigDecimal balance = BigDecimal.ZERO;
    private final List<Entry> statement = new ArrayList<>();

    public P02AccountBalance(BigDecimal overdraftLimit) {
        Objects.requireNonNull(overdraftLimit);
        if (overdraftLimit.signum() < 0) throw new IllegalArgumentException("overdraft limit must be >= 0");
        this.overdraftLimit = overdraftLimit;
    }

    public void deposit(BigDecimal amount) {
        requirePositive(amount);
        synchronized (lock) {
            balance = balance.add(amount);
            statement.add(new Entry("DEPOSIT", amount, balance));
        }
    }

    public boolean withdraw(BigDecimal amount) {
        requirePositive(amount);
        synchronized (lock) {
            if (balance.subtract(amount).compareTo(overdraftLimit.negate()) < 0) return false;
            balance = balance.subtract(amount);
            statement.add(new Entry("WITHDRAW", amount, balance));
            return true;
        }
    }

    public BigDecimal balance() {
        synchronized (lock) {
            return balance;
        }
    }

    public List<Entry> statement() {
        synchronized (lock) {
            return List.copyOf(statement);
        }
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("amount must be positive");
    }
}
