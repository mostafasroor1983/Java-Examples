package module08.circuitbreaker;

import module08.hierarchy.PaymentException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Retry with bounded attempts; only retries exceptions the domain marks retryable. Pairs with the breaker. */
public final class RetryPolicy {
    private final int maxAttempts;

    public RetryPolicy(int maxAttempts) {
        if (maxAttempts <= 0) throw new IllegalArgumentException("maxAttempts must be positive");
        this.maxAttempts = maxAttempts;
    }

    public <T> T execute(Supplier<T> action) {
        List<Throwable> failures = new ArrayList<>();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (PaymentException e) {
                failures.add(e);
                if (!e.retryable() || attempt == maxAttempts) {
                    for (int i = 0; i < failures.size() - 1; i++) e.addSuppressed(failures.get(i));
                    throw e;
                }
            }
        }
        throw new IllegalStateException("unreachable");
    }
}
