package com.revolut.interview.ratelimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/** Kata 4c: token bucket per key. Bursts up to capacity, steady rate afterwards, O(1) per key. */
public final class TokenBucketRateLimiter implements RateLimiter {
    private static final class Bucket {
        double tokens;
        long lastNanos;

        Bucket(double tokens, long lastNanos) {
            this.tokens = tokens;
            this.lastNanos = lastNanos;
        }
    }

    private final int capacity;
    private final double refillPerNano;
    private final LongSupplier nanoClock;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(int capacity, double refillPerSecond, LongSupplier nanoClock) {
        if (capacity <= 0 || refillPerSecond <= 0) throw new IllegalArgumentException("capacity and refill must be positive");
        this.capacity = capacity;
        this.refillPerNano = refillPerSecond / 1_000_000_000.0;
        this.nanoClock = nanoClock;
    }

    @Override
    public boolean allow(String key) {
        long now = nanoClock.getAsLong();
        Bucket b = buckets.computeIfAbsent(key, k -> new Bucket(capacity, now));
        synchronized (b) {
            long elapsed = now - b.lastNanos;
            if (elapsed > 0) {
                b.tokens = Math.min(capacity, b.tokens + elapsed * refillPerNano);
                b.lastNanos = now;
            }
            if (b.tokens >= 1) {
                b.tokens -= 1;
                return true;
            }
            return false;
        }
    }
}
