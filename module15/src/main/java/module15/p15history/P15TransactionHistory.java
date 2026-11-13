package module15.p15history;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * P15 - Transaction history with time-range queries, "balance as of", and pagination.
 * TreeMap keyed by timestamp (ties resolved by sequence) gives O(log n + k) range queries.
 */
public final class P15TransactionHistory {
    public record Tx(long seq, Instant at, String accountId, BigDecimal amount, String description) {
    }

    private record Key(Instant at, long seq) implements Comparable<Key> {
        public int compareTo(Key o) {
            int c = at.compareTo(o.at);
            return c != 0 ? c : Long.compare(seq, o.seq);
        }
    }

    private final NavigableMap<Key, Tx> byTime = new TreeMap<>();
    private final ReadWriteLock rw = new ReentrantReadWriteLock();
    private long nextSeq;

    public Tx append(Instant at, String accountId, BigDecimal amount, String description) {
        Objects.requireNonNull(at);
        Objects.requireNonNull(accountId);
        if (amount == null || amount.signum() == 0) throw new IllegalArgumentException("amount must be non-zero");
        rw.writeLock().lock();
        try {
            Tx tx = new Tx(nextSeq++, at, accountId, amount, description == null ? "" : description);
            byTime.put(new Key(at, tx.seq()), tx);
            return tx;
        } finally {
            rw.writeLock().unlock();
        }
    }

    /** [from, to) for one account, oldest first. */
    public List<Tx> between(String accountId, Instant fromInclusive, Instant toExclusive) {
        rw.readLock().lock();
        try {
            List<Tx> out = new ArrayList<>();
            for (Tx tx : byTime.subMap(new Key(fromInclusive, Long.MIN_VALUE), true, new Key(toExclusive, Long.MIN_VALUE), false).values()) {
                if (tx.accountId().equals(accountId)) out.add(tx);
            }
            return Collections.unmodifiableList(out);
        } finally {
            rw.readLock().unlock();
        }
    }

    /** Balance = sum of all entries at or before `asOf`. */
    public BigDecimal balanceAsOf(String accountId, Instant asOf) {
        rw.readLock().lock();
        try {
            BigDecimal b = BigDecimal.ZERO;
            for (Tx tx : byTime.headMap(new Key(asOf, Long.MAX_VALUE), true).values()) {
                if (tx.accountId().equals(accountId)) b = b.add(tx.amount());
            }
            return b;
        } finally {
            rw.readLock().unlock();
        }
    }

    /** Newest first, keyset pagination by (at, seq) cursor: stable even when new entries arrive. */
    public List<Tx> latest(String accountId, int limit, Tx cursor) {
        if (limit <= 0) throw new IllegalArgumentException("limit must be positive");
        rw.readLock().lock();
        try {
            NavigableMap<Key, Tx> view = cursor == null ? byTime.descendingMap()
                    : byTime.headMap(new Key(cursor.at(), cursor.seq()), false).descendingMap();
            List<Tx> out = new ArrayList<>();
            for (Tx tx : view.values()) {
                if (tx.accountId().equals(accountId)) {
                    out.add(tx);
                    if (out.size() == limit) break;
                }
            }
            return Collections.unmodifiableList(out);
        } finally {
            rw.readLock().unlock();
        }
    }

    public int size() {
        rw.readLock().lock();
        try {
            return byTime.size();
        } finally {
            rw.readLock().unlock();
        }
    }
}
