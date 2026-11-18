package module18.ch05;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Item 30 - Favor generic methods.
 *
 * <p>Static utilities should be generic so callers get type inference and no casts. The recursive bound
 * {@code <E extends Comparable<? super E>>} lets {@code max} work for types whose compareTo is inherited from a
 * supertype. A generic singleton factory hands out one stateless instance typed for any T.
 */
public final class Item30GenericMethods {
    private static final UnaryOperator<Object> IDENTITY = t -> t;

    private Item30GenericMethods() {
    }

    public static <E> Set<E> union(Set<? extends E> a, Set<? extends E> b) {
        Set<E> result = new HashSet<>(a);
        result.addAll(b);
        return result;
    }

    public static <E extends Comparable<? super E>> E max(Collection<? extends E> c) {
        Iterator<? extends E> it = c.iterator();
        if (!it.hasNext()) throw new IllegalArgumentException("empty collection");
        E best = it.next();
        while (it.hasNext()) {
            E e = it.next();
            if (e.compareTo(best) > 0) best = e;
        }
        return best;
    }

    @SuppressWarnings("unchecked") // safe: identity returns its argument unchanged, so it is a valid UnaryOperator<T>
    public static <T> UnaryOperator<T> identity() {
        return (UnaryOperator<T>) IDENTITY;
    }
}
