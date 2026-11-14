package module01.encapsulation.exercise;


import module01.encapsulation.InsufficientFundsException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/*
* This version encapsulates both the balance and history into a single immutable record (State).
* It guarantees transactional consistency:
* - the balance and the history log are always updated together as a single atomic unit.
*
* الإصدار الأول (Version 1):
*  يدمج الرصيد والسجل معا داخل كجل Record غير قابل للتعديل (Immutable) ويقوم بتحديثهما ككتلة واحدة عبر AtomicReference و CAS Loop لضمان التطابق التام.
* */
public final class LockFreeBankAccountVersion1 {

    /**
     * Immutable state container holding both balance and history.
     * Encapsulating both prevents race conditions where balance updates
     * outpace or desynchronize from transaction logs.
     */
    private record State(BigDecimal balance, List<String> history) {
        public State(BigDecimal balance, List<String> history) {
            this.balance = balance;
            // Defensive copy using List.copyOf ensures the inner list
            // cannot be mutated externally after state creation.
            this.history = List.copyOf(history);
        }
    }

    private final String accountId;
    private final AtomicReference<State> stateRef;

    public LockFreeBankAccountVersion1(String accountId, BigDecimal openingBalance) {
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("accountId must not be blank");
        }
        if (openingBalance == null || openingBalance.signum() < 0) {
            throw new IllegalArgumentException("opening balance must be >= 0");
        }
        this.accountId = accountId;
        List<String> initialHistory = List.of("OPEN " + openingBalance);
        this.stateRef = new AtomicReference<>(new State(openingBalance, initialHistory));
    }

    public void deposit(BigDecimal amount) {
        requirePositive(amount);

        // CAS (Compare-And-Set) Retry Loop:
        // Because multiple threads might modify the state concurrently,
        // we read the latest state, compute the new state, and try to swap it atomically.
        // If another thread modified 'stateRef' in the meantime, CAS fails and retries.
        while (true) {
            State current = stateRef.get();
            BigDecimal newBalance = current.balance.add(amount);

            // Create a new modifiable list from current history and append the entry
            List<String> newHistory = new ArrayList<>(current.history);
            newHistory.add("DEPOSIT " + amount);

            // Attempt atomic installation of the new state snapshot
            if (stateRef.compareAndSet(current, new State(newBalance, newHistory))) {
                return;
            }
        }
    }

    public void withdraw(BigDecimal amount) {
        requirePositive(amount);

        while (true) {
            State current = stateRef.get();

            // Validation must happen inside the loop because balance can change
            // between iterations due to concurrent thread activity.
            if (current.balance.compareTo(amount) < 0) {
                throw new InsufficientFundsException(accountId, current.balance, amount);
            }

            BigDecimal newBalance = current.balance.subtract(amount);
            List<String> newHistory = new ArrayList<>(current.history);
            newHistory.add("WITHDRAW " + amount);

            if (stateRef.compareAndSet(current, new State(newBalance, newHistory))) {
                return;
            }
        }
    }

    public BigDecimal balance() {
        return stateRef.get().balance;
    }

    public String accountId() {
        return accountId;
    }

    public List<String> history() {
        return stateRef.get().history;
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}