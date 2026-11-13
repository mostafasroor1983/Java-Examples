package module15.p07ratelimiter;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class P07Test {
    @Test
    void burstThenRefill() {
        AtomicLong nanos = new AtomicLong();
        var rl = new P07TokenBucketRateLimiter(5, 10, nanos::get); // 10 tokens/s
        for (int i = 0; i < 5; i++) assertTrue(rl.tryAcquire());
        assertFalse(rl.tryAcquire());
        nanos.addAndGet(100_000_000L); // 0.1s -> 1 token
        assertTrue(rl.tryAcquire());
        assertFalse(rl.tryAcquire());
        nanos.addAndGet(10_000_000_000L); // 10s -> capped at capacity
        assertEquals(5.0, rl.availableTokens(), 1e-9);
        assertTrue(rl.tryAcquire(5));
        assertFalse(rl.tryAcquire(1));
        assertThrows(IllegalArgumentException.class, () -> rl.tryAcquire(0));
        assertThrows(IllegalArgumentException.class, () -> new P07TokenBucketRateLimiter(0, 1, nanos::get));
    }

    @Test
    void concurrentCallersNeverExceedCapacity() throws Exception {
        var rl = new P07TokenBucketRateLimiter(100, 0.000001, () -> 0L); // effectively no refill
        AtomicInteger allowed = new AtomicInteger();
        int threads = 16;
        CountDownLatch done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) new Thread(() -> { try { for (int i = 0; i < 50; i++) if (rl.tryAcquire()) allowed.incrementAndGet(); } finally { done.countDown(); } }).start();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        assertEquals(100, allowed.get());
    }
}
