package module18.ch11;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Item 81 - Prefer concurrency utilities to wait and notify.
 *
 * <p>{@code wait/notify} is low level and easy to get wrong (must loop on the condition, notify vs notifyAll, lost
 * wake-ups). Use {@code java.util.concurrent}: concurrent collections ({@link ConcurrentHashMap#putIfAbsent} for
 * atomic "intern"), synchronizers ({@link CountDownLatch}, Semaphore, CyclicBarrier, Phaser), BlockingQueue. If you
 * must maintain legacy wait/notify code: always wait in a while loop, and prefer notifyAll.
 */
public final class Item81ConcurrencyUtilities {
    private static final ConcurrentMap<String, String> CANONICAL_CURRENCIES = new ConcurrentHashMap<>();

    private Item81ConcurrencyUtilities() {
    }

    /** Canonical instance per currency code (String.intern-like), atomic without locks. */
    public static String canonical(String code) {
        String previous = CANONICAL_CURRENCIES.putIfAbsent(code, code);
        return previous == null ? code : previous;
    }

    /**
     * Times how long it takes N concurrent workers to settle, with all of them starting at the same instant.
     * Classic latch use: ready / start / done.
     */
    public static long timeConcurrentSettlement(Executor executor, int workers, Runnable settle) throws InterruptedException {
        CountDownLatch ready = new CountDownLatch(workers), start = new CountDownLatch(1), done = new CountDownLatch(workers);
        for (int i = 0; i < workers; i++) {
            executor.execute(() -> {
                ready.countDown();
                try {
                    start.await();
                    settle.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        if (!ready.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("workers did not start");
        long t0 = System.nanoTime();
        start.countDown();
        if (!done.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("workers did not finish");
        return System.nanoTime() - t0;
    }
}
