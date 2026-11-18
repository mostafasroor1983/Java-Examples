package module18.ch02;

import java.util.Arrays;
import java.util.EmptyStackException;

/**
 * Item 7 - Eliminate obsolete object references.
 *
 * <p>A class that manages its own memory (array-backed stack, cache, listener list) must null out slots it no longer
 * uses; otherwise the GC still sees them as reachable and a long-running payment service slowly leaks. Other classic
 * leaks: caches without eviction (use bounded caches / TTLs) and listeners that are registered but never removed.
 */
public final class Item07ObsoleteReferences {
    private Item07ObsoleteReferences() {
    }

    /** Pending-payment stack used by a retry worker. */
    public static final class PendingStack<E> {
        private Object[] elements = new Object[4];
        private int size;

        public void push(E e) {
            if (size == elements.length) elements = Arrays.copyOf(elements, 2 * size + 1);
            elements[size++] = e;
        }

        @SuppressWarnings("unchecked") // only E instances are ever stored
        public E pop() {
            if (size == 0) throw new EmptyStackException();
            E result = (E) elements[--size];
            elements[size] = null; // eliminate the obsolete reference
            return result;
        }

        /** Test hook: true if the slot just above the top is cleared. */
        boolean slotAboveTopIsCleared() {
            return size >= elements.length || elements[size] == null;
        }
    }
}
