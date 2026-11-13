package module15.p14slidingwindow;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * P14 - Sliding-window counter/sum over the last `window`, using `buckets` fixed sub-windows (ring buffer).
 * O(1) memory, O(buckets) query. Used for "spend in the last 24h" limits and velocity checks.
 */
public final class P14SlidingWindowStats {
    private final long bucketMillis;
    private final int buckets;
    private final long[] bucketStart;   // start time (ms) of the window each slot currently represents
    private final long[] counts;
    private final BigDecimal[] sums;
    private final Supplier<Instant> clock;

    public P14SlidingWindowStats(Duration window, int buckets, Supplier<Instant> clock) {
        if (buckets <= 0 || window.toMillis() % buckets != 0) throw new IllegalArgumentException("window must divide evenly into buckets");
        this.buckets = buckets;
        this.bucketMillis = window.toMillis() / buckets;
        this.bucketStart = new long[buckets];
        this.counts = new long[buckets];
        this.sums = new BigDecimal[buckets];
        java.util.Arrays.fill(sums, BigDecimal.ZERO);
        java.util.Arrays.fill(bucketStart, Long.MIN_VALUE);
        this.clock = Objects.requireNonNull(clock);
    }

    public synchronized void record(BigDecimal amount) {
        Objects.requireNonNull(amount);
        long now = clock.get().toEpochMilli();
        int idx = slot(now);
        long start = bucketStartFor(now);
        if (bucketStart[idx] != start) { // slot holds stale data from a previous rotation
            bucketStart[idx] = start;
            counts[idx] = 0;
            sums[idx] = BigDecimal.ZERO;
        }
        counts[idx]++;
        sums[idx] = sums[idx].add(amount);
    }

    public synchronized long count() {
        long now = clock.get().toEpochMilli();
        long oldestValid = bucketStartFor(now) - (long) (buckets - 1) * bucketMillis;
        long c = 0;
        for (int i = 0; i < buckets; i++) if (bucketStart[i] >= oldestValid) c += counts[i];
        return c;
    }

    public synchronized BigDecimal sum() {
        long now = clock.get().toEpochMilli();
        long oldestValid = bucketStartFor(now) - (long) (buckets - 1) * bucketMillis;
        BigDecimal s = BigDecimal.ZERO;
        for (int i = 0; i < buckets; i++) if (bucketStart[i] >= oldestValid) s = s.add(sums[i]);
        return s;
    }

    private long bucketStartFor(long millis) {
        return Math.floorDiv(millis, bucketMillis) * bucketMillis;
    }

    private int slot(long millis) {
        return (int) Math.floorMod(Math.floorDiv(millis, bucketMillis), buckets);
    }
}
