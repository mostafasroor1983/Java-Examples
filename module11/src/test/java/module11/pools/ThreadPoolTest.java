package module11.pools;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ThreadPoolTest {

    @Test
    void maxThreadsOnlyStartWhenQueueIsFull() throws Exception {
        ThreadPoolExecutor pool = PoolFactory.bounded("settle", 1, 3, 2, RejectionPolicies.abort());
        CountDownLatch release = new CountDownLatch(1);
        Runnable blocker = () -> {
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        try {
            pool.execute(blocker);                 // core thread 1 busy
            pool.execute(blocker);                 // queued (1/2)
            pool.execute(blocker);                 // queued (2/2)
            assertEquals(1, pool.getPoolSize(), "no extra threads while queue has room");
            pool.execute(blocker);                 // queue full -> thread 2
            pool.execute(blocker);                 // queue full -> thread 3
            assertEquals(3, pool.getPoolSize());
            assertThrows(RejectedExecutionException.class, () -> pool.execute(blocker), "max threads + full queue -> reject");
            assertTrue(pool.getQueue().remainingCapacity() == 0);
            assertTrue(pool.getThreadFactory().newThread(() -> { }).getName().startsWith("settle-"));
        } finally {
            release.countDown();
            assertTrue(PoolFactory.shutdown(pool, 5, TimeUnit.SECONDS));
        }
    }

    @Test
    void callerRunsAppliesBackPressure() throws Exception {
        ThreadPoolExecutor pool = PoolFactory.bounded("io", 1, 1, 1, RejectionPolicies.callerRuns());
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger ranOnCaller = new AtomicInteger();
        String callerName = Thread.currentThread().getName();
        try {
            pool.execute(() -> { try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } });
            pool.execute(() -> { });                                  // queued
            pool.execute(() -> { if (Thread.currentThread().getName().equals(callerName)) ranOnCaller.incrementAndGet(); });
            assertEquals(1, ranOnCaller.get(), "third task ran synchronously on the submitting thread");
        } finally {
            release.countDown();
            assertTrue(PoolFactory.shutdown(pool, 5, TimeUnit.SECONDS));
        }
    }

    @Test
    void countingDropRecordsRejections() throws Exception {
        var drop = new RejectionPolicies.CountingDrop();
        ThreadPoolExecutor pool = PoolFactory.bounded("drop", 1, 1, 1, drop);
        CountDownLatch release = new CountDownLatch(1);
        try {
            pool.execute(() -> { try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } });
            pool.execute(() -> { });
            for (int i = 0; i < 5; i++) pool.execute(() -> { });
            assertEquals(5, drop.dropped());
        } finally {
            release.countDown();
            assertTrue(PoolFactory.shutdown(pool, 5, TimeUnit.SECONDS));
        }
    }

    @Test
    void sizingFormulas() {
        assertEquals(9, PoolFactory.cpuBoundSize(8));
        assertEquals(80, PoolFactory.ioBoundSize(8, 1.0, 90, 10));
        assertEquals(40, PoolFactory.ioBoundSize(8, 0.5, 90, 10));
        assertThrows(IllegalArgumentException.class, () -> PoolFactory.ioBoundSize(8, 1.5, 90, 10));
        assertThrows(IllegalArgumentException.class, () -> PoolFactory.ioBoundSize(8, 1.0, 90, 0));
    }

    @Test
    void shutdownNowInterruptsStuckTasks() throws Exception {
        ThreadPoolExecutor pool = PoolFactory.bounded("stuck", 1, 1, 1, RejectionPolicies.abort());
        CountDownLatch interrupted = new CountDownLatch(1);
        pool.execute(() -> {
            try {
                Thread.sleep(60_000);
            } catch (InterruptedException e) {
                interrupted.countDown();
            }
        });
        assertTrue(PoolFactory.shutdown(pool, 100, TimeUnit.MILLISECONDS));
        assertTrue(interrupted.await(5, TimeUnit.SECONDS));
        assertTrue(pool.isTerminated());
    }
}
