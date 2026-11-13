package module12.errors;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * exceptionally (recover), handle (map both paths), whenComplete (observe), exceptionallyCompose (recover async).
 * Exceptions propagate down the chain wrapped in CompletionException; join() rethrows CompletionException, get() ExecutionException.
 */
public final class ErrorHandling {
    private ErrorHandling() {
    }

    public static CompletableFuture<BigDecimal> withFallbackRate(CompletableFuture<BigDecimal> live, BigDecimal fallback) {
        return live.exceptionally(ex -> fallback);
    }

    /** handle sees (value, throwable) and must produce a value either way. */
    public static CompletableFuture<String> describe(CompletableFuture<BigDecimal> f) {
        return f.handle((v, ex) -> ex == null ? "ok:" + v : "error:" + unwrap(ex).getClass().getSimpleName());
    }

    /** exceptionallyCompose: try a secondary provider asynchronously. */
    public static CompletableFuture<BigDecimal> primaryThenSecondary(CompletableFuture<BigDecimal> primary, java.util.function.Supplier<CompletableFuture<BigDecimal>> secondary) {
        return primary.exceptionallyCompose(ex -> secondary.get());
    }

    /** whenComplete does not change the outcome; it is for logging/metrics. */
    public static CompletableFuture<BigDecimal> observed(CompletableFuture<BigDecimal> f, java.util.List<String> log) {
        return f.whenComplete((v, ex) -> log.add(ex == null ? "success " + v : "failure " + unwrap(ex).getMessage()));
    }

    /** Stages after a failure are skipped: thenApply never runs, exceptionally at the end catches. */
    public static CompletableFuture<String> skippedStages(CompletableFuture<Integer> start, java.util.List<String> log) {
        return start
                .thenApply(v -> { log.add("stage1"); return v + 1; })
                .thenApply(v -> { log.add("stage2"); return "v=" + v; })
                .exceptionally(ex -> { log.add("recovered"); return "fallback"; });
    }

    /** Exceptions in the chain are wrapped; unwrap for classification. */
    public static Throwable unwrap(Throwable t) {
        while ((t instanceof CompletionException || t instanceof java.util.concurrent.ExecutionException) && t.getCause() != null) t = t.getCause();
        return t;
    }

    /** Create an already-failed future (Java 9+). */
    public static <T> CompletableFuture<T> failed(Throwable t) {
        return CompletableFuture.failedFuture(t);
    }
}
