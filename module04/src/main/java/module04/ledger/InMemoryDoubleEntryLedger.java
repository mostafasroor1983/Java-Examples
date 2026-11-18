package module04.ledger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Double-entry ledger in memory: every transfer writes a DEBIT entry on one account and a CREDIT entry on another,
 * so the sum of all balances is constant (zero-sum against an external "world" account). This is the invariant
 * auditors check, and the one a concurrency bug breaks first.
 *
 * <p>Locking design:
 * <ul>
 *   <li><b>Per-account {@link ReentrantLock}s</b>, always acquired in account-id order: transfers on disjoint accounts
 *   run in parallel; A->B and B->A cannot deadlock.</li>
 *   <li><b>A {@link ReentrantReadWriteLock} "snapshot gate"</b>: every transfer holds the READ side (many at once);
 *   {@link #snapshot()} takes the WRITE side, so it sees a state where no transfer is half-applied.
 *   Iterating a ConcurrentHashMap alone is only weakly consistent and could observe the debit without the credit.</li>
 *   <li><b>Idempotency</b>: the transfer reference is claimed with {@code putIfAbsent} on a CHM, then released if
 *   the transfer fails a business rule, so the client can retry after fixing the cause.</li>
 * </ul>
 *
 * <p>Scope: ONE JVM. Two service instances each holding this ledger would happily double-spend. Production ledgers
 * keep balances in a database (row locks / serialisable transactions / conditional updates) or behind a single
 * writer per account partition (e.g. Kafka partition per account).
 */
public final class InMemoryDoubleEntryLedger {
    public enum Side { DEBIT, CREDIT }

    public record Entry(String transferRef, String accountId, Side side, BigDecimal amount) {
    }

    public enum Result { POSTED, DUPLICATE, INSUFFICIENT_FUNDS, UNKNOWN_ACCOUNT }

    private static final class AccountState {
        final ReentrantLock lock = new ReentrantLock();
        BigDecimal balance;           // guarded by lock
        final List<Entry> entries = new ArrayList<>(); // guarded by lock

        AccountState(BigDecimal opening) {
            this.balance = opening;
        }
    }

    private final ConcurrentHashMap<String, AccountState> accounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> processedRefs = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock snapshotGate = new ReentrantReadWriteLock();

    public void open(String accountId, BigDecimal opening) {
        if (opening.signum() < 0) throw new IllegalArgumentException("opening balance must be >= 0");
        if (accounts.putIfAbsent(accountId, new AccountState(opening)) != null) {
            throw new IllegalStateException("account exists: " + accountId);
        }
    }

    public Result transfer(String transferRef, String from, String to, BigDecimal amount) {
        Objects.requireNonNull(transferRef, "transferRef");
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("amount must be positive");
        if (from.equals(to)) throw new IllegalArgumentException("from and to must differ");
        AccountState a = accounts.get(from), b = accounts.get(to);
        if (a == null || b == null) return Result.UNKNOWN_ACCOUNT;

        if (processedRefs.putIfAbsent(transferRef, Boolean.TRUE) != null) return Result.DUPLICATE; // atomic claim

        AccountState first = from.compareTo(to) < 0 ? a : b;  // global lock order => no deadlock
        AccountState second = first == a ? b : a;
        boolean posted = false;
        snapshotGate.readLock().lock();
        try {
            first.lock.lock();
            try {
                second.lock.lock();
                try {
                    if (a.balance.compareTo(amount) < 0) return Result.INSUFFICIENT_FUNDS;
                    a.balance = a.balance.subtract(amount);
                    b.balance = b.balance.add(amount);
                    a.entries.add(new Entry(transferRef, from, Side.DEBIT, amount));
                    b.entries.add(new Entry(transferRef, to, Side.CREDIT, amount));
                    posted = true;
                    return Result.POSTED;
                } finally {
                    second.lock.unlock();
                }
            } finally {
                first.lock.unlock();
            }
        } finally {
            snapshotGate.readLock().unlock();
            if (!posted) processedRefs.remove(transferRef); // business failure: the reference may be retried
        }
    }

    /** Consistent point-in-time view of all balances (blocks new transfers briefly). Sorted for stable reports. */
    public Map<String, BigDecimal> snapshot() {
        snapshotGate.writeLock().lock();
        try {
            Map<String, BigDecimal> out = new TreeMap<>();
            accounts.forEach((id, st) -> out.put(id, st.balance)); // safe: no transfer holds account locks now
            return Collections.unmodifiableMap(out);
        } finally {
            snapshotGate.writeLock().unlock();
        }
    }

    public List<Entry> entries(String accountId) {
        AccountState st = accounts.get(accountId);
        if (st == null) return List.of();
        st.lock.lock();
        try {
            return List.copyOf(st.entries);
        } finally {
            st.lock.unlock();
        }
    }

    /** Audit invariant: for every account, opening + credits - debits == balance. */
    public boolean entriesExplainBalance(String accountId, BigDecimal opening) {
        AccountState st = accounts.get(accountId);
        st.lock.lock();
        try {
            BigDecimal computed = opening;
            for (Entry e : st.entries) computed = e.side() == Side.CREDIT ? computed.add(e.amount()) : computed.subtract(e.amount());
            return computed.compareTo(st.balance) == 0;
        } finally {
            st.lock.unlock();
        }
    }
}
