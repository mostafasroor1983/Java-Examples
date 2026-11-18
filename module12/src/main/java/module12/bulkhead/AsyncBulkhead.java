package module12.bulkhead;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

/**
 * Bulkhead for async calls to one downstream (e.g. a card scheme gateway that allows 50 concurrent connections).
 *
 * <p>Async code has no natural back-pressure: {@code supplyAsync} happily starts 100,000 calls, the downstream
 * collapses, and every caller times out. A bulkhead caps IN-FLIGHT calls with a {@link Semaphore} and fails fast when
 * full ({@link BulkheadFullException}), so the caller can return 429/503 with Retry-After or shed low-priority traffic.
 * One slow dependency then cannot exhaust resources needed by the others (the name comes from ship compartments).
 *
 * <p>The permit is released in {@code whenComplete} on EVERY outcome: success, failure, cancellation, and a supplier
 * that throws before returning a future. A leaked permit slowly shrinks capacity to zero, an outage that builds up over days.
 */
public final class AsyncBulkhead {
    public static final class BulkheadFullException extends RuntimeException {
        public BulkheadFullException(String name) {
            super("bulkhead '" + name + "' full");
        }
    }

    private final String name;
    private final int maxConcurrent;
    private final Semaphore permits;
    private final LongAdder rejected = new LongAdder();

    public AsyncBulkhead(String name, int maxConcurrent) {
        if (maxConcurrent < 1) throw new IllegalArgumentException("maxConcurrent must be >= 1");
        this.name = Objects.requireNonNull(name);
        this.maxConcurrent = maxConcurrent;
        this.permits = new Semaphore(maxConcurrent);
    }

    public <T> CompletableFuture<T> call(Supplier<CompletableFuture<T>> work) {
        if (!permits.tryAcquire()) {
            rejected.increment();
            return CompletableFuture.failedFuture(new BulkheadFullException(name));
        }
        CompletableFuture<T> f;
        try {
            f = work.get();
        } catch (RuntimeException e) {
            permits.release();
            return CompletableFuture.failedFuture(e);
        }
        return f.whenComplete((v, e) -> permits.release());
    }

    public int inFlight() {
        return maxConcurrent - permits.availablePermits();
    }

    public long rejectedCount() {
        return rejected.sum();
    }
}
