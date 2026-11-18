package module12.retry;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.DoubleSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Non-blocking retry for async calls (PSP authorisation, FX quote, KYC lookup).
 *
 * <ul>
 *   <li><b>No thread sleeps</b> during backoff: the next attempt is scheduled with
 *   {@link CompletableFuture#delayedExecutor}, so 10,000 payments in backoff cost zero threads.</li>
 *   <li><b>Exponential backoff with full jitter</b>: {@code delay = random(0, min(cap, base x 2^(n-1)))}. Jitter spreads
 *   retries from many clients so a recovering provider is not hit by a synchronized thundering herd.</li>
 *   <li><b>Retry only what is retryable</b> (predicate): never retry a decline or a validation error.</li>
 *   <li><b>Overall deadline</b>: the caller's SLA bounds the total time, not just the attempt count.</li>
 * </ul>
 * Safety: retrying a money-moving call requires the SAME idempotency key on every attempt; the supplier must capture it.
 */
public final class AsyncRetry {
    private final int maxAttempts;
    private final Duration base;
    private final Duration cap;
    private final Duration deadline;
    private final Predicate<Throwable> retryable;
    private final DoubleSupplier random; // injectable for deterministic tests
    private final Executor executor;

    public AsyncRetry(int maxAttempts, Duration base, Duration cap, Duration deadline,
                      Predicate<Throwable> retryable, DoubleSupplier random, Executor executor) {
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be >= 1");
        this.maxAttempts = maxAttempts;
        this.base = Objects.requireNonNull(base);
        this.cap = Objects.requireNonNull(cap);
        this.deadline = Objects.requireNonNull(deadline);
        this.retryable = Objects.requireNonNull(retryable);
        this.random = Objects.requireNonNull(random);
        this.executor = Objects.requireNonNull(executor);
    }

    /** Delay before attempt {@code attempt + 1} (attempt is 1-based): full jitter over a capped exponential. */
    long backoffMillis(int attempt) {
        long exp = base.toMillis() * (1L << Math.min(attempt - 1, 30));
        long ceiling = Math.min(cap.toMillis(), exp);
        return (long) (random.getAsDouble() * ceiling);
    }

    public <T> CompletableFuture<T> execute(Supplier<CompletableFuture<T>> call) {
        CompletableFuture<T> result = new CompletableFuture<>();
        long deadlineNanos = System.nanoTime() + deadline.toNanos();
        attempt(call, 1, deadlineNanos, result);
        // overall deadline: completes the result even if an attempt hangs forever
        return result.orTimeout(deadline.toMillis(), TimeUnit.MILLISECONDS);
    }

    private <T> void attempt(Supplier<CompletableFuture<T>> call, int n, long deadlineNanos, CompletableFuture<T> result) {
        if (result.isDone()) return; // timed out or cancelled by the caller: stop retrying
        CompletableFuture<T> f;
        try {
            f = call.get();
        } catch (RuntimeException e) {
            f = CompletableFuture.failedFuture(e); // a supplier that throws synchronously is treated like a failed call
        }
        f.whenComplete((value, error) -> {
            if (error == null) {
                result.complete(value);
                return;
            }
            Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
            long delay = backoffMillis(n);
            boolean timeLeft = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(delay) < deadlineNanos;
            if (n >= maxAttempts || !retryable.test(cause) || !timeLeft) {
                result.completeExceptionally(cause);
                return;
            }
            Executor delayed = CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS, executor);
            delayed.execute(() -> attempt(call, n + 1, deadlineNanos, result));
        });
    }

    public static boolean isTimeout(Throwable t) {
        return t instanceof TimeoutException || (t instanceof CompletionException c && c.getCause() instanceof TimeoutException);
    }
}
