package module18.ch07;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Item 44 - Favor the use of standard functional interfaces.
 *
 * <p>{@code java.util.function} has 43 interfaces; {@code Predicate}, {@code Function}, {@code Supplier},
 * {@code Consumer}, {@code UnaryOperator}, {@code BinaryOperator} and their primitive variants cover most needs and
 * interoperate with the JDK ({@code Predicate.and/negate}). Write a custom {@link FunctionalInterface} only when it
 * adds a meaningful name and contract (here {@code FraudRule} carries a rule id for audit) and annotate it.
 * Avoid boxed variants on hot paths (use {@code IntPredicate}, {@code ToLongFunction}).
 */
public final class Item44StandardFunctionalInterfaces {
    private Item44StandardFunctionalInterfaces() {
    }

    public record Txn(String id, BigDecimal amount, String country, Instant at) { }

    /** Custom interface justified: it has a name/id used in audit trails, and a documented contract. */
    @FunctionalInterface
    public interface FraudRule {
        /** @return a reason when the transaction must be reviewed, or null when it passes */
        String evaluate(Txn txn);
    }

    public static Predicate<Txn> largeAmount(BigDecimal threshold) {
        return t -> t.amount().compareTo(threshold) > 0;
    }

    public static Predicate<Txn> fromCountry(String country) {
        return t -> t.country().equals(country);
    }

    public static List<Txn> flagged(List<Txn> txns, Predicate<Txn> rule) {
        return txns.stream().filter(rule).toList();
    }

    public static final BiFunction<BigDecimal, BigDecimal, BigDecimal> FEE = (amount, pct) -> amount.multiply(pct).movePointLeft(2);

    public static Instant stamp(Supplier<Instant> clock) {
        return clock.get();
    }
}
