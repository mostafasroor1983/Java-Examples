package com.revolut.interview.ratelimiter;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/** Kata 4b: sliding log. Exact; O(limit) memory per key. */
public final class SlidingWindowRateLimiter implements RateLimiter {
    private final int limit;
    private final long windowMillis;
    private final LongSupplier millisClock;
    private final ConcurrentHashMap<String, Deque<Long>> logs = new ConcurrentHashMap<>();

    public SlidingWindowRateLimiter(int limit, long windowMillis, LongSupplier millisClock) {
        if (limit <= 0 || windowMillis <= 0) throw new IllegalArgumentException("limit and window must be positive");
        this.limit = limit;
        this.windowMillis = windowMillis;
        this.millisClock = millisClock;
    }

    @Override
    public boolean allow(String key) {
        long now = millisClock.getAsLong();
        Deque<Long> log = logs.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (log) {
            while (!log.isEmpty() && log.peekFirst() <= now - windowMillis) log.pollFirst();
            if (log.size() >= limit) return false;
            log.addLast(now);
            return true;
        }
    }
}
