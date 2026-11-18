package module18.ch11;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * Item 80 - Prefer executors, tasks, and streams to threads.
 *
 * <p>Separate the unit of work (Runnable/Callable task) from the execution mechanism (executor service). Executors
 * give pooling, futures with results and exceptions, timeouts, and orderly shutdown. Choose the executor explicitly:
 * a bounded pool for CPU work; for blocking I/O on <b>Java 21</b>, {@code newVirtualThreadPerTaskExecutor()}
 * (virtual threads: final in 21, preview in 19/20). ExecutorService is AutoCloseable since Java 19, so
 * try-with-resources waits for completion. The fork-join pool and parallel streams serve divide-and-conquer CPU work.
 */
public final class Item80Executors {
    private Item80Executors() {
    }

    /** Java 17 style: explicit fixed pool, explicit shutdown. */
    public static List<BigDecimal> fetchBalancesPool(List<String> accounts, Function<String, BigDecimal> lookup)
            throws InterruptedException, ExecutionException {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Callable<BigDecimal>> tasks = new ArrayList<>();
            for (String a : accounts) tasks.add(() -> lookup.apply(a));
            List<BigDecimal> out = new ArrayList<>();
            for (Future<BigDecimal> f : pool.invokeAll(tasks)) out.add(f.get());
            return out;
        } finally {
            pool.shutdown();
        }
    }

    /** Java 21 style: one virtual thread per blocking lookup; close() waits for all tasks. */
    public static List<BigDecimal> fetchBalancesVirtual(List<String> accounts, Function<String, BigDecimal> lookup)
            throws InterruptedException, ExecutionException {
        try (ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<BigDecimal>> futures = new ArrayList<>();
            for (String a : accounts) futures.add(vt.submit(() -> lookup.apply(a)));
            List<BigDecimal> out = new ArrayList<>();
            for (Future<BigDecimal> f : futures) out.add(f.get());
            return out;
        }
    }
}
