package module05.erasure;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Type erasure: generics exist only at compile time. Consequences and workarounds.
 */
public final class Erasure {
    private Erasure() {
    }

    /** At runtime both are java.util.ArrayList. */
    public static boolean sameRuntimeClass(List<String> a, List<Integer> b) {
        return a.getClass() == b.getClass();
    }

    /** Heap pollution: the raw-type path lets an Integer into a List<String>. The CCE fires later, at the read site. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List<String> pollute() {
        List<String> strings = new ArrayList<>();
        List raw = strings;
        raw.add(42);
        return strings;
    }

    /** Cannot do `new T[n]`; use a Class token (type token) or Array.newInstance. */
    public static <T> T[] newArray(Class<T> type, int n) {
        @SuppressWarnings("unchecked")
        T[] arr = (T[]) Array.newInstance(type, n);
        return arr;
    }

    /** Typesafe heterogeneous container (Effective Java item 33): the Class token survives erasure. */
    public static final class Favorites {
        private final Map<Class<?>, Object> values = new java.util.HashMap<>();

        public <T> void put(Class<T> type, T value) {
            values.put(Objects.requireNonNull(type), type.cast(value)); // cast guards against raw misuse
        }

        public <T> T get(Class<T> type) {
            return type.cast(values.get(type));
        }
    }

    /** Overloads that erase to the same signature do not compile; this is the legal alternative. */
    public static String describe(List<?> list) {
        if (list.isEmpty()) return "empty";
        Object first = list.get(0);
        return first instanceof String ? "strings" : first instanceof Number ? "numbers" : "other";
    }

    /** Bridge methods: Comparable<Money>.compareTo(Money) gets a synthetic compareTo(Object). */
    public static boolean hasBridgeMethod(Class<?> type, String name) {
        for (var m : type.getDeclaredMethods()) {
            if (m.getName().equals(name) && m.isBridge()) return true;
        }
        return false;
    }
}
