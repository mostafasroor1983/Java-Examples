package module18.ch08;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Item 53 - Use varargs judiciously.
 *
 * <p>Varargs allocate an array per call and accept zero arguments. When at least one argument is required, declare it
 * as a normal parameter followed by the varargs ({@code max(first, rest...)}) so "no arguments" is a compile error
 * rather than a runtime exception. For hot paths, provide fixed-arity overloads for the common counts (as
 * {@code List.of} and {@code EnumSet.of} do).
 */
public final class Item53Varargs {
    private Item53Varargs() {
    }

    /** BAD: zero arguments compile and fail at runtime. */
    public static BigDecimal maxBad(BigDecimal... amounts) {
        if (amounts.length == 0) throw new IllegalArgumentException("at least one amount required");
        BigDecimal max = amounts[0];
        for (BigDecimal a : amounts) max = max.max(a);
        return max;
    }

    /** GOOD: one required argument + optional rest. */
    public static BigDecimal max(BigDecimal first, BigDecimal... rest) {
        BigDecimal max = Objects.requireNonNull(first);
        for (BigDecimal a : rest) max = max.max(a);
        return max;
    }
}
