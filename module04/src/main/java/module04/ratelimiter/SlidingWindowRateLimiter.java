package module04.ratelimiter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Sliding-window log rate limiter: at most `limit` requests per `window` per key.
 * Each key holds a deque of timestamps; on each request we evict timestamps older than now - window.
 * Memory: O(limit) per key. Exact (no boundary burst like fixed windows).
 *
 * Per-key synchronisation on the deque keeps different keys independent.
 */
public final class SlidingWindowRateLimiter {
    private final int limit;
    private final Duration window;
    private final Supplier<Instant> clock;
    private final ConcurrentHashMap<String, Deque<Instant>> logs = new ConcurrentHashMap<>();

    public SlidingWindowRateLimiter(int limit, Duration window, Supplier<Instant> clock) {
        if (limit <= 0) throw new IllegalArgumentException("limit must be positive");
        if (window.isZero() || window.isNegative()) throw new IllegalArgumentException("window must be positive");
        this.limit = limit;
        this.window = window;
        this.clock = Objects.requireNonNull(clock);
    }

    public boolean tryAcquire(String key) {
        Objects.requireNonNull(key, "key");
        Instant now = clock.get();
        Deque<Instant> log = logs.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (log) {
            Instant cutoff = now.minus(window);
            while (!log.isEmpty() && !log.peekFirst().isAfter(cutoff)) {
                log.pollFirst();
            }
            if (log.size() >= limit) {
                return false;
            }
            log.addLast(now);
            return true;
        }
    }

    public int currentCount(String key) {
        Deque<Instant> log = logs.get(key);
        if (log == null) return 0;
        synchronized (log) {
            Instant cutoff = clock.get().minus(window);
            return (int) log.stream().filter(t -> t.isAfter(cutoff)).count();
        }
    }

    /** Housekeeping: drop keys with no recent activity so the map does not grow forever. */
    public int purgeIdleKeys() {
        Instant cutoff = clock.get().minus(window);
        int before = logs.size();
        logs.entrySet().removeIf(e -> {
            Deque<Instant> log = e.getValue();
            synchronized (log) {
                return log.isEmpty() || !log.peekLast().isAfter(cutoff);
            }
        });
        return before - logs.size();
    }
}
