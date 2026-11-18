package module18.ch04;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

/**
 * Item 18 - Favor composition over inheritance.
 *
 * <p>Extending a concrete class you do not control couples you to its self-use. {@code HashSet.addAll} calls
 * {@code add} internally, so a subclass that counts in both methods double-counts: a fraud monitor counting card
 * fingerprints would report 6 for 3 cards. A forwarding wrapper (composition) depends only on the public contract of
 * {@link Set} and works with ANY Set implementation.
 */
public final class Item18Composition {
    private Item18Composition() {
    }

    /** BAD: inheritance from a concrete class with self-use. */
    public static final class CountingHashSet<E> extends HashSet<E> {
        @java.io.Serial
        private static final long serialVersionUID = 1L; // Item 86: always explicit

        private int addCount;

        @Override
        public boolean add(E e) {
            addCount++;
            return super.add(e);
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            addCount += c.size();
            return super.addAll(c); // calls add() for each element -> counted twice
        }

        public int addCount() {
            return addCount;
        }
    }

    /** GOOD: wrapper (decorator) that forwards to any Set. */
    public static final class CountingSet<E> implements Set<E> {
        private final Set<E> delegate;
        private int addCount;

        public CountingSet(Set<E> delegate) {
            this.delegate = Objects.requireNonNull(delegate);
        }

        public int addCount() {
            return addCount;
        }

        @Override public boolean add(E e) { addCount++; return delegate.add(e); }
        @Override public boolean addAll(Collection<? extends E> c) { addCount += c.size(); return delegate.addAll(c); }
        @Override public int size() { return delegate.size(); }
        @Override public boolean isEmpty() { return delegate.isEmpty(); }
        @Override public boolean contains(Object o) { return delegate.contains(o); }
        @Override public Iterator<E> iterator() { return delegate.iterator(); }
        @Override public Object[] toArray() { return delegate.toArray(); }
        @Override public <T> T[] toArray(T[] a) { return delegate.toArray(a); }
        @Override public boolean remove(Object o) { return delegate.remove(o); }
        @Override public boolean containsAll(Collection<?> c) { return delegate.containsAll(c); }
        @Override public boolean retainAll(Collection<?> c) { return delegate.retainAll(c); }
        @Override public boolean removeAll(Collection<?> c) { return delegate.removeAll(c); }
        @Override public void clear() { delegate.clear(); }
        @Override public boolean equals(Object o) { return delegate.equals(o); }
        @Override public int hashCode() { return delegate.hashCode(); }
        @Override public String toString() { return delegate.toString(); }
    }
}
