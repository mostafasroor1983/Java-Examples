package module18.ch04;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Item 23 - Prefer class hierarchies to tagged classes.
 *
 * <p>A "tagged class" has a kind field plus fields for every kind, most unused, and a switch in every method. It is
 * verbose, error-prone (forget a case, read the wrong field) and memory-wasteful. Java 17 sealed interfaces + records
 * give each variant exactly its own fields, and Java 21 pattern-matching switch makes handling exhaustive.
 */
public final class Item23ClassHierarchies {
    private Item23ClassHierarchies() {
    }

    /** BAD: tagged class. */
    public static final class TaggedFee {
        public enum Kind { FLAT, PERCENTAGE, TIERED }

        final Kind kind;
        BigDecimal flat;        // FLAT only
        BigDecimal percent;     // PERCENTAGE only
        BigDecimal threshold;   // TIERED only
        BigDecimal low, high;   // TIERED only

        public TaggedFee(Kind kind, BigDecimal a, BigDecimal b, BigDecimal c) {
            this.kind = kind;
            switch (kind) {
                case FLAT -> flat = a;
                case PERCENTAGE -> percent = a;
                case TIERED -> { threshold = a; low = b; high = c; }
            }
        }

        public BigDecimal feeFor(BigDecimal amount) {
            return switch (kind) {
                case FLAT -> flat;
                case PERCENTAGE -> amount.multiply(percent).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
                case TIERED -> amount.compareTo(threshold) <= 0 ? low : high;
            };
        }
    }

    /** GOOD: sealed hierarchy (17) + exhaustive pattern switch (21). */
    public sealed interface Fee permits Flat, Percentage, Tiered { }

    public record Flat(BigDecimal amount) implements Fee { }

    public record Percentage(BigDecimal percent) implements Fee { }

    public record Tiered(BigDecimal threshold, BigDecimal low, BigDecimal high) implements Fee { }

    public static BigDecimal feeFor(Fee fee, BigDecimal amount) {
        return switch (fee) {
            case Flat(var f) -> f;
            case Percentage(var p) -> amount.multiply(p).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
            case Tiered(var t, var low, var high) -> amount.compareTo(t) <= 0 ? low : high;
        };
    }
}
