package module18.ch11;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Item 78 - Synchronize access to shared mutable data.
 *
 * <p>Synchronization gives two things: mutual exclusion AND visibility. Without it, a worker may never see a
 * {@code stopRequested = true} written by another thread (the JIT may hoist the read out of the loop), and
 * {@code nextSerial++} loses updates. Fix the flag with {@code volatile} (visibility only), and the counter with
 * {@link AtomicLong} (atomic read-modify-write). Both readers and writers must synchronise. Best of all: do not share
 * mutable data; confine it to one thread or make it immutable.
 */
public final class Item78SynchronizeSharedData {
    private Item78SynchronizeSharedData() {
    }

    public static final class SettlementWorker implements Runnable {
        private volatile boolean stopRequested; // volatile: the worker sees the update
        private final AtomicLong processed = new AtomicLong();

        @Override
        public void run() {
            while (!stopRequested) {
                processed.incrementAndGet();
                Thread.onSpinWait();
            }
        }

        public void requestStop() {
            stopRequested = true;
        }

        public long processed() {
            return processed.get();
        }
    }

    /** BAD: lost updates. */
    public static final class UnsafeSerial {
        private long next;

        public long next() {
            return next++;
        }
    }

    /** GOOD */
    public static final class SafeSerial {
        private final AtomicLong next = new AtomicLong();

        public long next() {
            return next.getAndIncrement();
        }
    }
}
