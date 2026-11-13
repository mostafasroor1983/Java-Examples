package module04.races;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.LongAdder;

/**
 * Demonstrates lost updates with a plain HashMap vs. correct results with CHM.merge / LongAdder.
 * Each `run*` method spins N threads incrementing M times and returns the observed total.
 */
public final class LostUpdateDemo {
    private LostUpdateDemo() {
    }

    /** BAD: plain HashMap with read-modify-write. Result is usually < threads*iterations, may even corrupt the map. */
    public static long runUnsafe(int threads, int iterations) throws InterruptedException {
        Map<String, Long> counts = new HashMap<>();
        counts.put("k", 0L);
        runAll(threads, () -> {
            for (int i = 0; i < iterations; i++) {
                counts.put("k", counts.get("k") + 1);
            }
        });
        Long v = counts.get("k");
        return v == null ? -1 : v;
    }

    /** BAD: CHM but still get+put (compound action). */
    public static long runCompoundOnChm(int threads, int iterations) throws InterruptedException {
        ConcurrentHashMap<String, Long> counts = new ConcurrentHashMap<>();
        counts.put("k", 0L);
        runAll(threads, () -> {
            for (int i = 0; i < iterations; i++) {
                counts.put("k", counts.get("k") + 1);
            }
        });
        return counts.get("k");
    }

    /** GOOD: merge is atomic per key. */
    public static long runMerge(int threads, int iterations) throws InterruptedException {
        ConcurrentHashMap<String, Long> counts = new ConcurrentHashMap<>();
        runAll(threads, () -> {
            for (int i = 0; i < iterations; i++) {
                counts.merge("k", 1L, Long::sum);
            }
        });
        return counts.get("k");
    }

    /** GOOD: LongAdder spreads contention across cells; sum() at the end. */
    public static long runLongAdder(int threads, int iterations) throws InterruptedException {
        LongAdder adder = new LongAdder();
        runAll(threads, () -> {
            for (int i = 0; i < iterations; i++) adder.increment();
        });
        return adder.sum();
    }

    private static void runAll(int threads, Runnable body) throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            Thread.ofPlatform().start(() -> {
                try {
                    start.await();
                    body.run();
                } catch (Throwable ignored) {
                    // HashMap corruption may throw; we only care about the count
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        done.await();
    }
}
