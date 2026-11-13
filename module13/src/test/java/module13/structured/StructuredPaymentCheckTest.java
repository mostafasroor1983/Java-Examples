package module13.structured;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class StructuredPaymentCheckTest {

    private static StructuredPaymentCheck.Ports ports(String balance, int fraud, String limit, Runnable onFraud) {
        return new StructuredPaymentCheck.Ports() {
            public StructuredPaymentCheck.Balance balance(String id) {
                return new StructuredPaymentCheck.Balance(id, new BigDecimal(balance));
            }

            public StructuredPaymentCheck.Fraud fraud(String id, BigDecimal amount) throws Exception {
                onFraud.run();
                return new StructuredPaymentCheck.Fraud(fraud);
            }

            public StructuredPaymentCheck.Limits limits(String id) {
                return new StructuredPaymentCheck.Limits(new BigDecimal(limit));
            }
        };
    }

    @Test
    void approvesAndDeclinesDeterministically() throws Exception {
        var ok = new StructuredPaymentCheck(ports("100", 10, "500", () -> { }));
        assertTrue(ok.check("A", new BigDecimal("50"), Duration.ofSeconds(5)).approved());
        assertEquals("insufficient funds", new StructuredPaymentCheck(ports("10", 10, "500", () -> { })).check("A", new BigDecimal("50"), Duration.ofSeconds(5)).reason());
        assertEquals("fraud score 99", new StructuredPaymentCheck(ports("100", 99, "500", () -> { })).check("A", new BigDecimal("50"), Duration.ofSeconds(5)).reason());
        assertEquals("daily limit", new StructuredPaymentCheck(ports("100", 10, "20", () -> { })).check("A", new BigDecimal("50"), Duration.ofSeconds(5)).reason());
    }

    @Test
    void oneFailureCancelsSiblingsAndPropagates() throws Exception {
        CountDownLatch fraudStarted = new CountDownLatch(1);
        AtomicBoolean fraudInterrupted = new AtomicBoolean();
        var ports = new StructuredPaymentCheck.Ports() {
            public StructuredPaymentCheck.Balance balance(String id) {
                throw new IllegalStateException("ledger unavailable");
            }

            public StructuredPaymentCheck.Fraud fraud(String id, BigDecimal amount) {
                fraudStarted.countDown();
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException e) {
                    fraudInterrupted.set(true);
                }
                return new StructuredPaymentCheck.Fraud(0);
            }

            public StructuredPaymentCheck.Limits limits(String id) {
                return new StructuredPaymentCheck.Limits(BigDecimal.TEN);
            }
        };
        long start = System.nanoTime();
        ExecutionException e = assertThrows(ExecutionException.class,
                () -> new StructuredPaymentCheck(ports).check("A", BigDecimal.ONE, Duration.ofSeconds(30)));
        assertInstanceOf(IllegalStateException.class, e.getCause());
        assertTrue(fraudStarted.await(1, TimeUnit.SECONDS));
        assertTrue((System.nanoTime() - start) / 1_000_000 < 5_000, "did not wait for the 10s fraud call");
        assertTrue(fraudInterrupted.get(), "sibling subtask was cancelled");
    }

    @Test
    void deadlineProducesTimeout() {
        var slow = ports("100", 10, "500", () -> {
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        });
        assertThrows(TimeoutException.class, () -> new StructuredPaymentCheck(slow).check("A", BigDecimal.ONE, Duration.ofMillis(100)));
    }

    @Test
    void fastestWinsAndCollectAllKeepsPartialResults() throws Exception {
        Callable<BigDecimal> fast = () -> new BigDecimal("1.25");
        Callable<BigDecimal> slow = () -> {
            Thread.sleep(5_000);
            return new BigDecimal("1.30");
        };
        Callable<BigDecimal> broken = () -> {
            throw new IllegalStateException("down");
        };
        assertEquals(new BigDecimal("1.25"), StructuredPaymentCheck.fastestRate(List.of(slow, fast, broken), Duration.ofSeconds(5)));
        assertThrows(ExecutionException.class, () -> StructuredPaymentCheck.fastestRate(List.of(broken), Duration.ofSeconds(1)));

        List<String> all = StructuredPaymentCheck.collectAll(List.of(() -> "ok1", () -> { throw new RuntimeException("bad"); }, () -> "ok2"));
        assertEquals(List.of("ok1", "FAILED:bad", "ok2"), all);
    }
}
