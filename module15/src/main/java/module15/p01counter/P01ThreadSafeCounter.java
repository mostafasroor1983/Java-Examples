package module15.p01counter;

import java.util.concurrent.atomic.AtomicLong;

/**
 * P01 - Thread-safe counter with increment, decrement-not-below-zero and snapshot.
 * Interview angle: explain why `count++` fails, offer synchronized vs CAS, discuss LongAdder for write-heavy use.
 */
public final class P01ThreadSafeCounter {
    private final AtomicLong value = new AtomicLong();

    public long increment() {
        return value.incrementAndGet();
    }

    /** Never goes below zero: CAS loop with a precondition. Returns false when it would. */
    public boolean decrement() {
        while (true) {
            long cur = value.get();
            if (cur == 0) return false;
            if (value.compareAndSet(cur, cur - 1)) return true;
        }
    }

    public long get() {
        return value.get();
    }

    /** Reset returning the previous value (e.g. flush metrics every minute). */
    public long getAndReset() {
        return value.getAndSet(0);
    }
}
