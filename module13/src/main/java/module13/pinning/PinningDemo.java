package module13.pinning;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Pinning (JDK 21): a virtual thread that blocks inside a `synchronized` block (or a native frame) cannot unmount;
 * it pins its carrier. With few carriers and many pinned threads, throughput collapses.
 * Fix: use ReentrantLock (unmounts fine) or keep synchronized sections short and non-blocking.
 * (JDK 24 removes most pinning for synchronized, JEP 491 - but interviewers still ask.)
 *
 * The demo bounds the carrier pool via jdk.virtualThreadScheduler.parallelism only when the JVM allows;
 * tests therefore assert behaviour (correctness and completion), not timing.
 */
public final class PinningDemo {
    private PinningDemo() {
    }

    /** BAD: sleeping while holding a monitor pins the carrier for the whole sleep. */
    public static int blockInsideSynchronized(int tasks, long sleepMillis) throws InterruptedException {
        Object lock = new Object();
        AtomicInteger done = new AtomicInteger();
        try (ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < tasks; i++) {
                vt.submit(() -> {
                    synchronized (lock) {
                        try {
                            Thread.sleep(sleepMillis); // pinned
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    done.incrementAndGet();
                });
            }
        }
        return done.get();
    }

    /** GOOD: ReentrantLock lets the virtual thread unmount while waiting for the lock and while sleeping outside it. */
    public static int blockWithReentrantLock(int tasks, long sleepMillis) throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();
        AtomicInteger done = new AtomicInteger();
        try (ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < tasks; i++) {
                vt.submit(() -> {
                    lock.lock();
                    try {
                        done.incrementAndGet(); // short critical section
                    } finally {
                        lock.unlock();
                    }
                    try {
                        Thread.sleep(sleepMillis); // blocking outside any monitor: unmounts
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        }
        return done.get();
    }

    /** Detect whether the current thread is a virtual thread (useful for guard rails in libraries). */
    public static boolean runningOnVirtualThread() {
        return Thread.currentThread().isVirtual();
    }

    /** Latch-based rendezvous to prove many virtual threads can be parked simultaneously (not pinned). */
    public static boolean parkManyAtOnce(int n) throws InterruptedException {
        CountDownLatch allParked = new CountDownLatch(n);
        CountDownLatch release = new CountDownLatch(1);
        try (ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < n; i++) {
                vt.submit(() -> {
                    allParked.countDown();
                    try {
                        release.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            boolean ok = allParked.await(10, TimeUnit.SECONDS);
            release.countDown();
            return ok;
        }
    }
}
