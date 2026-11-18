package module18.ch07;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Item 42 - Prefer lambdas to anonymous classes.
 *
 * <p>Lambdas remove the boilerplate of single-method anonymous classes and make enum constant-specific behaviour
 * compact (a lambda passed to the constructor). Keep lambdas to a few lines; if it needs a name or documentation,
 * make it a method. Anonymous classes are still needed for abstract classes, multiple methods, or when {@code this}
 * must refer to the function object itself.
 */
public final class Item42Lambdas {
    private Item42Lambdas() {
    }

    public record Payment(String id, BigDecimal amount) { }

    /** Anonymous class (old style). */
    public static void sortByAmountAnonymous(List<Payment> payments) {
        payments.sort(new Comparator<Payment>() {
            @Override
            public int compare(Payment a, Payment b) {
                return a.amount().compareTo(b.amount());
            }
        });
    }

    /** Lambda / comparator construction method. */
    public static void sortByAmount(List<Payment> payments) {
        payments.sort(Comparator.comparing(Payment::amount));
    }

    /** Enum with behaviour supplied as a lambda per constant. */
    public enum RoundingPolicy {
        BANKERS(a -> a.setScale(2, RoundingMode.HALF_EVEN)),
        HOUSE_NEVER_LOSES(a -> a.setScale(2, RoundingMode.DOWN)),
        CUSTOMER_FRIENDLY(a -> a.setScale(2, RoundingMode.UP));

        private final UnaryOperator<BigDecimal> op;

        RoundingPolicy(UnaryOperator<BigDecimal> op) {
            this.op = op;
        }

        public BigDecimal round(BigDecimal amount) {
            return op.apply(amount);
        }
    }
}
