package module10.snapshot;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * GOOD: publish an IMMUTABLE snapshot through one AtomicReference (a volatile under the hood).
 *
 * <p>JMM reasoning: the writer builds a fully constructed {@link Snapshot} (final fields, unmodifiable map), then does
 * one volatile write of the reference. Any reader that sees the new reference also sees everything written before
 * that write (volatile write happens-before subsequent volatile read), so bid, ask, version and timestamp are always
 * mutually consistent. Readers never lock; writers use copy-on-write.
 *
 * <p>Fits read-heavy data that changes in batches: FX quote books, fee schedules, fraud rule sets, feature flags.
 * Does not fit large maps with high write rates, since each update copies the whole map.
 */
public final class QuoteBook {
    public record Quote(long bidMicros, long askMicros) {
        public Quote {
            if (bidMicros <= 0 || askMicros < bidMicros) throw new IllegalArgumentException("crossed or invalid quote");
        }
    }

    /** Immutable: final fields + Map.copyOf. Safe to share without synchronisation once published. */
    public record Snapshot(long version, Instant asOf, Map<String, Quote> quotes) {
        public Snapshot {
            quotes = Map.copyOf(quotes);
        }
    }

    private final AtomicReference<Snapshot> current =
            new AtomicReference<>(new Snapshot(0, Instant.EPOCH, Map.of()));

    /** Atomically applies a batch of quote updates (copy-on-write). Retries internally on concurrent writers. */
    public Snapshot publish(Map<String, Quote> updates, Instant asOf) {
        Objects.requireNonNull(updates, "updates");
        return current.updateAndGet(prev -> {
            // must be side-effect free: updateAndGet may call this more than once under contention
            Map<String, Quote> next = new HashMap<>(prev.quotes());
            next.putAll(updates);
            return new Snapshot(prev.version() + 1, asOf, next);
        });
    }

    /** One volatile read: everything a caller needs for pricing comes from the same consistent snapshot. */
    public Snapshot snapshot() {
        return current.get();
    }

    public Optional<Quote> quote(String pair) {
        return Optional.ofNullable(current.get().quotes().get(pair));
    }
}
