package com.revolut.buildit.ratelimiter;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Build It #7: API rate limiter with per-tier limits (sliding window counter: current + previous window weighted),
 * a global ceiling, and Retry-After hints. Sliding window *counter* approximates the log with O(1) memory.
 */
public final class TieredRateLimiter {
    public enum Tier { FREE, PRO, ENTERPRISE }

    public record Decision(boolean allowed, long remaining, long retryAfterMillis) {
    }

    private static final class Window {
        long currentStart;
        long currentCount;
        long previousCount;
    }

    private final Map<Tier, Integer> limitsPerWindow;
    private final long windowMillis;
    private final long globalLimit;
    private final LongSupplier clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Window global = new Window();

    public TieredRateLimiter(Map<Tier, Integer> limitsPerWindow, long windowMillis, long globalLimit, LongSupplier clock) {
        this.limitsPerWindow = Map.copyOf(limitsPerWindow);
        if (windowMillis <= 0 || globalLimit <= 0) throw new IllegalArgumentException("window and global limit must be positive");
        this.windowMillis = windowMillis;
        this.globalLimit = globalLimit;
        this.clock = Objects.requireNonNull(clock);
    }

    public Decision check(String clientId, Tier tier) {
        int limit = Optional.ofNullable(limitsPerWindow.get(tier)).orElseThrow(() -> new IllegalArgumentException("no limit for " + tier));
        long now = clock.getAsLong();
        Window w = windows.computeIfAbsent(clientId, k -> new Window());
        // Lock order is always client -> global, so no deadlock; both checks and both increments happen atomically.
        synchronized (w) {
            roll(w, now);
            double est = estimate(w, now);
            if (est >= limit) return new Decision(false, 0, retryAfter(now));
            synchronized (global) {
                roll(global, now);
                if (estimate(global, now) >= globalLimit) return new Decision(false, 0, retryAfter(now));
                global.currentCount++;
            }
            w.currentCount++;
            return new Decision(true, Math.max(0, (long) Math.floor(limit - est - 1)), 0);
        }
    }

    /** Weighted count: previous window contributes proportionally to how much of it still overlaps the sliding window. */
    private double estimate(Window w, long now) {
        double elapsedFraction = (double) (now - w.currentStart) / windowMillis;
        return w.previousCount * (1.0 - elapsedFraction) + w.currentCount;
    }

    private void roll(Window w, long now) {
        long start = Math.floorDiv(now, windowMillis) * windowMillis;
        if (w.currentStart == start) return;
        w.previousCount = (start - w.currentStart == windowMillis) ? w.currentCount : 0; // skipped windows => nothing carries over
        w.currentCount = 0;
        w.currentStart = start;
    }

    private long retryAfter(long now) {
        return windowMillis - Math.floorMod(now, windowMillis);
    }
}
