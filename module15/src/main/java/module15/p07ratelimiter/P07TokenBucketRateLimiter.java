package module15.p07ratelimiter;

import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * P07 - Token bucket: capacity tokens, refilled at `refillPerSecond`; a request consumes one token.
 * Allows bursts up to capacity, then smooths to the refill rate. O(1) memory per key, lazy refill on access.
 */
public final class P07TokenBucketRateLimiter {
    private final long capacity;
    private final double refillPerNano;
    private final LongSupplier nanoClock;
    private double tokens;
    private long lastRefillNanos;

    public P07TokenBucketRateLimiter(long capacity, double refillPerSecond, LongSupplier nanoClock) {
        if (capacity <= 0 || refillPerSecond <= 0) throw new IllegalArgumentException("capacity and refill must be positive");
        this.capacity = capacity;
        this.refillPerNano = refillPerSecond / 1_000_000_000.0;
        this.nanoClock = Objects.requireNonNull(nanoClock);
        this.tokens = capacity;
        this.lastRefillNanos = nanoClock.getAsLong();
    }

    public synchronized boolean tryAcquire() {
        return tryAcquire(1);
    }

    public synchronized boolean tryAcquire(int n) {
        if (n <= 0) throw new IllegalArgumentException("n must be positive");
        refill();
        if (tokens >= n) {
            tokens -= n;
            return true;
        }
        return false;
    }

    public synchronized double availableTokens() {
        refill();
        return tokens;
    }

    private void refill() {
        long now = nanoClock.getAsLong();
        long elapsed = now - lastRefillNanos;
        if (elapsed > 0) {
            tokens = Math.min(capacity, tokens + elapsed * refillPerNano);
            lastRefillNanos = now;
        }
    }
}
