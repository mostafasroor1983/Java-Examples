package module15.p12retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * P12 - Retry with exponential backoff, jitter and a retryable predicate. Sleeping is injected so tests run instantly.
 */
public final class P12RetryWithBackoff {
    public interface Sleeper {
        void sleep(Duration d) throws InterruptedException;
    }

    public static final class RetriesExhaustedException extends RuntimeException {
        public RetriesExhaustedException(int attempts, Throwable last) {
            super("gave up after " + attempts + " attempts", last);
        }
    }

    private final int maxAttempts;
    private final Duration initialDelay;
    private final Duration maxDelay;
    private final double multiplier;
    private final Predicate<Throwable> retryable;
    private final Sleeper sleeper;
    private final java.util.Random jitter;

    public P12RetryWithBackoff(int maxAttempts, Duration initialDelay, Duration maxDelay, double multiplier,
                               Predicate<Throwable> retryable, Sleeper sleeper, long jitterSeed) {
        if (maxAttempts <= 0 || multiplier < 1.0) throw new IllegalArgumentException("bad retry config");
        this.maxAttempts = maxAttempts;
        this.initialDelay = Objects.requireNonNull(initialDelay);
        this.maxDelay = Objects.requireNonNull(maxDelay);
        this.multiplier = multiplier;
        this.retryable = Objects.requireNonNull(retryable);
        this.sleeper = Objects.requireNonNull(sleeper);
        this.jitter = new java.util.Random(jitterSeed);
    }

    /** Delay for attempt n (1-based) before jitter: min(maxDelay, initial * multiplier^(n-1)). */
    public Duration baseDelay(int attempt) {
        double millis = initialDelay.toMillis() * Math.pow(multiplier, attempt - 1);
        return Duration.ofMillis((long) Math.min(maxDelay.toMillis(), millis));
    }

    public <T> T execute(Supplier<T> action) throws InterruptedException {
        List<Throwable> failures = new ArrayList<>();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (RuntimeException e) {
                failures.add(e);
                if (!retryable.test(e) || attempt == maxAttempts) {
                    RetriesExhaustedException ex = new RetriesExhaustedException(attempt, e);
                    for (int i = 0; i < failures.size() - 1; i++) ex.addSuppressed(failures.get(i));
                    throw ex;
                }
                Duration base = baseDelay(attempt);
                long jittered = base.toMillis() == 0 ? 0 : (long) (base.toMillis() * (0.5 + jitter.nextDouble() * 0.5)); // 50%..100% of base
                sleeper.sleep(Duration.ofMillis(jittered));
            }
        }
        throw new IllegalStateException("unreachable");
    }
}
