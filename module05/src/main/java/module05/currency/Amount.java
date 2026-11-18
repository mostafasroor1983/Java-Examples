package module05.currency;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Money with the currency in its TYPE parameter.
 *
 * <pre>{@code
 * Amount<EUR> a = Amount.of("10.00", EUR.INSTANCE);
 * Amount<USD> b = Amount.of("10.00", USD.INSTANCE);
 * a.plus(b);   // does not compile: plus(Amount<EUR>) cannot take Amount<USD>
 * }</pre>
 *
 * The runtime {@code unit} component is still kept (erasure removes {@code C} at runtime), so serialisation,
 * logging and defensive checks still know the currency. The type parameter is a compile-time guard on top,
 * which catches mixing currencies at build time instead of in production.
 *
 * Limitation: it only helps where the currency is known statically. Amounts read from a database or API arrive as
 * {@code Amount<?>} and must be checked at runtime ({@link #as}) before you get a typed value.
 */
public record Amount<C extends CurrencyUnit>(BigDecimal value, C unit) implements Comparable<Amount<C>> {
    public Amount {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(unit, "unit");
        value = value.setScale(unit.scale(), RoundingMode.UNNECESSARY); // rejects sub-unit precision
    }

    public static <C extends CurrencyUnit> Amount<C> of(String value, C unit) {
        return new Amount<>(new BigDecimal(value), unit);
    }

    public Amount<C> plus(Amount<C> other) {
        return new Amount<>(value.add(other.value), unit);
    }

    public Amount<C> minus(Amount<C> other) {
        return new Amount<>(value.subtract(other.value), unit);
    }

    /** Runtime-checked narrowing for amounts that arrive untyped (DB rows, JSON). */
    public static <C extends CurrencyUnit> Amount<C> as(Amount<?> untyped, C expected) {
        if (!untyped.unit().equals(expected)) {
            throw new IllegalArgumentException("expected " + expected.code() + " but got " + untyped.unit().code());
        }
        return new Amount<>(untyped.value(), expected);
    }

    @Override
    public int compareTo(Amount<C> o) {
        return value.compareTo(o.value);
    }

    @Override
    public String toString() {
        return value.toPlainString() + " " + unit.code();
    }
}
