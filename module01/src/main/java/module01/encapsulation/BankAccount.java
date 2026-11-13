package module01.encapsulation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulation: balance can only change through operations that preserve invariants
 * (never negative, every change recorded). No setters, no exposed mutable collections.
 */
public final class BankAccount {
    private final String accountId;
    private BigDecimal balance;
    private final List<String> history = new ArrayList<>();

    public BankAccount(String accountId, BigDecimal openingBalance) {
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("accountId must not be blank");
        }
        if (openingBalance == null || openingBalance.signum() < 0) {
            throw new IllegalArgumentException("opening balance must be >= 0");
        }
        this.accountId = accountId;
        this.balance = openingBalance;
        history.add("OPEN " + openingBalance);
    }

    public synchronized void deposit(BigDecimal amount) {
        requirePositive(amount);
        balance = balance.add(amount);
        history.add("DEPOSIT " + amount);
    }

    public synchronized void withdraw(BigDecimal amount) {
        requirePositive(amount);
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(accountId, balance, amount);
        }
        balance = balance.subtract(amount);
        history.add("WITHDRAW " + amount);
    }

    public synchronized BigDecimal balance() {
        return balance;
    }

    public String accountId() {
        return accountId;
    }

    /** Defensive copy: callers cannot rewrite history. */
    public synchronized List<String> history() {
        return Collections.unmodifiableList(new ArrayList<>(history));
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
