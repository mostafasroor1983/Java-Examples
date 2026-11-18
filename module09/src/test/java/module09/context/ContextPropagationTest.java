package module09.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ContextPropagationTest {
    private final ExecutorService single = Executors.newSingleThreadExecutor(); // one reused thread makes leaks visible

    @AfterEach
    void tearDown() throws InterruptedException {
        CorrelationContext.clear();
        single.shutdown();
        assertTrue(single.awaitTermination(5, TimeUnit.SECONDS));
    }

    private String runAndCapture(java.util.concurrent.Executor ex) throws Exception {
        AtomicReference<String> seen = new AtomicReference<>();
        var done = new java.util.concurrent.CountDownLatch(1);
        ex.execute(() -> {
            seen.set(CorrelationContext.current().orElse("<none>"));
            done.countDown();
        });
        assertTrue(done.await(5, TimeUnit.SECONDS));
        return seen.get();
    }

    @Test
    void plainPoolLosesAndLeaksContext() throws Exception {
        CorrelationContext.set("req-A");
        assertEquals("<none>", runAndCapture(single), "lost: pool thread has no context");
        single.execute(() -> CorrelationContext.set("req-A")); // a task that forgets to clean up
        CorrelationContext.set("req-B");
        assertEquals("req-A", runAndCapture(single), "leaked: request B's work logged under request A");
    }

    @Test
    void propagatingExecutorCarriesAndCleansUp() throws Exception {
        var ex = new ContextPropagatingExecutor(single);
        CorrelationContext.set("req-A");
        assertEquals("req-A", runAndCapture(ex));
        CorrelationContext.set("req-B");
        assertEquals("req-B", runAndCapture(ex));
        CorrelationContext.clear();
        assertEquals("<none>", runAndCapture(ex), "no stale id after the previous task");
        assertEquals("<none>", runAndCapture(single), "worker thread restored to its own (empty) state");
    }
}
