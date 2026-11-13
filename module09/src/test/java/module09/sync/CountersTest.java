package module09.sync;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CountersTest {
    private static final int THREADS = 8, PER_THREAD = 50_000;

    private static long hammer(Counters.Counter c) throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(THREADS);
        for (int t = 0; t < THREADS; t++) {
            new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < PER_THREAD; i++) c.increment();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        start.countDown();
        assertTrue(done.await(20, TimeUnit.SECONDS));
        return c.value();
    }

    @Test
    void unsafeAndVolatileCountersCanLoseUpdates() throws Exception {
        long expected = (long) THREADS * PER_THREAD;
        assertTrue(hammer(new Counters.UnsafeCounter()) <= expected);
        assertTrue(hammer(new Counters.VolatileCounter()) <= expected);
    }

    @Test
    void correctCountersAreExact() throws Exception {
        long expected = (long) THREADS * PER_THREAD;
        assertEquals(expected, hammer(new Counters.SynchronizedCounter()));
        assertEquals(expected, hammer(new Counters.AtomicCounter()));
        assertEquals(expected, hammer(new Counters.AdderCounter()));
    }

    @Test
    void volatileFlagStopsWorker() throws Exception {
        VolatileFlag flag = new VolatileFlag();
        long[] result = {-1};
        Thread worker = new Thread(() -> result[0] = flag.runUntilStopped());
        worker.start();
        flag.stop();
        worker.join(5000);
        assertFalse(worker.isAlive(), "worker must observe the volatile write");
        assertTrue(result[0] >= 0);
    }

    @Test
    void concurrentTransfersConserveMoney() throws Exception {
        Account a = new Account("A", new BigDecimal("1000"));
        Account b = new Account("B", new BigDecimal("1000"));
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(2 * 4);
        for (int t = 0; t < 4; t++) {
            new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < 1000; i++) Account.transfer(a, b, BigDecimal.ONE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
            new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < 1000; i++) Account.transfer(b, a, BigDecimal.ONE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        start.countDown();
        assertTrue(done.await(20, TimeUnit.SECONDS), "deadlock-free thanks to lock ordering");
        assertEquals(new BigDecimal("2000"), a.balance().add(b.balance()));
        assertTrue(a.balance().signum() >= 0 && b.balance().signum() >= 0);
        assertFalse(a.withdraw(new BigDecimal("1000000")));
        assertThrows(IllegalArgumentException.class, () -> Account.transfer(a, a, BigDecimal.ONE));
    }
}
