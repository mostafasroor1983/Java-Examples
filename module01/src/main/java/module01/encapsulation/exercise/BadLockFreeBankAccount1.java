package module01.encapsulation.exercise;

import module01.encapsulation.InsufficientFundsException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


public final class BadLockFreeBankAccount1 {
    private final String accountId;
    private AtomicReference<BigDecimal> balanceRef;
    private final List<String> history = new ArrayList<>();

    public BadLockFreeBankAccount1(String accountId, BigDecimal openingBalance) {
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("accountId must not be blank");
        }
        if (openingBalance == null || openingBalance.signum() < 0) {
            throw new IllegalArgumentException("opening balance must be >= 0");
        }
        this.accountId = accountId;
        this.balanceRef = new AtomicReference<>(openingBalance);
        history.add("OPEN " + openingBalance);
    }


    public void deposit(BigDecimal amount) {
        requirePositive(amount);

        while (true) {
            BigDecimal balance = balance();
            BigDecimal newBalance = balance.add(amount);
            if (balanceRef.compareAndSet(balance, newBalance)) {
                history.add("DEPOSIT " + amount);
                break;
            }
        }
    }

    public void withdraw(BigDecimal amount) {
        requirePositive(amount);
        BigDecimal balance = balance();

        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(accountId, balance, amount);
        }

        while (true) {
            balance = balance();
            BigDecimal newBalance = balance.subtract(amount);
            if (balanceRef.compareAndSet(balance, newBalance)) {
                history.add("WITHDRAW " + amount);
                break;
            }
        }
    }

    public BigDecimal balance() {
        return balanceRef.get();
    }

    public String accountId() {
        return accountId;
    }

    /**
     * Defensive copy: callers cannot rewrite history.
     */
    public List<String> history() {
        return Collections.unmodifiableList(new ArrayList<>(history));
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
