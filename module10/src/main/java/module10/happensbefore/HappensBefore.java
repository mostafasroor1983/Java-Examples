package module10.happensbefore;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Happens-before edges you can rely on:
 *  - monitor unlock -> subsequent lock of the same monitor
 *  - volatile write -> subsequent volatile read of the same field
 *  - Thread.start() -> actions in the started thread
 *  - actions in a thread -> Thread.join() returning
 *  - CountDownLatch.countDown() -> await() returning (and all j.u.c. synchronizers)
 *  - executor submit -> task start; Future.get -> after task completion
 *
 * Each method publishes a plain (non-volatile) int through one of these edges and returns what the reader saw.
 */
public final class HappensBefore {
    private HappensBefore() {
    }

    private static final class Box {
        int plainValue; // deliberately plain: visibility comes from the edge, not the field
    }

    /** start(): writes before start() are visible inside the new thread. */
    public static int viaThreadStart() throws InterruptedException {
        Box box = new Box();
        int[] seen = new int[1];
        box.plainValue = 42;
        Thread t = new Thread(() -> seen[0] = box.plainValue);
        t.start();
        t.join();
        return seen[0];
    }

    /** join(): writes in the thread are visible after join() returns. */
    public static int viaThreadJoin() throws InterruptedException {
        Box box = new Box();
        Thread t = new Thread(() -> box.plainValue = 7);
        t.start();
        t.join();
        return box.plainValue;
    }

    /** volatile: write to plain field, then volatile write; reader does volatile read first, then plain read. */
    public static int viaVolatile() throws InterruptedException {
        Box box = new Box();
        final boolean[] holder = new boolean[1];
        var flag = new java.util.concurrent.atomic.AtomicBoolean(false); // atomic = volatile semantics
        Thread writer = new Thread(() -> {
            box.plainValue = 99;
            flag.set(true);
        });
        int[] seen = new int[1];
        Thread reader = new Thread(() -> {
            while (!flag.get()) Thread.onSpinWait();
            seen[0] = box.plainValue; // guaranteed 99 because of the volatile edge
        });
        reader.start();
        writer.start();
        writer.join();
        reader.join();
        return seen[0];
    }

    /** Latch: countDown() happens-before await() returning. */
    public static int viaLatch() throws InterruptedException {
        Box box = new Box();
        CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            box.plainValue = 1234;
            latch.countDown();
        }).start();
        latch.await();
        return box.plainValue;
    }

    /** Monitor: release of the lock by the writer, acquire by the reader. */
    public static int viaMonitor() throws InterruptedException {
        Box box = new Box();
        Object lock = new Object();
        AtomicInteger phase = new AtomicInteger();
        Thread writer = new Thread(() -> {
            synchronized (lock) {
                box.plainValue = 5;
                phase.set(1);
            }
        });
        writer.start();
        writer.join();
        synchronized (lock) {
            return box.plainValue;
        }
    }
}
