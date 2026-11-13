package module11.locks;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LocksTest {

    @Test
    void reentrantLockTryAndCondition() throws Exception {
        ReentrantLockAccount acc = new ReentrantLockAccount(new BigDecimal("10"), true);
        assertTrue(acc.isFair());
        assertTrue(acc.tryWithdraw(new BigDecimal("5"), 100, TimeUnit.MILLISECONDS));
        assertFalse(acc.tryWithdraw(new BigDecimal("50"), 100, TimeUnit.MILLISECONDS));
        assertTrue(acc.audit().startsWith("balance=5 holds=1"));

        AtomicBoolean result = new AtomicBoolean();
        CountDownLatch waiting = new CountDownLatch(1), done = new CountDownLatch(1);
        Thread waiter = new Thread(() -> {
            try {
                waiting.countDown();
                result.set(acc.awaitWithdraw(new BigDecimal("20"), 5, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        });
        waiter.start();
        waiting.await();
        acc.deposit(new BigDecimal("15")); // 5 + 15 = 20 -> signalAll wakes the waiter
        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertTrue(result.get());
        assertEquals(BigDecimal.ZERO, acc.balance());
        assertFalse(acc.awaitWithdraw(BigDecimal.ONE, 20, TimeUnit.MILLISECONDS), "times out when funds never arrive");
    }

    @Test
    void readWriteCacheAllowsConcurrentReadersAndIsConsistent() throws Exception {
        ReadWriteFxCache cache = new ReadWriteFxCache();
        cache.refresh(Map.of("GBPUSD", new BigDecimal("1.25")));
        assertEquals(new BigDecimal("1.25"), cache.rate("GBPUSD").orElseThrow());
        assertEquals(new BigDecimal("1.30"), cache.refreshAndRead(Map.of("GBPUSD", new BigDecimal("1.30")), "GBPUSD"));

        int readers = 8;
        AtomicInteger inconsistent = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(readers + 1);
        for (int r = 0; r < readers; r++) {
            new Thread(() -> {
                try {
                    for (int i = 0; i < 2000; i++) {
                        // a refresh always writes both pairs; a reader must never see only one
                        boolean a = cache.rate("EURUSD").isPresent();
                        int size = cache.size();
                        if (a && size != 2 && size != 1) inconsistent.incrementAndGet();
                    }
                } finally {
                    done.countDown();
                }
            }).start();
        }
        new Thread(() -> {
            try {
                for (int i = 0; i < 500; i++) {
                    cache.refresh(Map.of("GBPUSD", BigDecimal.ONE, "EURUSD", BigDecimal.TEN));
                    cache.refresh(Map.of("GBPUSD", BigDecimal.ONE));
                }
            } finally {
                done.countDown();
            }
        }).start();
        assertTrue(done.await(20, TimeUnit.SECONDS));
        assertEquals(0, inconsistent.get());
    }

    @Test
    void stampedLockKeepsTotalInvariant() throws Exception {
        StampedBalance sb = new StampedBalance(new BigDecimal("1000"));
        int threads = 8;
        AtomicInteger badTotals = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(threads * 2);
        for (int t = 0; t < threads; t++) {
            new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < 500; i++) {
                        if (sb.reserve(BigDecimal.ONE)) sb.release(BigDecimal.ONE);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
            new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < 2000; i++) {
                        if (sb.total().compareTo(new BigDecimal("1000")) != 0) badTotals.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        start.countDown();
        assertTrue(done.await(20, TimeUnit.SECONDS));
        assertEquals(0, badTotals.get(), "optimistic reads must never observe a torn available/reserved pair");
        assertEquals(new BigDecimal("1000"), sb.available());
        assertEquals(BigDecimal.ZERO, sb.reserved());
        assertTrue(sb.reserve(new BigDecimal("100")));
        assertTrue(sb.capture(new BigDecimal("40")));
        assertFalse(sb.capture(new BigDecimal("100")));
        assertEquals(new BigDecimal("60"), sb.reserved());
        assertEquals(new BigDecimal("960"), sb.total());
    }
}
