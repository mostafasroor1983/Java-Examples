package module04.idempotency;

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

class IdempotencyCacheTest {

    @Test
    void executesOnceWithinTtlThenAgainAfterExpiry() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.EPOCH);
        IdempotencyCache<String> cache = new IdempotencyCache<>(Duration.ofSeconds(60), now::get);
        AtomicInteger calls = new AtomicInteger();
        assertEquals("r1", cache.execute("key", () -> "r" + calls.incrementAndGet()));
        assertEquals("r1", cache.execute("key", () -> "r" + calls.incrementAndGet()));
        assertEquals(1, calls.get());
        assertEquals("r1", cache.peek("key").orElseThrow());

        now.set(Instant.EPOCH.plusSeconds(61));
        assertTrue(cache.peek("key").isEmpty());
        assertEquals("r2", cache.execute("key", () -> "r" + calls.incrementAndGet()));
    }

    @Test
    void purgeRemovesExpiredOnly() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.EPOCH);
        IdempotencyCache<Integer> cache = new IdempotencyCache<>(Duration.ofSeconds(10), now::get);
        cache.execute("old", () -> 1);
        now.set(Instant.EPOCH.plusSeconds(5));
        cache.execute("new", () -> 2);
        now.set(Instant.EPOCH.plusSeconds(11));
        assertEquals(1, cache.purgeExpired());
        assertEquals(1, cache.size());
        assertEquals(2, cache.peek("new").orElseThrow());
    }

    @Test
    void rejectsBadKeysAndTtl() {
        IdempotencyCache<String> cache = new IdempotencyCache<>(Duration.ofSeconds(1), Instant::now);
        assertThrows(IllegalArgumentException.class, () -> cache.execute(" ", () -> "x"));
        assertThrows(IllegalArgumentException.class, () -> cache.execute("x".repeat(129), () -> "x"));
        assertThrows(IllegalArgumentException.class, () -> new IdempotencyCache<>(Duration.ZERO, Instant::now));
    }

    @Test
    void concurrentCallersWithSameKeyExecuteOperationExactlyOnce() throws Exception {
        IdempotencyCache<Integer> cache = new IdempotencyCache<>(Duration.ofMinutes(1), Instant::now);
        AtomicInteger executions = new AtomicInteger();
        int threads = 32;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(threads);
        Integer[] results = new Integer[threads];
        for (int t = 0; t < threads; t++) {
            int idx = t;
            pool.submit(() -> {
                try {
                    start.await();
                    results[idx] = cache.execute("pay-123", executions::incrementAndGet);
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
        assertEquals(1, executions.get());
        for (Integer r : results) assertEquals(1, r);
    }
}
