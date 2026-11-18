package module18.ch09;

import java.util.Comparator;

/**
 * Item 61 - Prefer primitive types to boxed primitives.
 *
 * <p>Boxed primitives have identity ({@code ==} compares references, correct only by accident inside the Integer cache
 * -128..127), can be null (auto-unboxing throws NPE), and allocate. Use primitives except in collections and type
 * parameters, and never apply {@code ==} to two boxed values.
 */
public final class Item61PrimitivesOverBoxed {
    private Item61PrimitivesOverBoxed() {
    }

    /** BAD: == on Integer compares identity. */
    public static final Comparator<Integer> BROKEN = (a, b) -> a < b ? -1 : (a == b ? 0 : 1);

    public static final Comparator<Integer> FIXED = Comparator.naturalOrder();

    private static Integer riskScore; // null by default

    /** BAD: unboxing a null Integer throws NullPointerException. */
    public static boolean isHighRisk() {
        return riskScore > 80;
    }
}
