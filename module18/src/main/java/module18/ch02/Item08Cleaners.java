package module18.ch02;

import java.lang.ref.Cleaner;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Item 8 - Avoid finalizers and cleaners.
 *
 * <p>Finalizers are deprecated for removal (JEP 421, Java 18) and cleaners run at an unpredictable time, maybe never.
 * Never rely on them to release scarce resources (file handles, DB connections, HSM sessions). The correct pattern is
 * {@link AutoCloseable} + try-with-resources (Item 9). A {@link Cleaner} is acceptable only as a SAFETY NET that also
 * reports the leak, as shown here. The cleaning action must not reference the outer object (it would never become
 * unreachable), so the state lives in a static nested class.
 */
public final class Item08Cleaners {
    private static final Cleaner CLEANER = Cleaner.create();
    public static final AtomicInteger LEAKS_DETECTED = new AtomicInteger();

    private Item08Cleaners() {
    }

    /** A temporary export file for a customer statement. */
    public static final class StatementExport implements AutoCloseable {
        private static final class State implements Runnable {
            boolean open = true;

            @Override
            public void run() { // runs at most once: via close() or via the cleaner
                if (open) {
                    open = false;
                    LEAKS_DETECTED.incrementAndGet(); // only reached if close() was forgotten
                }
            }
        }

        private final State state = new State();
        private final Cleaner.Cleanable cleanable;

        public StatementExport() {
            this.cleanable = CLEANER.register(this, state);
        }

        public boolean isOpen() {
            return state.open;
        }

        @Override
        public void close() {
            state.open = false;   // normal path: no leak counted
            cleanable.clean();    // deregisters; the action sees open == false
        }
    }
}
