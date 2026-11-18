package module10.lockfree;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class CreditLimitReserverTest {

    @Test
    void singleThreadedRules() {
        var r = new CreditLimitReserver(10_000);
        assertTrue(r.tryReserve(6_000));
        assertFalse(r.tryReserve(4_001), "would exceed limit: rejected without side effects");
        assertEquals(6_000, r.reserved());
        assertTrue(r.tryReserve(4_000));
        assertEquals(0, r.available());
        r.release(1_000);
        assertEquals(1_000, r.available());
        assertThrows(IllegalStateException.class, () -> r.release(50_000));
        assertThrows(IllegalArgumentException.class, () -> r.tryReserve(0));
        var huge = new CreditLimitReserver(Long.MAX_VALUE);
        assertTrue(huge.tryReserve(Long.MAX_VALUE - 1));
        assertFalse(huge.tryReserve(5), "long overflow is detected, not wrapped to a negative balance");
    }

    @Test
    void neverExceedsLimitUnderContention() throws Exception {
        long limit = 1_000_000;
        var r = new CreditLimitReserver(limit);
        AtomicLong granted = new AtomicLong();
        AtomicBoolean overLimitSeen = new AtomicBoolean();
        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                start.await();
                var rnd = ThreadLocalRandom.current();
                for (int i = 0; i < 20_000; i++) {
                    long amt = rnd.nextLong(1, 500);
                    if (r.tryReserve(amt)) {
                        granted.addAndGet(amt);
                        if (rnd.nextInt(4) == 0) { r.release(amt); granted.addAndGet(-amt); } // reversal
                    }
                    if (r.reserved() > limit) overLimitSeen.set(true);
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
        assertFalse(overLimitSeen.get(), "limit never exceeded at any observed instant");
        assertEquals(granted.get(), r.reserved(), "no lost or phantom reservations");
        assertTrue(r.reserved() <= limit);
    }
}
