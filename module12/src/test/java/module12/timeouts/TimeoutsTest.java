package module12.timeouts;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

class TimeoutsTest {

    @Test
    void orTimeoutFailsWithTimeoutException() {
        CompletableFuture<BigDecimal> never = new CompletableFuture<>();
        CompletionException e = assertThrows(CompletionException.class, () -> Timeouts.failAfter(never, Duration.ofMillis(50)).join());
        assertTrue(Timeouts.isTimeout(e));
    }

    @Test
    void completeOnTimeoutGivesDefault() {
        CompletableFuture<BigDecimal> never = new CompletableFuture<>();
        assertEquals(BigDecimal.ONE, Timeouts.defaultAfter(never, Duration.ofMillis(50), BigDecimal.ONE).join());
        assertEquals(BigDecimal.TEN, Timeouts.defaultAfter(CompletableFuture.completedFuture(BigDecimal.TEN), Duration.ofSeconds(5), BigDecimal.ONE).join());
    }

    @Test
    void timeoutCancelsSourceAndNonTimeoutErrorsPropagate() {
        CompletableFuture<BigDecimal> slow = new CompletableFuture<>();
        assertEquals(BigDecimal.ZERO, Timeouts.withTimeoutAndCancel(slow, Duration.ofMillis(50), BigDecimal.ZERO).join());
        assertTrue(slow.isCancelled());

        CompletableFuture<BigDecimal> broken = CompletableFuture.failedFuture(new IllegalStateException("venue down"));
        CompletionException e = assertThrows(CompletionException.class,
                () -> Timeouts.withTimeoutAndCancel(broken, Duration.ofSeconds(1), BigDecimal.ZERO).join());
        assertFalse(Timeouts.isTimeout(e));
    }
}
