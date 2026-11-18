package module18.ch04;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Item 16 - In public classes, use accessor methods, not public fields.
 *
 * <p>Public mutable fields give up encapsulation: no validation, no invariants, no thread-safety, no way to change the
 * representation later. Accessors (or a record, whose accessors are generated) keep control. Exposing fields is
 * tolerable only in private or package-private helper classes.
 */
public final class Item16Accessors {
    private Item16Accessors() {
    }

    /** BAD: anyone can set a negative limit or a null currency. */
    public static final class PublicFieldsLimit {
        public BigDecimal dailyLimit;
        public String currency;
    }

    /** GOOD: record accessors + validation. */
    public record DailyLimit(BigDecimal amount, String currency) {
        public DailyLimit {
            Objects.requireNonNull(amount);
            Objects.requireNonNull(currency);
            if (amount.signum() < 0) throw new IllegalArgumentException("limit must be >= 0");
        }
    }
}
