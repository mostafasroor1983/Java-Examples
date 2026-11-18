package module04.delay;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PaymentRetrySchedulerTest {

    @Test
    void backoffIsExponentialAndCapped() {
        try (var s = new PaymentRetryScheduler(t -> true, Duration.ofMillis(100), Duration.ofMillis(500), 5)) {
            assertEquals(Duration.ofMillis(100), s.backoff(1));
            assertEquals(Duration.ofMillis(200), s.backoff(2));
            assertEquals(Duration.ofMillis(400), s.backoff(3));
            assertEquals(Duration.ofMillis(500), s.backoff(4));
            assertEquals(Duration.ofMillis(500), s.backoff(60), "no overflow for huge attempt numbers");
        }
    }

    @Test
    void succeedsOnThirdAttemptWithSameIdempotencyKey() throws Exception {
        Map<String, Integer> attempts = new ConcurrentHashMap<>();
        CountDownLatch accepted = new CountDownLatch(1);
        try (var s = new PaymentRetryScheduler(t -> {
            assertEquals("idem-1", t.idempotencyKey());
            attempts.put(t.paymentId(), t.attempt());
            if (t.attempt() < 3) return false;
            accepted.countDown();
            return true;
        }, Duration.ofMillis(10), Duration.ofMillis(40), 5)) {
            s.scheduleRetry("PAY-1", "idem-1");
            assertTrue(accepted.await(5, TimeUnit.SECONDS));
            assertEquals(3, attempts.get("PAY-1"));
            assertTrue(s.deadLetters().isEmpty());
        }
    }

    @Test
    void exhaustedRetriesGoToDeadLetterIncludingThrowingSubmitter() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        try (var s = new PaymentRetryScheduler(t -> {
            calls.incrementAndGet();
            throw new IllegalStateException("PSP 503");
        }, Duration.ofMillis(5), Duration.ofMillis(10), 3)) {
            s.scheduleRetry("PAY-9", "idem-9");
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (s.deadLetters().isEmpty() && System.nanoTime() < deadline) Thread.onSpinWait();
            assertEquals(1, s.deadLetters().size());
            assertEquals(3, s.deadLetters().get(0).attempt());
            assertEquals(3, calls.get());
        }
    }

    @Test
    void shutdownReturnsPendingTasksInsteadOfLosingThem() {
        var s = new PaymentRetryScheduler(t -> true, Duration.ofHours(1), Duration.ofHours(1), 3);
        s.scheduleRetry("PAY-1", "k1");
        s.scheduleRetry("PAY-2", "k2");
        List<PaymentRetryScheduler.RetryTask> pending = s.shutdown();
        assertEquals(2, pending.size());
        assertThrows(IllegalStateException.class, () -> s.scheduleRetry("PAY-3", "k3"));
    }
}
