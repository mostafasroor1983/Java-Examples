package module14.falsesharing;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * False sharing: two threads write to different variables that live on the same 64-byte cache line;
 * each write invalidates the other core's line, so independent counters crawl.
 * Fixes: padding (@Contended needs -XX:-RestrictContended), striping (LongAdder), or per-thread accumulation.
 */
public final class FalseSharingDemo {
    private FalseSharingDemo() {
    }

    /** BAD: adjacent longs in one object - almost certainly the same cache line. */
    public static final class AdjacentCounters {
        public volatile long a;
        public volatile long b;
    }

    /** GOOD (manual padding): 7 longs between a and b push them onto different lines (56 bytes + header). */
    public static final class PaddedCounters {
        public volatile long a;
        @SuppressWarnings("unused") private long p1, p2, p3, p4, p5, p6, p7;
        public volatile long b;
    }

    /** Runs two threads incrementing a and b `iterations` times; returns elapsed nanos. Correctness is asserted, timing is informational. */
    public static long run(Runnable incA, Runnable incB, long iterations) throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(2);
        Thread ta = new Thread(() -> loop(start, done, incA, iterations));
        Thread tb = new Thread(() -> loop(start, done, incB, iterations));
        ta.start();
        tb.start();
        long t0 = System.nanoTime();
        start.countDown();
        done.await();
        return System.nanoTime() - t0;
    }

    private static void loop(CountDownLatch start, CountDownLatch done, Runnable body, long iterations) {
        try {
            start.await();
            for (long i = 0; i < iterations; i++) body.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            done.countDown();
        }
    }

    /** Library answer: LongAdder stripes cells to avoid contention and (mostly) false sharing. */
    public static long adderUnderContention(int threads, long perThread) throws InterruptedException {
        LongAdder adder = new LongAdder();
        CountDownLatch done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            new Thread(() -> {
                try {
                    for (long i = 0; i < perThread; i++) adder.increment();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        done.await();
        return adder.sum();
    }

    public static long atomicUnderContention(int threads, long perThread) throws InterruptedException {
        AtomicLong al = new AtomicLong();
        CountDownLatch done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            new Thread(() -> {
                try {
                    for (long i = 0; i < perThread; i++) al.incrementAndGet();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        done.await();
        return al.get();
    }
}
