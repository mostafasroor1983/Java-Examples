package module10.atomics;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

/**
 * Atomic toolbox: AtomicLong (CAS), LongAdder (striped), LongAccumulator (custom op),
 * AtomicReference (immutable value swap), AtomicIntegerArray, getAndUpdate/updateAndGet/accumulateAndGet.
 */
public final class AtomicsDeepDive {
    private AtomicsDeepDive() {
    }

    /** Immutable BigDecimal balance swapped atomically. Functions must be pure: they may run more than once. */
    public static final class AtomicBalance {
        private final AtomicReference<BigDecimal> balance;
        private final AtomicLong retries = new AtomicLong();

        public AtomicBalance(BigDecimal initial) {
            this.balance = new AtomicReference<>(initial);
        }

        public BigDecimal credit(BigDecimal amount) {
            return balance.accumulateAndGet(amount, BigDecimal::add);
        }

        /** Returns false if insufficient; never goes negative, no locks. */
        public boolean tryDebit(BigDecimal amount) {
            while (true) {
                BigDecimal current = balance.get();
                if (current.compareTo(amount) < 0) return false;
                if (balance.compareAndSet(current, current.subtract(amount))) return true;
                retries.incrementAndGet();
            }
        }

        public BigDecimal get() {
            return balance.get();
        }

        public long retries() {
            return retries.get();
        }
    }

    /** Max observed latency with LongAccumulator; the op must be commutative and associative. */
    public static final class MaxLatency {
        private final LongAccumulator max = new LongAccumulator(Math::max, Long.MIN_VALUE);

        public void record(long nanos) {
            max.accumulate(nanos);
        }

        public long get() {
            return max.get();
        }
    }

    /** Per-shard counters without false sharing concerns handled by LongAdder's cells. */
    public static final class RequestStats {
        private final LongAdder total = new LongAdder();
        private final LongAdder errors = new LongAdder();
        private final AtomicIntegerArray statusBuckets = new AtomicIntegerArray(6); // 0..5 -> 0xx..5xx

        public void record(int httpStatus) {
            total.increment();
            if (httpStatus >= 500) errors.increment();
            statusBuckets.incrementAndGet(Math.min(5, Math.max(0, httpStatus / 100)));
        }

        public long total() {
            return total.sum();
        }

        public long errors() {
            return errors.sum();
        }

        public int bucket(int hundreds) {
            return statusBuckets.get(hundreds);
        }
    }

    /** getAndUpdate vs updateAndGet: returns old vs new. */
    public static int[] oldAndNew(AtomicInteger ai) {
        int old = ai.getAndUpdate(x -> x * 2);
        int now = ai.updateAndGet(x -> x + 1);
        return new int[]{old, now};
    }

    /** Sequence generator with wrap-around (e.g. 6-digit reference numbers). */
    public static final class Sequence {
        private final AtomicLong next = new AtomicLong();
        private final long modulus;

        public Sequence(long modulus) {
            if (modulus <= 0) throw new IllegalArgumentException("modulus must be positive");
            this.modulus = modulus;
        }

        public long next() {
            return next.getAndUpdate(n -> (n + 1) % modulus);
        }
    }
}
