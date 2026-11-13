package module09.threads;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ThreadBasicsTest {

    @Test
    void joinWaitsForAllWorkers() throws Exception {
        AtomicInteger done = new AtomicInteger();
        List<String> names = ThreadBasics.runAndJoin(List.of(done::incrementAndGet, done::incrementAndGet, done::incrementAndGet));
        assertEquals(3, done.get());
        assertEquals(List.of("settlement-worker-0", "settlement-worker-1", "settlement-worker-2"), names);
    }

    @Test
    void interruptionIsCooperative() throws Exception {
        assertEquals(1000, ThreadBasics.interruptibleWorker(1000));
        assertTrue(ThreadBasics.sleepWasInterrupted(null));
    }

    @Test
    void uncaughtExceptionsReachHandlerNotCaller() throws Exception {
        Throwable t = ThreadBasics.captureUncaught(() -> { throw new IllegalStateException("boom"); });
        assertInstanceOf(IllegalStateException.class, t);
        assertNull(ThreadBasics.captureUncaught(() -> { }));
        assertTrue(ThreadBasics.isDaemon(true));
        assertFalse(ThreadBasics.isDaemon(false));
    }
}
