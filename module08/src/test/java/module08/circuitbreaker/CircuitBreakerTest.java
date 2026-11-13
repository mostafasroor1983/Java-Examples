package module08.circuitbreaker;

import module08.hierarchy.PaymentProviderException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CircuitBreakerTest {

    @Test
    void closedToOpenToHalfOpenToClosed() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.EPOCH);
        CircuitBreaker cb = new CircuitBreaker("provider", 3, Duration.ofSeconds(30), now::get);
        RuntimeException boom = new IllegalStateException("down");

        for (int i = 0; i < 3; i++) assertThrows(IllegalStateException.class, () -> cb.call(() -> { throw boom; }));
        assertEquals(CircuitBreaker.State.OPEN, cb.currentState());
        assertThrows(CircuitBreaker.CircuitOpenException.class, () -> cb.call(() -> "never called"));

        now.set(Instant.EPOCH.plusSeconds(30));
        assertEquals(CircuitBreaker.State.HALF_OPEN, cb.currentState());
        assertEquals("ok", cb.call(() -> "ok"));
        assertEquals(CircuitBreaker.State.CLOSED, cb.currentState());
        assertEquals(0, cb.consecutiveFailures());
    }

    @Test
    void halfOpenFailureReopens() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.EPOCH);
        CircuitBreaker cb = new CircuitBreaker("provider", 1, Duration.ofSeconds(10), now::get);
        assertThrows(IllegalStateException.class, () -> cb.call(() -> { throw new IllegalStateException(); }));
        now.set(Instant.EPOCH.plusSeconds(10));
        assertEquals(CircuitBreaker.State.HALF_OPEN, cb.currentState());
        assertThrows(IllegalStateException.class, () -> cb.call(() -> { throw new IllegalStateException(); }));
        assertEquals(CircuitBreaker.State.OPEN, cb.currentState());
        now.set(Instant.EPOCH.plusSeconds(15));
        assertEquals(CircuitBreaker.State.OPEN, cb.currentState(), "cooldown restarted");
    }

    @Test
    void successResetsFailureCount() {
        CircuitBreaker cb = new CircuitBreaker("p", 3, Duration.ofSeconds(1), () -> Instant.EPOCH);
        assertThrows(IllegalStateException.class, () -> cb.call(() -> { throw new IllegalStateException(); }));
        assertThrows(IllegalStateException.class, () -> cb.call(() -> { throw new IllegalStateException(); }));
        cb.call(() -> "ok");
        assertEquals(0, cb.consecutiveFailures());
        assertEquals(CircuitBreaker.State.CLOSED, cb.currentState());
        assertThrows(IllegalArgumentException.class, () -> new CircuitBreaker("p", 0, Duration.ofSeconds(1), Instant::now));
    }

    @Test
    void openCircuitBlocksAllConcurrentCallers() throws Exception {
        CircuitBreaker cb = new CircuitBreaker("p", 1, Duration.ofHours(1), () -> Instant.EPOCH);
        assertThrows(IllegalStateException.class, () -> cb.call(() -> { throw new IllegalStateException(); }));
        AtomicInteger executed = new AtomicInteger(), rejected = new AtomicInteger();
        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try {
                    cb.call(() -> executed.incrementAndGet());
                } catch (CircuitBreaker.CircuitOpenException e) {
                    rejected.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        assertTrue(done.await(10, TimeUnit.SECONDS));
        pool.shutdown();
        assertEquals(0, executed.get());
        assertEquals(threads, rejected.get());
    }

    @Test
    void retryPolicyRetriesOnlyRetryable() {
        AtomicInteger calls = new AtomicInteger();
        RetryPolicy retry = new RetryPolicy(3);
        PaymentProviderException e = assertThrows(PaymentProviderException.class, () -> retry.execute(() -> {
            calls.incrementAndGet();
            throw new PaymentProviderException("timeout", null, true);
        }));
        assertEquals(3, calls.get());
        assertEquals(2, e.getSuppressed().length, "earlier attempts attached as suppressed");

        calls.set(0);
        assertThrows(PaymentProviderException.class, () -> retry.execute(() -> {
            calls.incrementAndGet();
            throw new PaymentProviderException("bad request", null, false);
        }));
        assertEquals(1, calls.get());

        calls.set(0);
        assertEquals("ok", retry.execute(() -> {
            if (calls.incrementAndGet() < 2) throw new PaymentProviderException("flaky", null, true);
            return "ok";
        }));
    }
}
