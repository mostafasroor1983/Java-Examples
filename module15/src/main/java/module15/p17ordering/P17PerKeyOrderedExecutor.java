package module15.p17ordering;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * P17 - Per-key ordered execution: events for the SAME account are applied strictly in submission order, events for
 * DIFFERENT accounts run in parallel.
 *
 * <p>Why: "deposit 100, then withdraw 80" applied in reverse order is rejected for insufficient funds. A plain thread pool
 * gives no ordering. A single thread gives ordering but no parallelism. Kafka solves it with partition-by-key (one consumer
 * thread per partition); this class is the in-process equivalent.
 *
 * <p>Mechanism: each key maps to the TAIL of its chain of {@link CompletableFuture}s; a new task is chained after the
 * tail ({@code thenRunAsync}). {@code compute} makes "read tail + append" atomic. When a task finishes and it is still the
 * tail, the key is removed, so idle accounts do not leak memory. A failing task does not break the chain for later
 * events: its failure is returned to the submitter, and the next task still runs.
 */
public final class P17PerKeyOrderedExecutor<K> {
    private final ConcurrentHashMap<K, CompletableFuture<Void>> tails = new ConcurrentHashMap<>();
    private final Executor executor;

    public P17PerKeyOrderedExecutor(Executor executor) {
        this.executor = Objects.requireNonNull(executor);
    }

    public CompletableFuture<Void> submit(K key, Runnable task) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(task);
        AtomicReference<CompletableFuture<Void>> mine = new AtomicReference<>(); // typed holder for the lambda
        tails.compute(key, (k, tail) -> {
            CompletableFuture<Void> base = tail == null ? CompletableFuture.completedFuture(null)
                    : tail.exceptionally(t -> null); // isolate failures: the next event still runs
            mine.set(base.thenRunAsync(task, executor));
            return mine.get();
        });
        CompletableFuture<Void> f = mine.get();
        f.whenComplete((v, t) -> tails.remove(key, f)); // only removes if no newer task was chained
        return f;
    }

    /** Number of keys with pending work (for metrics / leak tests). */
    public int activeKeys() {
        return tails.size();
    }
}
