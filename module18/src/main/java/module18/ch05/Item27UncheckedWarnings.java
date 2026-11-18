package module18.ch05;

import java.util.Arrays;

/**
 * Item 27 - Eliminate unchecked warnings.
 *
 * <p>Every unchecked warning is a potential ClassCastException. Eliminate as many as possible; when one is provably
 * safe, suppress it on the SMALLEST scope (a single local variable declaration, never the whole class) and write a
 * comment explaining why it is safe.
 */
public final class Item27UncheckedWarnings {
    private Item27UncheckedWarnings() {
    }

    /** A growable buffer of settlement entries backed by Object[]. */
    public static final class Buffer<E> {
        private Object[] items = new Object[2];
        private int size;

        public void add(E e) {
            if (size == items.length) items = Arrays.copyOf(items, size * 2);
            items[size++] = e;
        }

        public E get(int i) {
            java.util.Objects.checkIndex(i, size);
            // Safe: add(E) is the only way elements enter the array, so every element is an E.
            @SuppressWarnings("unchecked") E e = (E) items[i];
            return e;
        }

        public int size() {
            return size;
        }
    }
}
