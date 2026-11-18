package module04.cache;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FxRateCacheTest {
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2024-03-01T10:00:00Z"));

    @Test
    void stampedeOnColdCacheCallsProviderOnce() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch release = new CountDownLatch(1);
        var cache = new FxRateCache(pair -> {
            calls.incrementAndGet();
            try {
                release.await(); // keep the load in flight while everyone else arrives
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return new BigDecimal("1.0850");
        }, Duration.ofSeconds(30), now::get);

        ExecutorService pool = Executors.newFixedThreadPool(32);
        CountDownLatch done = new CountDownLatch(200);
        AtomicInteger correct = new AtomicInteger();
        for (int i = 0; i < 200; i++) {
            pool.submit(() -> {
                if (cache.rate("EURUSD").compareTo(new BigDecimal("1.0850")) == 0) correct.incrementAndGet();
                done.countDown();
            });
        }
        Thread.sleep(100); // give callers time to pile up (not used for synchronisation of results)
        release.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        pool.shutdown();
        assertEquals(1, calls.get());
        assertEquals(200, correct.get());
    }

    @Test
    void ttlExpiryAndInvalidationReload() {
        AtomicInteger calls = new AtomicInteger();
        var cache = new FxRateCache(p -> BigDecimal.valueOf(calls.incrementAndGet()), Duration.ofSeconds(30), now::get);
        assertEquals(BigDecimal.ONE, cache.rate("EURUSD"));
        now.set(now.get().plusSeconds(29));
        assertEquals(BigDecimal.ONE, cache.rate("EURUSD"));
        now.set(now.get().plusSeconds(1)); // exactly at TTL => stale
        assertEquals(BigDecimal.valueOf(2), cache.rate("EURUSD"));
        cache.invalidate("EURUSD");
        assertEquals(BigDecimal.valueOf(3), cache.rate("EURUSD"));
    }

    @Test
    void failuresAreNotCached() {
        AtomicInteger calls = new AtomicInteger();
        var cache = new FxRateCache(p -> {
            if (calls.incrementAndGet() == 1) throw new IllegalStateException("provider down");
            return BigDecimal.TEN;
        }, Duration.ofMinutes(1), now::get);
        assertThrows(IllegalStateException.class, () -> cache.rate("GBPUSD"));
        assertEquals(0, cache.size());
        assertEquals(BigDecimal.TEN, cache.rate("GBPUSD"));
    }
}
