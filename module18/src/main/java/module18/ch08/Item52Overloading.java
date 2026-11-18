package module18.ch08;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Item 52 - Use overloading judiciously.
 *
 * <p>Overload resolution happens at COMPILE time on static types, while overriding dispatches at runtime. A
 * {@code Collection<?>} variable holding a Set still picks {@code classify(Collection)}. And {@code List<Integer>}'s
 * {@code remove(int index)} vs {@code remove(Object)} has bitten many: {@code ids.remove(1)} removes the element AT
 * index 1. Avoid overloads with the same number of parameters; give methods different names
 * ({@code removeAt}, {@code removeValue}) instead.
 */
public final class Item52Overloading {
    private Item52Overloading() {
    }

    public static String classify(Set<?> s) {
        return "set";
    }

    public static String classify(List<?> l) {
        return "list";
    }

    public static String classify(Collection<?> c) {
        return "collection";
    }

    /** GOOD: one method that checks at runtime, or distinct names. */
    public static String classifyAtRuntime(Collection<?> c) {
        return c instanceof Set ? "set" : c instanceof List ? "list" : "collection";
    }
}
