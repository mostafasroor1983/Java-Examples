package module18.ch04;

import java.math.BigDecimal;

/**
 * Item 22 - Use interfaces only to define types.
 *
 * <p>The "constant interface" (an interface with only constants that classes implement to avoid qualifying names)
 * leaks implementation details into the public type and is inherited forever by subclasses. Put constants in the class
 * or enum they belong to, or in a noninstantiable utility class, and use static imports if qualifying is tedious.
 */
public final class Item22InterfacesOnlyForTypes {
    private Item22InterfacesOnlyForTypes() {
    }

    /** BAD: constant interface. */
    public interface PaymentConstants {
        int MAX_REFERENCE_LENGTH = 140;
        BigDecimal SEPA_INSTANT_LIMIT = new BigDecimal("100000");
    }

    /** BAD usage: every PaymentLimitsBad is now "a PaymentConstants". */
    public static final class PaymentLimitsBad implements PaymentConstants {
    }

    /** GOOD: constants in a noninstantiable holder (or in the enum/class they describe). */
    public static final class SepaLimits {
        public static final int MAX_REFERENCE_LENGTH = 140;
        public static final BigDecimal INSTANT_LIMIT = new BigDecimal("100000");

        private SepaLimits() {
        }
    }
}
