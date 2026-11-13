package module02.records;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Records give you equals/hashCode/toString for free, but they are only shallowly immutable:
 * copy mutable components in the compact constructor.
 */
public record Payment(String id, BigDecimal amount, String currency, List<String> tags) {
    public Payment {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        tags = List.copyOf(tags); // defensive copy, also rejects nulls
    }

    /** Static factory for the common case. */
    public static Payment of(String id, String amount, String currency) {
        return new Payment(id, new BigDecimal(amount), currency, List.of());
    }

    public Payment withTag(String tag) {
        var copy = new java.util.ArrayList<>(tags);
        copy.add(tag);
        return new Payment(id, amount, currency, copy);
    }
}
