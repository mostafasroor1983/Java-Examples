package module18.ch08;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Item 55 - Return optionals judiciously.
 *
 * <p>{@link Optional} is a good return type for "may be absent" results (lookups, max of a possibly empty list): it
 * forces the caller to decide ({@code orElse}, {@code orElseThrow}, {@code map}). Do NOT: return Optional of a
 * collection (return an empty collection), use Optional for fields, parameters or map values, return null from an
 * Optional-returning method, or box primitives ({@link OptionalLong}). Prefer {@code orElseThrow()} (Java 10) to
 * {@code get()}.
 */
public final class Item55Optionals {
    private static final Map<String, BigDecimal> LIMITS = Map.of("ACC-1", new BigDecimal("500"));

    private Item55Optionals() {
    }

    public static Optional<BigDecimal> overdraftLimit(String account) {
        return Optional.ofNullable(LIMITS.get(account));
    }

    public static BigDecimal effectiveLimit(String account) {
        return overdraftLimit(account).orElse(BigDecimal.ZERO);
    }

    public static OptionalLong largestCents(List<Long> cents) {
        return cents.stream().mapToLong(Long::longValue).max();
    }
}
