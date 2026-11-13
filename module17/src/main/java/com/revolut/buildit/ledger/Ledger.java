package com.revolut.buildit.ledger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * Build It #8: double-entry ledger. A journal entry has >= 2 postings whose amounts sum to zero per currency.
 * The journal is append-only; balances are a derived projection kept in step under the write lock;
 * reconcile() rebuilds balances from the journal and compares. Idempotent by entry id.
 */
public final class Ledger {
    public record Posting(String account, String currency, BigDecimal amount) {
        public Posting {
            Objects.requireNonNull(account);
            Objects.requireNonNull(currency);
            Objects.requireNonNull(amount);
            if (amount.signum() == 0) throw new IllegalArgumentException("posting amount must be non-zero");
        }
    }

    public record JournalEntry(String id, Instant at, String description, List<Posting> postings) {
        public JournalEntry {
            Objects.requireNonNull(id);
            postings = List.copyOf(postings);
            if (postings.size() < 2) throw new IllegalArgumentException("an entry needs at least two postings");
            Map<String, BigDecimal> perCurrency = new HashMap<>();
            for (Posting p : postings) perCurrency.merge(p.currency(), p.amount(), BigDecimal::add);
            for (var e : perCurrency.entrySet()) {
                if (e.getValue().signum() != 0) throw new UnbalancedEntryException(id, e.getKey(), e.getValue());
            }
        }
    }

    public static final class UnbalancedEntryException extends IllegalArgumentException {
        public UnbalancedEntryException(String id, String currency, BigDecimal delta) {
            super("entry " + id + " does not balance in " + currency + ": " + delta);
        }
    }

    public record AccountKey(String account, String currency) {
    }

    private final List<JournalEntry> journal = new ArrayList<>();
    private final Map<AccountKey, BigDecimal> balances = new HashMap<>();
    private final Set<String> ids = ConcurrentHashMap.newKeySet();
    private final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
    private final Supplier<Instant> clock;

    public Ledger(Supplier<Instant> clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    /** Appends; returns false if the id was already posted (idempotent). Throws if unbalanced. */
    public boolean post(String id, String description, List<Posting> postings) {
        JournalEntry entry = new JournalEntry(id, clock.get(), description, postings); // validates balance before taking the lock
        if (!ids.add(id)) return false;
        rw.writeLock().lock();
        try {
            journal.add(entry);
            for (Posting p : postings) balances.merge(new AccountKey(p.account(), p.currency()), p.amount(), BigDecimal::add);
            return true;
        } finally {
            rw.writeLock().unlock();
        }
    }

    /** Convenience: transfer = debit from, credit to. */
    public boolean transfer(String id, String from, String to, String currency, BigDecimal amount) {
        if (amount.signum() <= 0) throw new IllegalArgumentException("amount must be positive");
        return post(id, "transfer " + from + "->" + to, List.of(new Posting(from, currency, amount.negate()), new Posting(to, currency, amount)));
    }

    public BigDecimal balance(String account, String currency) {
        rw.readLock().lock();
        try {
            return balances.getOrDefault(new AccountKey(account, currency), BigDecimal.ZERO);
        } finally {
            rw.readLock().unlock();
        }
    }

    public List<JournalEntry> journal() {
        rw.readLock().lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(journal));
        } finally {
            rw.readLock().unlock();
        }
    }

    /** Rebuild balances from the journal and compare with the projection. Returns the mismatching keys (empty = consistent). */
    public Set<AccountKey> reconcile() {
        rw.readLock().lock();
        try {
            Map<AccountKey, BigDecimal> rebuilt = new HashMap<>();
            for (JournalEntry e : journal) for (Posting p : e.postings()) rebuilt.merge(new AccountKey(p.account(), p.currency()), p.amount(), BigDecimal::add);
            Set<AccountKey> bad = new java.util.HashSet<>();
            for (AccountKey k : rebuilt.keySet()) if (rebuilt.get(k).compareTo(balances.getOrDefault(k, BigDecimal.ZERO)) != 0) bad.add(k);
            for (AccountKey k : balances.keySet()) if (!rebuilt.containsKey(k) && balances.get(k).signum() != 0) bad.add(k);
            return bad;
        } finally {
            rw.readLock().unlock();
        }
    }

    /** Sum of all balances per currency must be zero in a closed system. */
    public BigDecimal systemTotal(String currency) {
        rw.readLock().lock();
        try {
            BigDecimal t = BigDecimal.ZERO;
            for (var e : balances.entrySet()) if (e.getKey().currency().equals(currency)) t = t.add(e.getValue());
            return t;
        } finally {
            rw.readLock().unlock();
        }
    }
}
