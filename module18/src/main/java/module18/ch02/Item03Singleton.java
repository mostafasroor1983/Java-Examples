package module18.ch02;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Item 3 - Enforce the singleton property with a private constructor or an enum type.
 *
 * <p>A single-element enum is the simplest robust singleton: the JVM guarantees one instance, thread-safe
 * initialisation, and correct behaviour under serialization and reflection attacks (enum constructors cannot be
 * invoked reflectively). The private-constructor + static final field form is shown for comparison.
 *
 * <p>Caveat for testability: singletons are global state. Prefer injecting the dependency (Item 5) and use a
 * singleton only for genuinely unique, stateless or process-wide things.
 */
public final class Item03Singleton {
    private Item03Singleton() {
    }

    /** Preferred: enum singleton. */
    public enum PaymentReferenceGenerator {
        INSTANCE;

        private final AtomicLong sequence = new AtomicLong();

        public String next() {
            return "PAY-" + sequence.incrementAndGet();
        }
    }

    /** Classic form: private constructor guards against a second instance created reflectively. */
    public static final class LegacyRegistry {
        public static final LegacyRegistry INSTANCE = new LegacyRegistry();
        private static boolean created;

        private LegacyRegistry() {
            synchronized (LegacyRegistry.class) {
                if (created) throw new IllegalStateException("singleton already created");
                created = true;
            }
        }
    }
}
