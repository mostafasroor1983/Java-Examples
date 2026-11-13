package module11.pools;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Production thread pools: explicit ThreadPoolExecutor with a bounded queue, named threads, and a
 * rejection policy you chose on purpose. Executors.newFixedThreadPool hides an unbounded queue.
 */
public final class PoolFactory {
    private PoolFactory() {
    }

    /** Named daemon threads with an uncaught handler - log lines you can grep. */
    public static ThreadFactory namedThreads(String prefix) {
        AtomicInteger seq = new AtomicInteger();
        return r -> {
            Thread t = new Thread(r, prefix + "-" + seq.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
    }

    /**
     * ThreadPoolExecutor semantics: new task -> if fewer than core threads, start one; else enqueue;
     * else if fewer than max threads, start one; else reject. Note: max threads only kick in when the queue is FULL.
     */
    public static ThreadPoolExecutor bounded(String name, int core, int max, int queueCapacity, RejectedExecutionHandler onReject) {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(core, max, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity), namedThreads(name), onReject);
        pool.allowCoreThreadTimeOut(false);
        return pool;
    }

    /** CPU-bound sizing: cores (+1 to cover a page fault/stall). */
    public static int cpuBoundSize(int cores) {
        return cores + 1;
    }

    /**
     * I/O-bound sizing (Little's law / Goetz): threads = cores * targetUtilisation * (1 + wait/compute).
     * E.g. 8 cores, 100% util, 90ms waiting on a provider per 10ms compute -> 8 * 1 * 10 = 80 threads.
     */
    public static int ioBoundSize(int cores, double targetUtilisation, double waitMillis, double computeMillis) {
        if (cores <= 0 || targetUtilisation <= 0 || targetUtilisation > 1 || computeMillis <= 0 || waitMillis < 0) {
            throw new IllegalArgumentException("invalid sizing inputs");
        }
        return (int) Math.ceil(cores * targetUtilisation * (1 + waitMillis / computeMillis));
    }

    /** Graceful shutdown with a hard deadline. Returns true if everything terminated. */
    public static boolean shutdown(ExecutorService pool, long timeout, TimeUnit unit) throws InterruptedException {
        pool.shutdown();
        if (pool.awaitTermination(timeout, unit)) return true;
        pool.shutdownNow();
        return pool.awaitTermination(timeout, unit);
    }
}
