package module18.ch03;

import java.util.Objects;

/**
 * Item 10 - Obey the general contract when overriding equals.
 *
 * <p>Contract: reflexive, symmetric, transitive, consistent, and {@code x.equals(null) == false}.
 * {@link BrokenCaseInsensitiveRef} tries to be "helpful" by also equalling plain Strings: it is not symmetric
 * ({@code ref.equals("abc")} is true but {@code "abc".equals(ref)} is false), so collections behave unpredictably.
 * {@link PaymentReference} is the fix: compare only with its own type and normalise once at construction.
 * Records (Java 16+) generate a correct equals for value types.
 */
public final class Item10EqualsContract {
    private Item10EqualsContract() {
    }

    /** BAD: interoperates with String, breaking symmetry. */
    public static final class BrokenCaseInsensitiveRef {
        private final String value;

        public BrokenCaseInsensitiveRef(String value) {
            this.value = Objects.requireNonNull(value);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof BrokenCaseInsensitiveRef r) return value.equalsIgnoreCase(r.value);
            if (o instanceof String s) return value.equalsIgnoreCase(s); // one-way interoperability
            return false;
        }

        @Override
        public int hashCode() {
            return value.toUpperCase().hashCode();
        }
    }

    /** GOOD: canonical form + same-type comparison. */
    public record PaymentReference(String value) {
        public PaymentReference {
            Objects.requireNonNull(value);
            value = value.strip().toUpperCase(java.util.Locale.ROOT);
        }
    }
}
