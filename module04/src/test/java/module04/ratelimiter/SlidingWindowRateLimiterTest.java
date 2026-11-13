package module04.ratelimiter;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SlidingWindowRateLimiterTest {

    @Test
    void allowsLimitThenRejectsThenSlides() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.EPOCH);
        SlidingWindowRateLimiter rl = new SlidingWindowRateLimiter(3, Duration.ofSeconds(10), now::get);
        assertTrue(rl.tryAcquire("c1"));
        now.set(Instant.EPOCH.plusSeconds(2));
        assertTrue(rl.tryAcquire("c1"));
        now.set(Instant.EPOCH.plusSeconds(4));
        assertTrue(rl.tryAcquire("c1"));
        assertFalse(rl.tryAcquire("c1"));
        assertEquals(3, rl.currentCount("c1"));
        assertTrue(rl.tryAcquire("c2"), "keys are independent");

        now.set(Instant.EPOCH.plusSeconds(10)); // first request (t=0) falls out at t > 10? cutoff = 0 -> not after -> evicted
        assertTrue(rl.tryAcquire("c1"));
        assertFalse(rl.tryAcquire("c1"));
        now.set(Instant.EPOCH.plusSeconds(12)); // t=2 evicted
        assertTrue(rl.tryAcquire("c1"));
    }

    @Test
    void purgeIdleKeys() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.EPOCH);
        SlidingWindowRateLimiter rl = new SlidingWindowRateLimiter(1, Duration.ofSeconds(1), now::get);
        rl.tryAcquire("a");
        now.set(Instant.EPOCH.plusSeconds(5));
        rl.tryAcquire("b");
        assertEquals(1, rl.purgeIdleKeys());
        assertEquals(0, rl.currentCount("a"));
        assertEquals(1, rl.currentCount("b"));
    }

    @Test
    void validation() {
        assertThrows(IllegalArgumentException.class, () -> new SlidingWindowRateLimiter(0, Duration.ofSeconds(1), Instant::now));
        assertThrows(IllegalArgumentException.class, () -> new SlidingWindowRateLimiter(1, Duration.ZERO, Instant::now));
        assertThrows(NullPointerException.class, () -> new SlidingWindowRateLimiter(1, Duration.ofSeconds(1), Instant::now).tryAcquire(null));
    }

    @Test
    void concurrentCallersNeverExceedLimit() throws Exception {
        Instant fixed = Instant.EPOCH;
        SlidingWindowRateLimiter rl = new SlidingWindowRateLimiter(100, Duration.ofMinutes(1), () -> fixed);
        int threads = 16, perThread = 100;
        AtomicInteger allowed = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) if (rl.tryAcquire("shared")) allowed.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        pool.shutdown();
        assertEquals(100, allowed.get());
    }
}
