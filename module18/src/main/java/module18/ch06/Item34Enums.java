package module18.ch06;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Item 34 - Use enums instead of int constants.
 *
 * <p>{@code static final int STATUS_SETTLED = 3} has no type safety (any int compiles), no namespace, prints as a
 * number in logs and breaks clients when renumbered. Enums are full classes: type-safe, printable, can carry data and
 * behaviour, and {@code switch} over them is checked. Constant-specific behaviour (here: rounding per card scheme) is
 * written as a field holding a lambda or as constant-specific method bodies.
 */
public final class Item34Enums {
    private Item34Enums() {
    }

    /** BAD: int constants. */
    public static final int SCHEME_VISA = 0;
    public static final int SCHEME_MASTERCARD = 1;

    /** GOOD: data + behaviour per constant. */
    public enum CardScheme {
        VISA("4", new BigDecimal("0.20")),
        MASTERCARD("5", new BigDecimal("0.25")),
        AMEX("3", new BigDecimal("1.50")) {
            @Override
            public BigDecimal interchange(BigDecimal amount) { // constant-specific method body
                return super.interchange(amount).max(new BigDecimal("0.50"));
            }
        };

        private static final Map<String, CardScheme> BY_PREFIX =
                Stream.of(values()).collect(Collectors.toMap(s -> s.prefix, Function.identity()));

        private final String prefix;
        private final BigDecimal interchangePercent;

        CardScheme(String prefix, BigDecimal interchangePercent) {
            this.prefix = prefix;
            this.interchangePercent = interchangePercent;
        }

        public BigDecimal interchange(BigDecimal amount) {
            return amount.multiply(interchangePercent).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
        }

        /** fromString-style lookup returning Optional rather than throwing. */
        public static Optional<CardScheme> fromPan(String pan) {
            return pan.isEmpty() ? Optional.empty() : Optional.ofNullable(BY_PREFIX.get(pan.substring(0, 1)));
        }
    }
}
