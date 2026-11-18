package module18.ch06;

import java.util.Objects;

/**
 * Item 40 - Consistently use the Override annotation.
 *
 * <p>{@code equals(PaymentId other)} looks like an override but is an OVERLOAD: HashSet calls {@code equals(Object)},
 * which is still identity-based, so duplicates slip through deduplication. Writing {@code @Override} on it would have
 * been a compile error. Put {@code @Override} on every method meant to override (including interface methods).
 */
public final class Item40Override {
    private Item40Override() {
    }

    /** BAD: overloads equals instead of overriding it. */
    public static final class OverloadedPaymentId {
        private final String value;

        public OverloadedPaymentId(String value) {
            this.value = value;
        }

        public boolean equals(OverloadedPaymentId other) { // @Override here would not compile
            return value.equals(other.value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }

    /** GOOD */
    public static final class PaymentId {
        private final String value;

        public PaymentId(String value) {
            this.value = Objects.requireNonNull(value);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof PaymentId p && value.equals(p.value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }
}
