package module13.pinning;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class PinningDemoTest {

    @Test
    void bothVariantsCompleteAllTasks() throws Exception {
        // Correctness holds either way; pinning is a throughput problem, which the doc explains and -Djdk.tracePinnedThreads reveals.
        assertEquals(50, PinningDemo.blockInsideSynchronized(50, 5));
        assertEquals(50, PinningDemo.blockWithReentrantLock(50, 5));
    }

    @Test
    void reentrantLockVariantOverlapsSleeps() throws Exception {
        long start = System.nanoTime();
        assertEquals(2000, PinningDemo.blockWithReentrantLock(2000, 100));
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertTrue(ms < 20_000, "2000 x 100ms must overlap on virtual threads, took " + ms + "ms");
    }

    @Test
    void manyVirtualThreadsCanParkTogether() throws Exception {
        assertTrue(PinningDemo.parkManyAtOnce(5_000));
        assertFalse(PinningDemo.runningOnVirtualThread());
        try (var vt = Executors.newVirtualThreadPerTaskExecutor()) {
            assertTrue(vt.submit(PinningDemo::runningOnVirtualThread).get());
        }
    }
}
