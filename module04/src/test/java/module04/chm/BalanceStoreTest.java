package module04.chm;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BalanceStoreTest {

    @Test
    void basicOperations() {
        BalanceStore s = new BalanceStore();
        s.open("A", new BigDecimal("100"));
        assertThrows(IllegalStateException.class, () -> s.open("A", BigDecimal.ONE));
        assertEquals(new BigDecimal("150"), s.credit("A", new BigDecimal("50")));
        assertEquals(new BigDecimal("120"), s.debit("A", new BigDecimal("30")));
        assertThrows(BalanceStore.InsufficientFundsException.class, () -> s.debit("A", new BigDecimal("1000")));
        assertEquals(new BigDecimal("120"), s.balance("A").orElseThrow(), "failed debit leaves balance untouched");
        assertThrows(IllegalArgumentException.class, () -> s.debit("Z", BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> s.credit("A", BigDecimal.ZERO));
        assertTrue(s.debitOptimistic("A", new BigDecimal("20")));
        assertFalse(s.debitOptimistic("A", new BigDecimal("1000")));
        assertEquals(new BigDecimal("100"), s.balance("A").orElseThrow());
    }

    @Test
    void concurrentCreditsAreExact() throws Exception {
        BalanceStore s = new BalanceStore();
        s.open("A", BigDecimal.ZERO);
        int threads = 16, per = 2000;
        runConcurrently(threads, () -> {
            for (int i = 0; i < per; i++) s.credit("A", BigDecimal.ONE);
        });
        assertEquals(new BigDecimal(threads * per), s.balance("A").orElseThrow());
    }

    @Test
    void concurrentDebitsNeverOverdraw() throws Exception {
        BalanceStore s = new BalanceStore();
        s.open("A", new BigDecimal("1000"));
        int threads = 16, per = 200; // 3200 attempted, only 1000 can succeed
        AtomicInteger successes = new AtomicInteger();
        runConcurrently(threads, () -> {
            for (int i = 0; i < per; i++) {
                try {
                    s.debit("A", BigDecimal.ONE);
                    successes.incrementAndGet();
                } catch (BalanceStore.InsufficientFundsException expected) {
                    // fine
                }
            }
        });
        assertEquals(1000, successes.get());
        assertEquals(BigDecimal.ZERO, s.balance("A").orElseThrow());
    }

    @Test
    void optimisticDebitsNeverOverdraw() throws Exception {
        BalanceStore s = new BalanceStore();
        s.open("A", new BigDecimal("500"));
        AtomicInteger successes = new AtomicInteger();
        runConcurrently(8, () -> {
            for (int i = 0; i < 200; i++) {
                if (s.debitOptimistic("A", BigDecimal.ONE)) successes.incrementAndGet();
            }
        });
        assertEquals(500, successes.get());
        assertEquals(BigDecimal.ZERO, s.balance("A").orElseThrow());
    }

    @Test
    void brokenStoreLosesUpdates() throws Exception {
        BrokenBalanceStore s = new BrokenBalanceStore();
        s.open("A", BigDecimal.ZERO);
        int threads = 16, per = 5000;
        runConcurrently(threads, () -> {
            for (int i = 0; i < per; i++) s.credit("A", BigDecimal.ONE);
        });
        // Not asserting strict inequality (a lucky run could be exact); assert the invariant that must hold.
        assertTrue(s.balance("A").compareTo(new BigDecimal(threads * per)) <= 0);
    }

    static void runConcurrently(int threads, Runnable body) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try {
                    start.await();
                    body.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(20, TimeUnit.SECONDS));
        pool.shutdown();
    }
}
