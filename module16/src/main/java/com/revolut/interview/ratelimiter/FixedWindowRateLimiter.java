package com.revolut.interview.ratelimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

/**
 * Kata 4a: fixed window. Simple, O(1), but allows up to 2x the limit across a window boundary.
 * Window counters are keyed by (key, windowIndex) so old windows are naturally abandoned; purge() removes them.
 */
public final class FixedWindowRateLimiter implements RateLimiter {
    private record WindowKey(String key, long window) {
    }

    private final int limit;
    private final long windowMillis;
    private final LongSupplier millisClock;
    private final ConcurrentHashMap<WindowKey, AtomicInteger> counters = new ConcurrentHashMap<>();

    public FixedWindowRateLimiter(int limit, long windowMillis, LongSupplier millisClock) {
        if (limit <= 0 || windowMillis <= 0) throw new IllegalArgumentException("limit and window must be positive");
        this.limit = limit;
        this.windowMillis = windowMillis;
        this.millisClock = millisClock;
    }

    @Override
    public boolean allow(String key) {
        long window = Math.floorDiv(millisClock.getAsLong(), windowMillis);
        AtomicInteger c = counters.computeIfAbsent(new WindowKey(key, window), k -> new AtomicInteger());
        if (c.incrementAndGet() <= limit) return true;
        c.decrementAndGet(); // keep the counter honest for metrics
        return false;
    }

    public int purge() {
        long current = Math.floorDiv(millisClock.getAsLong(), windowMillis);
        int before = counters.size();
        counters.keySet().removeIf(k -> k.window() < current);
        return before - counters.size();
    }
}
