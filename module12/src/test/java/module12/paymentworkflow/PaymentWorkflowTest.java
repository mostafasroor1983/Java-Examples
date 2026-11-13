package module12.paymentworkflow;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PaymentWorkflowTest {
    private final ExecutorService pool = Executors.newFixedThreadPool(4);
    private final List<String> log = new ArrayList<>();

    private PaymentWorkflow.Ports ports(CompletableFuture<Integer> fraud, AtomicInteger debitCalls, int failDebitTimes, boolean notifyFails, CountDownLatch notified) {
        return new PaymentWorkflow.Ports() {
            public CompletableFuture<Integer> fraudScore(PaymentWorkflow.Payment p) {
                return fraud;
            }

            public CompletableFuture<Boolean> debit(PaymentWorkflow.Payment p) {
                int n = debitCalls.incrementAndGet();
                if (n <= failDebitTimes) return CompletableFuture.failedFuture(new IllegalStateException("ledger busy"));
                return CompletableFuture.completedFuture(p.amount().compareTo(new BigDecimal("500")) <= 0);
            }

            public CompletableFuture<Void> notify(PaymentWorkflow.Payment p, PaymentWorkflow.Status s) {
                notified.countDown();
                return notifyFails ? CompletableFuture.failedFuture(new RuntimeException("smtp down")) : CompletableFuture.completedFuture(null);
            }
        };
    }

    private PaymentWorkflow.Payment pay(String amount) {
        return new PaymentWorkflow.Payment("p1", "A", new BigDecimal(amount));
    }

    @Test
    void approvedWithRetryAndNotificationFailureIgnored() throws Exception {
        AtomicInteger debits = new AtomicInteger();
        CountDownLatch notified = new CountDownLatch(1);
        var wf = new PaymentWorkflow(ports(CompletableFuture.completedFuture(10), debits, 2, true, notified), pool, Duration.ofSeconds(1), log);
        var out = wf.process(pay("100")).join();
        assertEquals(PaymentWorkflow.Status.APPROVED, out.status());
        assertEquals(3, debits.get(), "two failures then success");
        assertTrue(notified.await(2, TimeUnit.SECONDS));
        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.SECONDS);
        synchronized (log) {
            assertTrue(log.stream().anyMatch(l -> l.startsWith("notify failed")), log.toString());
        }
    }

    @Test
    void declinedByFraudOrFunds() {
        AtomicInteger debits = new AtomicInteger();
        var fraud = new PaymentWorkflow(ports(CompletableFuture.completedFuture(95), debits, 0, false, new CountDownLatch(1)), pool, Duration.ofSeconds(1), log);
        assertEquals(PaymentWorkflow.Status.DECLINED, fraud.process(pay("10")).join().status());
        assertEquals(0, debits.get(), "no debit when fraud declines");
        var funds = new PaymentWorkflow(ports(CompletableFuture.completedFuture(5), debits, 0, false, new CountDownLatch(1)), pool, Duration.ofSeconds(1), log);
        var out = funds.process(pay("5000")).join();
        assertEquals(PaymentWorkflow.Status.DECLINED, out.status());
        assertEquals("insufficient funds", out.detail());
        pool.shutdown();
    }

    @Test
    void fraudTimeoutRoutesToManualReview() {
        CompletableFuture<Integer> hanging = new CompletableFuture<>();
        AtomicInteger debits = new AtomicInteger();
        var wf = new PaymentWorkflow(ports(hanging, debits, 0, false, new CountDownLatch(1)), pool, Duration.ofMillis(50), log);
        var out = wf.process(pay("10")).join();
        assertEquals(PaymentWorkflow.Status.MANUAL_REVIEW, out.status());
        assertEquals(0, debits.get());
        synchronized (log) {
            assertTrue(log.stream().anyMatch(l -> l.contains("TimeoutException")), log.toString());
        }
        pool.shutdown();
    }

    @Test
    void retryGivesUpAfterAttempts() {
        AtomicInteger calls = new AtomicInteger();
        var f = PaymentWorkflow.retry(() -> {
            calls.incrementAndGet();
            return CompletableFuture.<String>failedFuture(new IllegalStateException("always"));
        }, 3);
        assertThrows(java.util.concurrent.CompletionException.class, f::join);
        assertEquals(3, calls.get());
        assertThrows(IllegalArgumentException.class, () -> pay("0"));
    }
}
