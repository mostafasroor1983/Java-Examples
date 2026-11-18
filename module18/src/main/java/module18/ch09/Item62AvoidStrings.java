package module18.ch09;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * Item 62 - Avoid strings where other types are more appropriate.
 *
 * <p>Strings are poor substitutes for numbers, enums, aggregate types and capabilities. {@code "EUR:100.00"} has to be
 * parsed everywhere and breaks when a field contains the delimiter; a record parses once and is type-safe. Strings
 * as unforgeable keys (e.g. a ThreadLocal key) can collide between unrelated code.
 */
public final class Item62AvoidStrings {
    private Item62AvoidStrings() {
    }

    /** BAD: compound string. */
    public static BigDecimal amountFromCompound(String compound) {
        return new BigDecimal(compound.split(":")[1]);
    }

    /** GOOD: aggregate type. */
    public record Amount(Currency currency, BigDecimal value) {
        public Amount {
            Objects.requireNonNull(currency);
            Objects.requireNonNull(value);
        }

        public static Amount parse(String compound) {
            int i = compound.indexOf(':');
            if (i != 3) throw new IllegalArgumentException("expected CCY:amount");
            return new Amount(Currency.getInstance(compound.substring(0, 3)), new BigDecimal(compound.substring(4)));
        }
    }
}
