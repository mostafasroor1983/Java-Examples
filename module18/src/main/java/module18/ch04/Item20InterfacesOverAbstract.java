package module18.ch04;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Item 20 - Prefer interfaces to abstract classes.
 *
 * <p>A class can implement many interfaces but extend one class, so interfaces are ideal for mixins
 * ({@code Refundable}, {@code Auditable}) and for retrofitting existing classes. Default methods (Java 8) supply
 * convenience behaviour. When implementation help is substantial, pair the interface with a SKELETAL implementation
 * ({@code AbstractPaymentChannel}) that implementors may extend, but are not forced to.
 */
public final class Item20InterfacesOverAbstract {
    private Item20InterfacesOverAbstract() {
    }

    public interface PaymentChannel {
        String name();

        BigDecimal maxAmount();

        /** Default method: built only on other interface methods. */
        default boolean accepts(BigDecimal amount) {
            return amount.signum() > 0 && amount.compareTo(maxAmount()) <= 0;
        }
    }

    public interface Refundable {
        boolean supportsPartialRefund();
    }

    /** Skeletal implementation: handles name/limit plumbing. */
    public abstract static class AbstractPaymentChannel implements PaymentChannel {
        private final String name;
        private final BigDecimal maxAmount;

        protected AbstractPaymentChannel(String name, BigDecimal maxAmount) {
            this.name = Objects.requireNonNull(name);
            this.maxAmount = Objects.requireNonNull(maxAmount);
        }

        @Override public final String name() { return name; }
        @Override public final BigDecimal maxAmount() { return maxAmount; }
    }

    public static final class CardChannel extends AbstractPaymentChannel implements Refundable {
        public CardChannel() {
            super("card", new BigDecimal("10000"));
        }

        @Override
        public boolean supportsPartialRefund() {
            return true;
        }
    }

    /** Implements the interface directly without the skeleton: still possible. */
    public record SepaInstantChannel() implements PaymentChannel {
        public String name() { return "sepa-instant"; }
        public BigDecimal maxAmount() { return new BigDecimal("100000"); }
    }
}
