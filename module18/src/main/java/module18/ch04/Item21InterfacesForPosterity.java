package module18.ch04;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Item 21 - Design interfaces for posterity.
 *
 * <p>Adding a default method to a published interface injects behaviour into implementations written before it
 * existed, and the default may violate their invariants. Here {@code postAll} was added to {@code Journal} later; its
 * default calls {@code post} one by one, which breaks an implementation that guarantees ALL-OR-NOTHING batches.
 * Implementations with special guarantees must override such defaults, and interface designers should think hard
 * (and test against several implementations) before releasing.
 */
public final class Item21InterfacesForPosterity {
    private Item21InterfacesForPosterity() {
    }

    public interface Journal {
        void post(String entry);

        List<String> entries();

        /** Added in "v2". Not atomic: a failure halfway leaves a partial batch. */
        default void postAll(Collection<String> batch) {
            for (String e : batch) post(e);
        }
    }

    /** Written for v1: rejects blank entries. Inherits the non-atomic default -> partial batches. */
    public static class StrictJournal implements Journal {
        protected final List<String> entries = new ArrayList<>();

        @Override
        public synchronized void post(String entry) {
            if (entry.isBlank()) throw new IllegalArgumentException("blank entry");
            entries.add(entry);
        }

        @Override
        public synchronized List<String> entries() {
            return List.copyOf(entries);
        }
    }

    /** Fixed: overrides the default to validate first, then append atomically. */
    public static final class AtomicStrictJournal extends StrictJournal {
        @Override
        public synchronized void postAll(Collection<String> batch) {
            for (String e : batch) if (e.isBlank()) throw new IllegalArgumentException("blank entry");
            entries.addAll(batch);
        }
    }
}
