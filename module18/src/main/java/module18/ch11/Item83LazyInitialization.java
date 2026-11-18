package module18.ch11;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Item 83 - Use lazy initialization judiciously.
 *
 * <p>Default to normal (eager) initialisation. Lazy-initialise only when a field is expensive and often unused, and
 * measure. Idioms: for a STATIC field use the lazy initialization holder class (the JVM initialises the holder on
 * first access, thread-safely, with no synchronization afterwards); for an INSTANCE field use double-check with a
 * {@code volatile} field and a local variable (read the volatile once on the fast path).
 */
public final class Item83LazyInitialization {
    public static final AtomicInteger HOLDER_INITIALISATIONS = new AtomicInteger();

    private Item83LazyInitialization() {
    }

    /** Static field: holder class idiom. */
    public static Map<String, String> bicDirectory() {
        return BicDirectoryHolder.DIRECTORY;
    }

    private static final class BicDirectoryHolder {
        static final Map<String, String> DIRECTORY = load();

        private static Map<String, String> load() {
            HOLDER_INITIALISATIONS.incrementAndGet();
            return Map.of("DEUTDEFF", "Deutsche Bank", "BNPAFRPP", "BNP Paribas");
        }
    }

    /** Instance field: double-check idiom. */
    public static final class FraudModel {
        private final AtomicInteger loads = new AtomicInteger();
        private volatile double[] weights;

        public double[] weights() {
            double[] result = weights; // read the volatile only once on the fast path
            if (result == null) {
                synchronized (this) {
                    result = weights;
                    if (result == null) {
                        loads.incrementAndGet();
                        weights = result = new double[]{0.4, 0.35, 0.25};
                    }
                }
            }
            return result;
        }

        public int loads() {
            return loads.get();
        }
    }
}
