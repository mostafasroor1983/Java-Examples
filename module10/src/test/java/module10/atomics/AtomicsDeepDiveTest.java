package module10.atomics;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AtomicsDeepDiveTest {

    @Test
    void atomicBalanceNeverGoesNegative() throws Exception {
        var bal = new AtomicsDeepDive.AtomicBalance(new BigDecimal("1000.00"));
        int threads = 8, per = 500;
        AtomicInteger ok = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < per; i++) if (bal.tryDebit(new BigDecimal("1.00"))) ok.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        start.countDown();
        assertTrue(done.await(20, TimeUnit.SECONDS));
        assertEquals(1000, ok.get());
        assertEquals(new BigDecimal("0.00"), bal.get());
        assertEquals(new BigDecimal("5.50"), bal.credit(new BigDecimal("5.50")));
    }

    @Test
    void accumulatorAdderArray() throws Exception {
        var max = new AtomicsDeepDive.MaxLatency();
        var stats = new AtomicsDeepDive.RequestStats();
        int threads = 4, per = 1000;
        CountDownLatch done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            int id = t;
            new Thread(() -> {
                try {
                    for (int i = 0; i < per; i++) {
                        max.record(id * 1000L + i);
                        stats.record(i % 10 == 0 ? 503 : 200);
                    }
                } finally {
                    done.countDown();
                }
            }).start();
        }
        assertTrue(done.await(20, TimeUnit.SECONDS));
        assertEquals(3999, max.get());
        assertEquals(4000, stats.total());
        assertEquals(400, stats.errors());
        assertEquals(3600, stats.bucket(2));
        assertEquals(400, stats.bucket(5));
    }

    @Test
    void updateVariantsAndSequence() {
        assertArrayEquals(new int[]{5, 11}, AtomicsDeepDive.oldAndNew(new AtomicInteger(5)));
        var seq = new AtomicsDeepDive.Sequence(3);
        assertEquals(0, seq.next());
        assertEquals(1, seq.next());
        assertEquals(2, seq.next());
        assertEquals(0, seq.next());
        assertThrows(IllegalArgumentException.class, () -> new AtomicsDeepDive.Sequence(0));
    }
}
