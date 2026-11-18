package module12.retry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AsyncRetryTest {
    static final class Transient extends RuntimeException { Transient() { super("503"); } }
    static final class Declined extends RuntimeException { Declined() { super("do not honour"); } }

    private final ExecutorService pool = Executors.newFixedThreadPool(2);

    @AfterEach
    void close() {
        pool.shutdownNow();
    }

    private AsyncRetry retry(int attempts, Duration deadline) {
        return new AsyncRetry(attempts, Duration.ofMillis(10), Duration.ofMillis(40), deadline,
                t -> t instanceof Transient, () -> 1.0, pool); // random fixed at 1.0 => delay == ceiling
    }

    @Test
    void backoffIsCappedExponentialTimesJitter() {
        var r = new AsyncRetry(5, Duration.ofMillis(100), Duration.ofMillis(1_000), Duration.ofSeconds(5),
                t -> true, () -> 0.5, pool);
        assertEquals(50, r.backoffMillis(1));
        assertEquals(100, r.backoffMillis(2));
        assertEquals(200, r.backoffMillis(3));
        assertEquals(500, r.backoffMillis(10), "capped at 1000 x 0.5");
        assertEquals(500, r.backoffMillis(200), "no shift overflow");
    }

    @Test
    void succeedsAfterTransientFailuresWithSameIdempotencyKey() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        String key = "idem-123";
        var f = retry(5, Duration.ofSeconds(5)).execute(() -> {
            assertEquals("idem-123", key);
            return calls.incrementAndGet() < 3 ? CompletableFuture.failedFuture(new Transient())
                    : CompletableFuture.completedFuture("AUTH-OK");
        });
        assertEquals("AUTH-OK", f.get(5, TimeUnit.SECONDS));
        assertEquals(3, calls.get());
    }

    @Test
    void nonRetryableFailsImmediately() {
        AtomicInteger calls = new AtomicInteger();
        var f = retry(5, Duration.ofSeconds(5)).<String>execute(() -> {
            calls.incrementAndGet();
            return CompletableFuture.failedFuture(new Declined());
        });
        var ex = assertThrows(ExecutionException.class, () -> f.get(5, TimeUnit.SECONDS));
        assertInstanceOf(Declined.class, ex.getCause());
        assertEquals(1, calls.get(), "a decline is never retried");
    }

    @Test
    void attemptsAreBoundedAndSynchronousThrowsCount() {
        AtomicInteger calls = new AtomicInteger();
        var f = retry(3, Duration.ofSeconds(5)).<String>execute(() -> {
            calls.incrementAndGet();
            throw new Transient();
        });
        var ex = assertThrows(ExecutionException.class, () -> f.get(5, TimeUnit.SECONDS));
        assertInstanceOf(Transient.class, ex.getCause());
        assertEquals(3, calls.get());
    }

    @Test
    void overallDeadlineWinsOverHangingAttempt() {
        var f = retry(3, Duration.ofMillis(100)).<String>execute(CompletableFuture::new); // never completes
        var ex = assertThrows(ExecutionException.class, () -> f.get(5, TimeUnit.SECONDS));
        assertInstanceOf(TimeoutException.class, ex.getCause());
        assertTrue(AsyncRetry.isTimeout(ex.getCause()));
    }
}
