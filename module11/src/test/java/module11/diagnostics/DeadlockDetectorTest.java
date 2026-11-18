package module11.diagnostics;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;

class DeadlockDetectorTest {

    // Note: the detector reports EVERY deadlock in the JVM. Other tests in this module (module11.deadlock) leave
    // deliberately deadlocked daemon threads behind, so assertions filter by this test's unique thread names -
    // exactly what you would do in production when correlating a report with a specific thread pool.
    private static final String PREFIX = "detector-test-";

    private static List<DeadlockDetector.DeadlockedThread> mine(DeadlockDetector d) {
        return d.detect().stream().filter(t -> t.threadName().startsWith(PREFIX)).toList();
    }

    @Test
    void detectsRealDeadlockAndRecoversByInterrupting() throws Exception {
        ReentrantLock accountA = new ReentrantLock(), accountB = new ReentrantLock();
        CountDownLatch bothHoldFirstLock = new CountDownLatch(2);
        Thread t1 = transfer(PREFIX + "A-to-B", accountA, accountB, bothHoldFirstLock);
        Thread t2 = transfer(PREFIX + "B-to-A", accountB, accountA, bothHoldFirstLock);

        DeadlockDetector detector = new DeadlockDetector();
        List<DeadlockDetector.DeadlockedThread> found = List.of();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (found.isEmpty() && System.nanoTime() < deadline) {
            found = mine(detector);
            Thread.onSpinWait();
        }
        assertEquals(2, found.size());
        assertTrue(found.stream().anyMatch(d -> d.threadName().equals(PREFIX + "A-to-B") && d.heldBy().equals(PREFIX + "B-to-A")));
        assertTrue(found.stream().allMatch(d -> d.waitingFor().contains("ReentrantLock")));

        // recovery: lockInterruptibly lets us break the cycle (synchronized could NOT be interrupted)
        t1.interrupt();
        t2.interrupt();
        t1.join(5_000);
        t2.join(5_000);
        assertFalse(t1.isAlive() || t2.isAlive());
        assertTrue(mine(detector).isEmpty(), "cycle broken");
    }

    private static Thread transfer(String name, ReentrantLock first, ReentrantLock second, CountDownLatch ready) {
        Thread t = new Thread(() -> {
            try {
                first.lockInterruptibly();
                try {
                    ready.countDown();
                    ready.await(); // guarantee both threads hold their first lock -> deterministic deadlock
                    second.lockInterruptibly();
                    second.unlock();
                } finally {
                    first.unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, name);
        t.setDaemon(true);
        t.start();
        return t;
    }
}
