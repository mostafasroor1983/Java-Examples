package module03.ledgers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Practical ledger combining the collections: an append-only ArrayList of entries,
 * a HashMap index by account, and a TreeMap index by timestamp for range queries.
 */
public final class TransactionLedger {
    public record Entry(String id, String accountId, BigDecimal amount, Instant at) {
        public Entry {
            Objects.requireNonNull(id);
            Objects.requireNonNull(accountId);
            Objects.requireNonNull(amount);
            Objects.requireNonNull(at);
            if (amount.signum() == 0) throw new IllegalArgumentException("zero amount entry is meaningless");
        }
    }

    private final List<Entry> journal = new ArrayList<>();
    private final Map<String, List<Entry>> byAccount = new HashMap<>();
    private final NavigableMap<Instant, List<Entry>> byTime = new TreeMap<>();
    private final Map<String, BigDecimal> balances = new HashMap<>();

    public void append(Entry e) {
        journal.add(e);
        byAccount.computeIfAbsent(e.accountId(), k -> new ArrayList<>()).add(e);
        byTime.computeIfAbsent(e.at(), k -> new ArrayList<>()).add(e);
        balances.merge(e.accountId(), e.amount(), BigDecimal::add);
    }

    public BigDecimal balance(String accountId) {
        return balances.getOrDefault(accountId, BigDecimal.ZERO);
    }

    public List<Entry> forAccount(String accountId) {
        return Collections.unmodifiableList(byAccount.getOrDefault(accountId, List.of()));
    }

    public List<Entry> between(Instant fromInclusive, Instant toExclusive) {
        List<Entry> out = new ArrayList<>();
        byTime.subMap(fromInclusive, true, toExclusive, false).values().forEach(out::addAll);
        return out;
    }

    public int size() {
        return journal.size();
    }

    /** Invariant check used in tests: sum of journal == sum of balances. */
    public boolean balancesReconcile() {
        BigDecimal fromJournal = journal.stream().map(Entry::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal fromBalances = balances.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return fromJournal.compareTo(fromBalances) == 0;
    }
}
