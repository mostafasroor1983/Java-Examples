package module13.virtualthreads;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Virtual threads: cheap (a few hundred bytes of stack to start), scheduled by the JVM onto a small pool of
 * carrier threads. Blocking (sleep, I/O, locks) unmounts the virtual thread instead of wasting a carrier.
 * Rule: one virtual thread per task; never pool them; never cache them in ThreadLocals.
 */
public final class VirtualThreadDemos {
    private VirtualThreadDemos() {
    }

    /** Spawn N virtual threads that each "wait on a provider" (sleep) - impossible with N platform threads. */
    public static int fanOutBlockingCalls(int n, Duration simulatedLatency) throws InterruptedException {
        AtomicInteger completed = new AtomicInteger();
        try (ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < n; i++) {
                vt.submit(() -> {
                    try {
                        Thread.sleep(simulatedLatency);
                        completed.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        } // close() waits for all tasks
        return completed.get();
    }

    /** Same code shape as a platform-thread pool, but every task gets its own thread. */
    public static List<BigDecimal> fetchBalances(List<String> accountIds, java.util.function.Function<String, BigDecimal> blockingLookup) throws Exception {
        try (ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<BigDecimal>> futures = new ArrayList<>();
            for (String id : accountIds) futures.add(vt.submit((Callable<BigDecimal>) () -> blockingLookup.apply(id)));
            List<BigDecimal> out = new ArrayList<>();
            for (Future<BigDecimal> f : futures) out.add(f.get());
            return out;
        }
    }

    public static boolean isVirtual(Thread t) {
        return t.isVirtual();
    }

    /** Thread.Builder API: named virtual threads, started or unstarted. */
    public static String nameOfVirtualThread() throws InterruptedException {
        String[] name = new String[1];
        Thread t = Thread.ofVirtual().name("settlement-vt-", 1).start(() -> name[0] = Thread.currentThread().getName());
        t.join();
        return name[0];
    }

    /** Carrier threads: many virtual threads run on few carriers. Returns distinct carrier-ish count via a crude heuristic. */
    public static long distinctCarriersObserved(int virtualThreads) throws InterruptedException {
        java.util.Set<String> carriers = java.util.concurrent.ConcurrentHashMap.newKeySet();
        try (ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < virtualThreads; i++) {
                vt.submit(() -> {
                    // Thread.toString() of a virtual thread includes the carrier: VirtualThread[#N]/runnable@ForkJoinPool-1-worker-3
                    String s = Thread.currentThread().toString();
                    int at = s.indexOf('@');
                    if (at > 0) carriers.add(s.substring(at + 1));
                });
            }
        }
        return carriers.size();
    }

    /** Memory/scheduling cost comparison helper: time to start-and-join N threads of each kind. */
    public static long nanosToRun(int n, boolean virtual) throws InterruptedException {
        AtomicLong sink = new AtomicLong();
        long start = System.nanoTime();
        Thread.Builder b = virtual ? Thread.ofVirtual() : Thread.ofPlatform();
        List<Thread> threads = new ArrayList<>(n);
        for (int i = 0; i < n; i++) threads.add(b.start(sink::incrementAndGet));
        for (Thread t : threads) t.join();
        return System.nanoTime() - start;
    }
}
