package module10.jmm;

import java.util.function.Supplier;

/**
 * Lazy initialisation patterns.
 * BAD: double-checked locking without volatile (a reader may see a non-null but not fully initialised object).
 * GOOD: volatile DCL, or the holder idiom, or just synchronized.
 */
public final class DoubleCheckedLocking {
    private DoubleCheckedLocking() {
    }

    public static final class BrokenLazy<T> {
        private T value; // NOT volatile
        private final Supplier<T> factory;

        public BrokenLazy(Supplier<T> factory) {
            this.factory = factory;
        }

        public T get() {
            if (value == null) {                 // first check without lock: may see a reference to a half-built object
                synchronized (this) {
                    if (value == null) value = factory.get();
                }
            }
            return value;
        }
    }

    public static final class VolatileLazy<T> {
        private volatile T value;
        private final Supplier<T> factory;
        private int factoryCalls;

        public VolatileLazy(Supplier<T> factory) {
            this.factory = factory;
        }

        public T get() {
            T local = value;                      // one volatile read on the fast path
            if (local == null) {
                synchronized (this) {
                    local = value;
                    if (local == null) {
                        factoryCalls++;
                        value = local = factory.get();
                    }
                }
            }
            return local;
        }

        public synchronized int factoryCalls() {
            return factoryCalls;
        }
    }

    /** Holder idiom: class init is guaranteed thread-safe and lazy by the JLS. */
    public static final class FeeTableHolder {
        private FeeTableHolder() {
        }

        private static final class Holder {
            static final java.util.Map<String, String> TABLE = java.util.Map.of("CARD", "1.5%", "BANK", "0.20");
        }

        public static java.util.Map<String, String> table() {
            return Holder.TABLE;
        }
    }
}
