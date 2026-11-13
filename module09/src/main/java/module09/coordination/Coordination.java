package module09.coordination;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** CountDownLatch (one-shot gate), CyclicBarrier (reusable rendezvous), Semaphore (permits). */
public final class Coordination {
    private Coordination() {
    }

    /** Start N workers simultaneously (start latch) and wait until all finish (done latch). */
    public static int startTogetherAndAwait(int workers, Runnable body) throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(workers);
        AtomicInteger completed = new AtomicInteger();
        for (int i = 0; i < workers; i++) {
            new Thread(() -> {
                try {
                    start.await();
                    body.run();
                    completed.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        start.countDown();
        if (!done.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("workers did not finish");
        return completed.get();
    }

    /**
     * Batch settlement in phases: every worker must finish phase k before anyone starts phase k+1.
     * The barrier action runs once per phase when the last thread arrives.
     */
    public static List<String> phasedSettlement(int workers, int phases) throws InterruptedException {
        List<String> log = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger phase = new AtomicInteger();
        CyclicBarrier barrier = new CyclicBarrier(workers, () -> log.add("phase " + phase.incrementAndGet() + " complete"));
        List<Thread> threads = new ArrayList<>();
        for (int w = 0; w < workers; w++) {
            int id = w;
            Thread t = new Thread(() -> {
                try {
                    for (int p = 0; p < phases; p++) {
                        log.add("w" + id + " p" + p);
                        barrier.await();
                    }
                } catch (InterruptedException | BrokenBarrierException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) t.join();
        return new ArrayList<>(log);
    }

    /** Limit concurrent calls to a provider to `permits`; returns the max concurrency observed. */
    public static int boundedConcurrency(int permits, int callers) throws InterruptedException {
        Semaphore semaphore = new Semaphore(permits);
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger maxObserved = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(callers);
        CyclicBarrier inside = new CyclicBarrier(Math.min(permits, callers)); // force the permitted callers to overlap
        for (int i = 0; i < callers; i++) {
            new Thread(() -> {
                try {
                    semaphore.acquire();
                    try {
                        int now = inFlight.incrementAndGet();
                        maxObserved.accumulateAndGet(now, Math::max);
                        try {
                            inside.await(1, TimeUnit.SECONDS);
                        } catch (BrokenBarrierException | java.util.concurrent.TimeoutException ignored) {
                            // later waves may not fill the barrier; that is fine
                        }
                        inFlight.decrementAndGet();
                    } finally {
                        semaphore.release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        if (!done.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("callers did not finish");
        return maxObserved.get();
    }

    /** tryAcquire for non-blocking admission control (e.g. shed load instead of queueing). */
    public static boolean admit(Semaphore s) {
        return s.tryAcquire();
    }
}
