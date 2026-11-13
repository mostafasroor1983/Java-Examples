package module12.timeouts;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * orTimeout (fail with TimeoutException), completeOnTimeout (default value), and manual cancellation.
 * Both use a shared single-threaded scheduler internally; do not run work on it.
 */
public final class Timeouts {
    private Timeouts() {
    }

    public static CompletableFuture<BigDecimal> failAfter(CompletableFuture<BigDecimal> f, Duration d) {
        return f.orTimeout(d.toMillis(), TimeUnit.MILLISECONDS);
    }

    public static CompletableFuture<BigDecimal> defaultAfter(CompletableFuture<BigDecimal> f, Duration d, BigDecimal fallback) {
        return f.completeOnTimeout(fallback, d.toMillis(), TimeUnit.MILLISECONDS);
    }

    /** Timeout + fallback + cancellation of the slow source so it does not keep running for nothing. */
    public static CompletableFuture<BigDecimal> withTimeoutAndCancel(CompletableFuture<BigDecimal> source, Duration d, BigDecimal fallback) {
        // orTimeout returns `this`, so apply it to a copy: otherwise the source is already completed and cancel() is a no-op.
        return source.copy().orTimeout(d.toMillis(), TimeUnit.MILLISECONDS)
                .exceptionally(ex -> {
                    if (ex instanceof TimeoutException || ex.getCause() instanceof TimeoutException) {
                        source.cancel(true);
                        return fallback;
                    }
                    throw new java.util.concurrent.CompletionException(ex);
                });
    }

    public static boolean isTimeout(Throwable t) {
        while (t != null) {
            if (t instanceof TimeoutException) return true;
            t = t.getCause();
        }
        return false;
    }
}
