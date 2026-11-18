package module18.ch05;

import java.util.ArrayList;
import java.util.List;

/**
 * Item 26 - Don't use raw types.
 *
 * <p>A raw {@code List} compiles with only a warning, lets anything in, and moves the failure to a distant read site
 * as a ClassCastException. The parameterised type catches the mistake at compile time. If you genuinely do not care
 * about the element type, use the unbounded wildcard {@code List<?>} (you can read Objects, but cannot add).
 * Raw types remain legal only for class literals ({@code List.class}) and {@code instanceof List}.
 */
public final class Item26RawTypes {
    private Item26RawTypes() {
    }

    /** BAD: raw parameter lets a non-String into a List<String>. */
    @SuppressWarnings({"rawtypes", "unchecked"}) // deliberately demonstrating the bug
    public static void addLegacyEntry(List rawRefs) {
        rawRefs.add(42); // compiles: heap pollution
    }

    /** Shows where the failure surfaces: far from the cause. */
    public static String firstReference(List<String> refs) {
        return refs.get(0); // ClassCastException here if polluted
    }

    /** GOOD: unbounded wildcard when the element type is irrelevant. */
    public static int countDistinct(List<?> anything) {
        return (int) anything.stream().distinct().count();
    }

    public static List<String> newRefs() {
        return new ArrayList<>();
    }
}
