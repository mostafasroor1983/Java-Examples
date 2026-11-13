package module12.executors;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Which thread runs what:
 * - supplyAsync(s)            -> ForkJoinPool.commonPool() (CPU count - 1 threads, shared JVM-wide: never block in it)
 * - supplyAsync(s, executor)  -> your executor
 * - thenApply(f)              -> the thread that completed the previous stage (or the caller if already complete)
 * - thenApplyAsync(f)         -> common pool;  thenApplyAsync(f, executor) -> your executor
 */
public final class AsyncExecutors {
    private AsyncExecutors() {
    }

    public static ThreadFactory named(String prefix) {
        AtomicInteger n = new AtomicInteger();
        return r -> {
            Thread t = new Thread(r, prefix + "-" + n.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
    }

    public static ExecutorService ioPool(String name, int threads) {
        return Executors.newFixedThreadPool(threads, named(name));
    }

    /** Returns the names of the threads that ran each stage. */
    public static CompletableFuture<String[]> whoRunsWhat(Executor io, Executor cpu) {
        String[] names = new String[3];
        return CompletableFuture.supplyAsync(() -> { names[0] = Thread.currentThread().getName(); return 1; }, io)
                .thenApply(v -> { names[1] = Thread.currentThread().getName(); return v + 1; })           // same thread as stage 0 (usually)
                .thenApplyAsync(v -> { names[2] = Thread.currentThread().getName(); return v + 1; }, cpu) // cpu pool
                .thenApply(v -> names);
    }

    /** BAD: blocking inside the common pool starves every other parallel stream / async in the JVM. */
    public static CompletableFuture<String> blockingInCommonPoolBad(long millis) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return Thread.currentThread().getName();
        });
    }

    /** GOOD: blocking I/O goes to a dedicated, bounded pool (or virtual threads in Java 21 - module13). */
    public static CompletableFuture<String> blockingOnIoPool(long millis, Executor io) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return Thread.currentThread().getName();
        }, io);
    }
}
