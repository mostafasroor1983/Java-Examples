package module15.p06dedup;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * P06 - Deduplicate events (e.g. webhook redeliveries) by id within a TTL window, bounded memory via purge.
 * `firstSeen` is atomic: exactly one caller gets true for a given id within the window.
 */
public final class P06EventDeduplicator {
    private final ConcurrentHashMap<String, Instant> seen = new ConcurrentHashMap<>();
    private final Duration window;
    private final Supplier<Instant> clock;

    public P06EventDeduplicator(Duration window, Supplier<Instant> clock) {
        if (window.isZero() || window.isNegative()) throw new IllegalArgumentException("window must be positive");
        this.window = window;
        this.clock = Objects.requireNonNull(clock);
    }

    public boolean firstSeen(String eventId) {
        Objects.requireNonNull(eventId);
        Instant now = clock.get();
        Instant cutoff = now.minus(window);
        boolean[] fresh = {false};
        seen.compute(eventId, (k, prev) -> {
            if (prev == null || !prev.isAfter(cutoff)) {
                fresh[0] = true;
                return now;
            }
            return prev;
        });
        return fresh[0];
    }

    public int purgeExpired() {
        Instant cutoff = clock.get().minus(window);
        int before = seen.size();
        seen.entrySet().removeIf(e -> !e.getValue().isAfter(cutoff));
        return before - seen.size();
    }

    public int size() {
        return seen.size();
    }
}
