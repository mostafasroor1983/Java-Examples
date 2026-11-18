package module13.throttling;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Month-end statement generation: one task per account (10k+ accounts), each blocking on a DB query.
 *
 * <p>Virtual threads (Java 21 final; preview in 19 and 20 via JEP 425/436) make "one thread per account" cheap: blocked
 * tasks unmount from their carrier. But the DATABASE does not get cheaper: the connection pool has, say, 20 connections.
 * 10,000 virtual threads hammering a 20-connection pool means 9,980 threads waiting inside the pool with opaque
 * timeouts. The fix is explicit admission control: a {@link Semaphore} sized to the downstream capacity.
 *
 * <p>Behaviour:
 * <ul>
 *   <li>At most {@code maxConcurrent} tasks run the blocking call at once (measured via {@link #maxObservedInFlight}).</li>
 *   <li>A task that cannot get a permit within {@code acquireTimeout} fails fast with {@code ADMISSION_TIMEOUT}
 *   instead of queueing forever (load shedding for interactive paths; batch jobs use a generous timeout).</li>
 *   <li>One account failing does not abort the batch: per-account outcomes are returned for a rerun of the failures only.</li>
 * </ul>
 * Do NOT use a fixed pool of virtual threads to limit concurrency: pooling virtual threads defeats their purpose.
 */
public final class BoundedVirtualFanOut {
    public sealed interface Outcome<R> {
        record Ok<R>(R value) implements Outcome<R> { }

        record Failed<R>(String reason) implements Outcome<R> { }
    }

    private final Semaphore permits;
    private final Duration acquireTimeout;
    private final AtomicInteger inFlight = new AtomicInteger();
    private final AtomicInteger maxInFlight = new AtomicInteger();

    public BoundedVirtualFanOut(int maxConcurrent, Duration acquireTimeout) {
        if (maxConcurrent < 1) throw new IllegalArgumentException("maxConcurrent must be >= 1");
        this.permits = new Semaphore(maxConcurrent, true); // fair: FIFO admission, no starvation of early accounts
        this.acquireTimeout = Objects.requireNonNull(acquireTimeout);
    }

    /** Runs {@code work} for every key on its own virtual thread; returns outcomes in input order. */
    public <K, R> Map<K, Outcome<R>> run(List<K> keys, Function<K, R> work) throws InterruptedException {
        Map<K, Future<Outcome<R>>> futures = new LinkedHashMap<>();
        try (ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor()) {
            for (K key : keys) futures.put(key, vt.submit(() -> guarded(key, work)));
        } // close() waits for every task: no orphaned threads after run() returns
        Map<K, Outcome<R>> out = new LinkedHashMap<>();
        for (var e : futures.entrySet()) {
            try {
                out.put(e.getKey(), e.getValue().get());
            } catch (java.util.concurrent.ExecutionException ex) {
                out.put(e.getKey(), new Outcome.Failed<>("UNEXPECTED: " + ex.getCause().getClass().getSimpleName()));
            }
        }
        return out;
    }

    private <K, R> Outcome<R> guarded(K key, Function<K, R> work) throws InterruptedException {
        if (!permits.tryAcquire(acquireTimeout.toNanos(), TimeUnit.NANOSECONDS)) {
            return new Outcome.Failed<>("ADMISSION_TIMEOUT");
        }
        try {
            maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
            return new Outcome.Ok<>(work.apply(key));
        } catch (RuntimeException e) {
            return new Outcome.Failed<>(e.getClass().getSimpleName()); // no message: may contain customer data
        } finally {
            inFlight.decrementAndGet();
            permits.release();
        }
    }

    public int maxObservedInFlight() {
        return maxInFlight.get();
    }

    public static <K, R> List<K> failedKeys(Map<K, Outcome<R>> outcomes) {
        List<K> failed = new ArrayList<>();
        outcomes.forEach((k, o) -> {
            if (o instanceof Outcome.Failed<R>) failed.add(k);
        });
        return failed;
    }
}
