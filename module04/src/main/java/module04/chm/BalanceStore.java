package module04.chm;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ConcurrentHashMap atomic operations: compute, merge, putIfAbsent, replace(k, old, new).
 * The rule: one CHM call = one atomic step. get-then-put is NOT atomic.
 */
public final class BalanceStore {
    private final ConcurrentHashMap<String, BigDecimal> balances = new ConcurrentHashMap<>();

    public void open(String accountId, BigDecimal opening) {
        if (balances.putIfAbsent(accountId, opening) != null) {
            throw new IllegalStateException("account exists: " + accountId);
        }
    }

    /** Atomic credit via merge. */
    public BigDecimal credit(String accountId, BigDecimal amount) {
        requirePositive(amount);
        return balances.merge(accountId, amount, BigDecimal::add);
    }

    /** Atomic debit with invariant check via computeIfPresent: throwing aborts the update. */
    public BigDecimal debit(String accountId, BigDecimal amount) {
        requirePositive(amount);
        BigDecimal result = balances.computeIfPresent(accountId, (id, bal) -> {
            if (bal.compareTo(amount) < 0) {
                throw new InsufficientFundsException(id, bal, amount);
            }
            return bal.subtract(amount);
        });
        if (result == null) {
            throw new IllegalArgumentException("unknown account " + accountId);
        }
        return result;
    }

    /** Optimistic update using replace(key, expected, new) - a CAS on the value. */
    public boolean debitOptimistic(String accountId, BigDecimal amount) {
        requirePositive(amount);
        while (true) {
            BigDecimal current = balances.get(accountId);
            if (current == null) throw new IllegalArgumentException("unknown account " + accountId);
            if (current.compareTo(amount) < 0) return false;
            if (balances.replace(accountId, current, current.subtract(amount))) {
                return true;
            }
            // lost the race, retry
        }
    }

    public Optional<BigDecimal> balance(String accountId) {
        return Optional.ofNullable(balances.get(accountId));
    }

    /** Weakly consistent snapshot: fine for monitoring, not for accounting. */
    public BigDecimal totalApprox() {
        return balances.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<String, BigDecimal> snapshot() {
        return Map.copyOf(balances);
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("amount must be positive");
    }

    public static final class InsufficientFundsException extends RuntimeException {
        public InsufficientFundsException(String id, BigDecimal balance, BigDecimal requested) {
            super(id + ": balance " + balance + " < " + requested);
        }
    }
}
