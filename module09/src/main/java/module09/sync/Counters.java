package module09.sync;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/** Four counters: one broken, three correct in different ways. Tests hammer them with N threads. */
public final class Counters {
    private Counters() {
    }

    public interface Counter {
        void increment();

        long value();
    }

    /** BAD: ++ is read-modify-write; lost updates under contention. */
    public static final class UnsafeCounter implements Counter {
        private long count;

        @Override
        public void increment() {
            count++;
        }

        @Override
        public long value() {
            return count;
        }
    }

    /** BAD: volatile gives visibility, not atomicity. ++ is still three operations. */
    public static final class VolatileCounter implements Counter {
        private volatile long count;

        @Override
        public void increment() {
            count++;
        }

        @Override
        public long value() {
            return count;
        }
    }

    /** GOOD: intrinsic lock makes increment atomic and publishes the value. */
    public static final class SynchronizedCounter implements Counter {
        private long count;

        @Override
        public synchronized void increment() {
            count++;
        }

        @Override
        public synchronized long value() {
            return count;
        }
    }

    /** GOOD: CAS loop inside AtomicLong. */
    public static final class AtomicCounter implements Counter {
        private final AtomicLong count = new AtomicLong();

        @Override
        public void increment() {
            count.incrementAndGet();
        }

        @Override
        public long value() {
            return count.get();
        }
    }

    /** GOOD: striped cells; best for write-heavy counters read rarely. */
    public static final class AdderCounter implements Counter {
        private final LongAdder adder = new LongAdder();

        @Override
        public void increment() {
            adder.increment();
        }

        @Override
        public long value() {
            return adder.sum();
        }
    }
}
