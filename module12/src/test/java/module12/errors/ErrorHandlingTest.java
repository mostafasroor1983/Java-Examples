package module12.errors;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class ErrorHandlingTest {

    @Test
    void recoverMapObserve() {
        CompletableFuture<BigDecimal> failed = ErrorHandling.failed(new IllegalStateException("venue down"));
        assertEquals(BigDecimal.ONE, ErrorHandling.withFallbackRate(failed, BigDecimal.ONE).join());
        assertEquals("error:IllegalStateException", ErrorHandling.describe(failed).join());
        assertEquals("ok:5", ErrorHandling.describe(CompletableFuture.completedFuture(new BigDecimal("5"))).join());
        assertEquals(BigDecimal.TEN, ErrorHandling.primaryThenSecondary(failed, () -> CompletableFuture.completedFuture(BigDecimal.TEN)).join());

        List<String> log = new ArrayList<>();
        assertThrows(CompletionException.class, () -> ErrorHandling.observed(failed, log).join());
        assertEquals(List.of("failure venue down"), log);
    }

    @Test
    void failureSkipsIntermediateStages() {
        List<String> log = new ArrayList<>();
        CompletableFuture<Integer> start = new CompletableFuture<>();
        CompletableFuture<String> chain = ErrorHandling.skippedStages(start, log);
        start.completeExceptionally(new RuntimeException("boom"));
        assertEquals("fallback", chain.join());
        assertEquals(List.of("recovered"), log);

        List<String> log2 = new ArrayList<>();
        assertEquals("v=2", ErrorHandling.skippedStages(CompletableFuture.completedFuture(1), log2).join());
        assertEquals(List.of("stage1", "stage2"), log2);
    }

    @Test
    void joinVsGetWrapping() {
        CompletableFuture<BigDecimal> failed = ErrorHandling.failed(new IllegalArgumentException("bad"));
        CompletionException ce = assertThrows(CompletionException.class, failed::join);
        assertInstanceOf(IllegalArgumentException.class, ce.getCause());
        ExecutionException ee = assertThrows(ExecutionException.class, failed::get);
        assertInstanceOf(IllegalArgumentException.class, ErrorHandling.unwrap(ee));
        // an exception thrown inside a stage is wrapped once, not twice
        CompletableFuture<Integer> thrower = CompletableFuture.completedFuture(1).thenApply(v -> { throw new IllegalStateException("x"); });
        assertInstanceOf(IllegalStateException.class, ErrorHandling.unwrap(assertThrows(CompletionException.class, thrower::join)));
    }
}
