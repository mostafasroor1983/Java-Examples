package module18.ch05;

import java.util.ArrayList;
import java.util.List;

/**
 * Item 32 - Combine generics and varargs judiciously.
 *
 * <p>A generic varargs parameter is backed by an array of a non-reifiable type, so it can cause heap pollution.
 * It is safe when the method (1) never stores into the varargs array and (2) never lets the array escape to untrusted
 * code. Then annotate with {@link SafeVarargs} (allowed on static, final and, since Java 9, private methods).
 * Returning the array, as {@link #toArrayUnsafe} does, is the classic failure: the caller gets an {@code Object[]}
 * where it expected {@code String[]}.
 */
public final class Item32GenericsAndVarargs {
    private Item32GenericsAndVarargs() {
    }

    /** SAFE: reads the array only, does not expose it. */
    @SafeVarargs
    public static <T> List<T> flatten(List<? extends T>... lists) {
        List<T> result = new ArrayList<>();
        for (List<? extends T> l : lists) result.addAll(l);
        return result;
    }

    /** UNSAFE: exposes the varargs array. Not annotated, on purpose. */
    @SuppressWarnings("unchecked")
    static <T> T[] toArrayUnsafe(T... args) {
        return args;
    }

    /** Calling the unsafe method from a generic context creates an Object[] at runtime. */
    static <T> T[] pickTwo(T a, T b) {
        return toArrayUnsafe(a, b);
    }

    public static String[] pickTwoStrings(String a, String b) {
        return pickTwo(a, b); // ClassCastException: Object[] cannot be cast to String[]
    }
}
