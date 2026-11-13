package module09.executors;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Executors, Callable/Future, invokeAll, graceful shutdown, exceptions inside tasks. */
public final class ExecutorDemos {
    private ExecutorDemos() {
    }

    /** Fan-out fee calculations over a fixed pool and collect results in submission order. */
    public static List<BigDecimal> calculateFees(List<BigDecimal> amounts, BigDecimal rate, int threads) throws InterruptedException, ExecutionException {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Callable<BigDecimal>> tasks = new ArrayList<>();
            for (BigDecimal a : amounts) tasks.add(() -> a.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP));
            List<Future<BigDecimal>> futures = pool.invokeAll(tasks);
            List<BigDecimal> out = new ArrayList<>();
            for (Future<BigDecimal> f : futures) out.add(f.get());
            return out;
        } finally {
            shutdownGracefully(pool, 5, TimeUnit.SECONDS);
        }
    }

    /** A task exception surfaces as ExecutionException on get(); the pool thread survives. */
    public static Throwable causeOfFailingTask(ExecutorService pool) throws InterruptedException {
        Future<?> f = pool.submit(() -> { throw new IllegalStateException("provider down"); });
        try {
            f.get();
            return null;
        } catch (ExecutionException e) {
            return e.getCause();
        }
    }

    /** BAD: execute() with a throwing Runnable - the exception goes to the thread's handler, Future never sees it. */
    public static void fireAndForgetBad(ExecutorService pool, Runnable r) {
        pool.execute(r);
    }

    /** get with timeout + cancel: never block forever on a dependency. */
    public static String withTimeout(ExecutorService pool, Callable<String> slow, long timeoutMs) throws InterruptedException, ExecutionException {
        Future<String> f = pool.submit(slow);
        try {
            return f.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            f.cancel(true);
            return "TIMEOUT";
        }
    }

    /** The standard shutdown dance. */
    public static boolean shutdownGracefully(ExecutorService pool, long timeout, TimeUnit unit) throws InterruptedException {
        pool.shutdown(); // stop accepting, finish queued
        if (!pool.awaitTermination(timeout, unit)) {
            pool.shutdownNow(); // interrupt running tasks
            return pool.awaitTermination(timeout, unit);
        }
        return true;
    }
}
