package module11.conditions;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FundingAwaitAccountTest {

    private static void awaitWaiters(FundingAwaitAccount a, int n) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (a.waiters() < n) {
            if (System.nanoTime() > deadline) fail("waiters never parked");
            Thread.onSpinWait();
        }
    }

    @Test
    void waitingPayoutProceedsWhenFundsArrive() throws Exception {
        var acc = new FundingAwaitAccount(new BigDecimal("10"));
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Future<Boolean> payout = pool.submit(() -> acc.debitWhenFunded(new BigDecimal("100"), Duration.ofSeconds(10)));
        awaitWaiters(acc, 1);
        acc.credit(new BigDecimal("50"));  // not enough: the waiter re-checks and keeps waiting
        acc.credit(new BigDecimal("40"));
        assertTrue(payout.get(5, TimeUnit.SECONDS));
        assertEquals(BigDecimal.ZERO, acc.balance());
        pool.shutdown();
    }

    @Test
    void deadlinePassesWithoutPartialDebit() throws Exception {
        var acc = new FundingAwaitAccount(new BigDecimal("10"));
        long start = System.nanoTime();
        assertFalse(acc.debitWhenFunded(new BigDecimal("100"), Duration.ofMillis(50)));
        assertTrue(System.nanoTime() - start >= TimeUnit.MILLISECONDS.toNanos(45), "honoured the timeout");
        assertEquals(new BigDecimal("10"), acc.balance());
    }

    @Test
    void signalAllLetsTheRightWaiterProceed() throws Exception {
        var acc = new FundingAwaitAccount(BigDecimal.ZERO);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<Boolean> big = pool.submit(() -> acc.debitWhenFunded(new BigDecimal("1000"), Duration.ofMillis(300)));
        Future<Boolean> small = pool.submit(() -> acc.debitWhenFunded(new BigDecimal("5"), Duration.ofSeconds(5)));
        awaitWaiters(acc, 2);
        acc.credit(new BigDecimal("5")); // with signal() the big waiter might be woken alone and the small one never
        assertTrue(small.get(5, TimeUnit.SECONDS));
        assertFalse(big.get(5, TimeUnit.SECONDS));
        pool.shutdown();
    }

    @Test
    void interruptionDuringShutdownLeavesStateUnchanged() throws Exception {
        var acc = new FundingAwaitAccount(BigDecimal.ONE);
        AtomicReference<Throwable> thrown = new AtomicReference<>();
        Thread t = new Thread(() -> {  // Java 17: no Thread.ofPlatform() (that builder is Java 21)
            try {
                acc.debitWhenFunded(BigDecimal.TEN, Duration.ofMinutes(1));
            } catch (Throwable e) {
                thrown.set(e);
            }
        });
        t.start();
        awaitWaiters(acc, 1);
        t.interrupt();
        t.join(5_000);
        assertInstanceOf(InterruptedException.class, thrown.get());
        assertEquals(BigDecimal.ONE, acc.balance());
    }
}
