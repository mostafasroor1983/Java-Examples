package module18.ch04;

import java.math.BigDecimal;

/**
 * Item 19 - Design and document for inheritance or else prohibit it.
 *
 * <p>A class meant for subclassing must document its self-use of overridable methods ({@code @implSpec}) and must NOT
 * call overridable methods from its constructor: the subclass override runs before the subclass's fields are
 * initialised. Otherwise prohibit inheritance: {@code final}, private constructors, or (Java 17) a {@code sealed}
 * hierarchy that lists exactly which subclasses exist.
 *
 * <p>Java 21: {@code javac -Xlint:this-escape} (new in JDK 21) warns about exactly this bug in
 * {@code FeeCalculatorBase}: "possible 'this' escape before subclass is fully initialized".
 */
public final class Item19DesignForInheritance {
    private Item19DesignForInheritance() {
    }

    /** BAD: constructor calls an overridable method. */
    public static class FeeCalculatorBase {
        protected final String description;

        public FeeCalculatorBase() {
            this.description = "fee: " + describeRate(); // runs the SUBCLASS override too early
        }

        protected String describeRate() {
            return "standard";
        }
    }

    public static final class PremiumFeeCalculator extends FeeCalculatorBase {
        private final BigDecimal discount = new BigDecimal("0.50");

        @Override
        protected String describeRate() {
            return "premium discount " + discount; // discount is still null when called from super()
        }

        public String description() {
            return description;
        }
    }

    /** GOOD (Java 17): closed hierarchy, every permitted subtype known and final. */
    public sealed interface FeePolicy permits FlatFee, PercentageFee {
        BigDecimal feeFor(BigDecimal amount);
    }

    public record FlatFee(BigDecimal fee) implements FeePolicy {
        public BigDecimal feeFor(BigDecimal amount) {
            return fee;
        }
    }

    public record PercentageFee(BigDecimal percent) implements FeePolicy {
        public BigDecimal feeFor(BigDecimal amount) {
            return amount.multiply(percent).movePointLeft(2);
        }
    }
}
