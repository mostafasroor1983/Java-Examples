package module10.cas;

import java.util.concurrent.atomic.AtomicLong;

/** Hand-written CAS loop: read, compute, compareAndSet, retry. This is what incrementAndGet does internally. */
public final class CasCounter {
    private final AtomicLong value = new AtomicLong();
    private final AtomicLong casFailures = new AtomicLong();

    public long increment() {
        while (true) {
            long current = value.get();
            long next = current + 1;
            if (value.compareAndSet(current, next)) {
                return next;
            }
            casFailures.incrementAndGet(); // contention: someone else won, retry
        }
    }

    /** Conditional update: only add if it keeps the value under a cap (a credit limit). */
    public boolean addIfUnder(long delta, long cap) {
        while (true) {
            long current = value.get();
            long next = current + delta;
            if (next > cap) return false;
            if (value.compareAndSet(current, next)) return true;
        }
    }

    public long get() {
        return value.get();
    }

    public long casFailures() {
        return casFailures.get();
    }
}
