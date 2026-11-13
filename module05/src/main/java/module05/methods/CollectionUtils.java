package module05.methods;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Generic methods: the type parameter is declared before the return type and inferred at the call site. */
public final class CollectionUtils {
    private CollectionUtils() {
    }

    public static <T> List<T> filter(Collection<? extends T> source, Predicate<? super T> keep) {
        List<T> out = new ArrayList<>();
        for (T t : source) if (keep.test(t)) out.add(t);
        return out;
    }

    public static <T, R> List<R> map(Collection<? extends T> source, Function<? super T, ? extends R> fn) {
        List<R> out = new ArrayList<>(source.size());
        for (T t : source) out.add(fn.apply(t));
        return out;
    }

    public static <T, K> Map<K, List<T>> groupBy(Collection<T> source, Function<? super T, ? extends K> key) {
        return source.stream().collect(Collectors.groupingBy(key));
    }

    /** `T extends Comparable<? super T>` is the canonical bound for "sortable things". */
    public static <T extends Comparable<? super T>> T max(Collection<? extends T> source) {
        if (source.isEmpty()) throw new IllegalArgumentException("empty collection");
        T best = null;
        for (T t : source) if (best == null || t.compareTo(best) > 0) best = t;
        return best;
    }

    public static <T> T maxBy(Collection<? extends T> source, Comparator<? super T> cmp) {
        Objects.requireNonNull(cmp);
        if (source.isEmpty()) throw new IllegalArgumentException("empty collection");
        T best = null;
        for (T t : source) if (best == null || cmp.compare(t, best) > 0) best = t;
        return best;
    }
}
