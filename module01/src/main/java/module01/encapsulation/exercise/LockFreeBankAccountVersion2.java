package module01.encapsulation.exercise;

import module01.encapsulation.InsufficientFundsException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/*
  This version decouples the balance (AtomicReference) from the transaction log (ConcurrentLinkedQueue).
  It offers high write throughput and low memory overhead since history entries are appended asynchronously
  right after the balance CAS succeeds.
  الإصدار الثاني (Version 2):
   يفصل الرصيد عن السجل، حيث يستعمل AtomicReference للرصيد و ConcurrentLinkedQueue آمنة للثريدات لتسجيل العمليات بكفاءة عالية وسرعة كتابة.
 */
public final class LockFreeBankAccountVersion2 {

    private final String accountId;
    private final AtomicReference<BigDecimal> balanceRef;

    // ConcurrentLinkedQueue is an Non-blocking, unbounded, thread-safe, lock-free queue
    // based on linked nodes, optimized for concurrent multi-producer appends.

    //We didn't choose CopyOnWriteArrayList because every mutative operation (add, remove, set) creates
    // a fresh copy of the underlying array under a lock. Reads are completely lock-free and fast.
    private final Queue<String> history = new ConcurrentLinkedQueue<>();

    public LockFreeBankAccountVersion2(String accountId, BigDecimal openingBalance) {
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
            BigDecimal currentBalance = balanceRef.get();
            BigDecimal newBalance = currentBalance.add(amount);

            // Step 1: Secure atomic consensus on the balance update first.
            if (balanceRef.compareAndSet(currentBalance, newBalance)) {
                // Step 2: Once balance is safely committed, log the operation.
                // Note: History is "eventually consistent" with balance by a tiny window.
                history.add("DEPOSIT " + amount);
                return;
            }
        }
    }

    public void withdraw(BigDecimal amount) {
        requirePositive(amount);

        while (true) {
            BigDecimal currentBalance = balanceRef.get();

            // Check funds against the latest observed balance inside the retry loop
            if (currentBalance.compareTo(amount) < 0) {
                throw new InsufficientFundsException(accountId, currentBalance, amount);
            }

            BigDecimal newBalance = currentBalance.subtract(amount);

            if (balanceRef.compareAndSet(currentBalance, newBalance)) {
                history.add("WITHDRAW " + amount);
                return;
            }
        }
    }

    public BigDecimal balance() {
        return balanceRef.get();
    }

    public String accountId() {
        return accountId;
    }

    public List<String> history() {
        // Defensive copy: create an ArrayList snapshot from the concurrent queue
        // and wrap it in an unmodifiable wrapper so external callers cannot mutate it.
        return Collections.unmodifiableList(new ArrayList<>(history));
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}