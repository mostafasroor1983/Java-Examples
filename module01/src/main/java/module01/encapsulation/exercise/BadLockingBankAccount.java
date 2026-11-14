package module01.encapsulation.exercise;

import module01.encapsulation.InsufficientFundsException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


public final class BadLockingBankAccount {
    private final String accountId;
    private AtomicReference<BigDecimal> balanceRef;

    /*
     * history is a standard ArrayList, which is not thread-safe. Multiple threads calling deposit() or withdraw()
     * concurrently will mutate the list structure simultaneously, leading to race conditions, lost updates,
     * corrupted internal arrays, or a ConcurrentModificationException
     * */
    private final List<String> history = new ArrayList<>();

    public BadLockingBankAccount(String accountId, BigDecimal openingBalance) {
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


    /*
    * No Retry Loop: CAS operations require a retry loop (e.g., a while loop) because if another thread updates the reference
    * concurrently, the compareAndSet fails. Here, if it fails, the method simply moves on without updating the balance.
      Silent Failure & Desync: If the CAS fails, the balance is not updated, but history.add(...) still executes anyway.
      This puts the bank account state completely out of sync with its transaction history.*/
    public void deposit(BigDecimal amount) {
        requirePositive(amount);
        BigDecimal balance = balanceRef.get();
        BigDecimal newBalance = balanceRef.get().add(amount);
        balanceRef.compareAndSet(balance, newBalance);
        history.add("DEPOSIT " + amount);
    }

    public void withdraw(BigDecimal amount) {
        requirePositive(amount);
        if (balanceRef.get().compareTo(amount) < 0) {
            throw new InsufficientFundsException(accountId, balance(), amount);
        }

        BigDecimal balance = balanceRef.get();
        BigDecimal newBalance = balanceRef.get().subtract(amount);
        balanceRef.compareAndSet(balance, newBalance);

        history.add("WITHDRAW " + amount);
    }

    public synchronized BigDecimal balance() {
        return balanceRef.get();
    }

    public String accountId() {
        return accountId;
    }

    /**
     * Defensive copy: callers cannot rewrite history.
     */
    public synchronized List<String> history() {
        return Collections.unmodifiableList(new ArrayList<>(history));
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
