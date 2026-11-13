package module11.deadlock;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DeadlockTest {

    @Test
    void badOrderingDeadlocksAndJvmDetectsIt() throws Exception {
        assertTrue(DeadlockDemo.reproduceDeadlock(), "ThreadMXBean.findDeadlockedThreads should report the two demo threads");
    }

    @Test
    void orderedLockingNeverDeadlocksAndConservesMoney() throws Exception {
        var a = new DeadlockFixes.Account("A", new BigDecimal("1000"));
        var b = new DeadlockFixes.Account("B", new BigDecimal("1000"));
        runOpposingTransfers(() -> DeadlockFixes.transferOrdered(a, b, BigDecimal.ONE),
                () -> DeadlockFixes.transferOrdered(b, a, BigDecimal.ONE));
        assertEquals(new BigDecimal("2000"), a.balance().add(b.balance()));
    }

    @Test
    void tryLockWithBackoffCompletes() throws Exception {
        var a = new DeadlockFixes.Account("A", new BigDecimal("1000"));
        var b = new DeadlockFixes.Account("B", new BigDecimal("1000"));
        AtomicInteger gaveUp = new AtomicInteger();
        runOpposingTransfers(
                () -> { try { if (!DeadlockFixes.transferTryLock(a, b, BigDecimal.ONE, 1000)) gaveUp.incrementAndGet(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } },
                () -> { try { if (!DeadlockFixes.transferTryLock(b, a, BigDecimal.ONE, 1000)) gaveUp.incrementAndGet(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } });
        assertEquals(new BigDecimal("2000"), a.balance().add(b.balance()));
        assertEquals(0, gaveUp.get());
    }

    private static void runOpposingTransfers(Runnable ab, Runnable ba) throws InterruptedException {
        int threads = 4, per = 500;
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(threads * 2);
        for (int t = 0; t < threads; t++) {
            for (Runnable r : new Runnable[]{ab, ba}) {
                new Thread(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < per; i++) r.run();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                }).start();
            }
        }
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "transfers must finish - no deadlock");
    }
}
