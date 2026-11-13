package module09.executors;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ExecutorDemosTest {

    @Test
    void invokeAllPreservesSubmissionOrder() throws Exception {
        var fees = ExecutorDemos.calculateFees(List.of(new BigDecimal("100"), new BigDecimal("250"), new BigDecimal("7")), new BigDecimal("0.01"), 3);
        assertEquals(List.of(new BigDecimal("1.00"), new BigDecimal("2.50"), new BigDecimal("0.07")), fees);
    }

    @Test
    void taskExceptionsSurfaceOnGet() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Throwable cause = ExecutorDemos.causeOfFailingTask(pool);
            assertInstanceOf(IllegalStateException.class, cause);
            assertEquals("provider down", cause.getMessage());
            // pool thread survived: it can still run work
            assertEquals("alive", pool.submit(() -> "alive").get(1, TimeUnit.SECONDS));
        } finally {
            ExecutorDemos.shutdownGracefully(pool, 1, TimeUnit.SECONDS);
        }
    }

    @Test
    void timeoutCancelsSlowTask() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        CountDownLatch interrupted = new CountDownLatch(1);
        try {
            String r = ExecutorDemos.withTimeout(pool, () -> {
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException e) {
                    interrupted.countDown();
                    throw e;
                }
                return "late";
            }, 50);
            assertEquals("TIMEOUT", r);
            assertTrue(interrupted.await(2, TimeUnit.SECONDS), "cancel(true) interrupts the task");
            assertEquals("fast", ExecutorDemos.withTimeout(pool, () -> "fast", 1000));
        } finally {
            assertTrue(ExecutorDemos.shutdownGracefully(pool, 2, TimeUnit.SECONDS));
        }
    }

    @Test
    void shutdownRejectsNewTasks() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        assertTrue(ExecutorDemos.shutdownGracefully(pool, 1, TimeUnit.SECONDS));
        assertTrue(pool.isTerminated());
        assertThrows(java.util.concurrent.RejectedExecutionException.class, () -> pool.submit(() -> "x"));
    }
}
