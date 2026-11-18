package module18.ch03;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;

/**
 * Item 14 - Consider implementing Comparable.
 *
 * <p>A value type with a natural order (amounts, value dates) should implement Comparable so it works with sorting,
 * TreeMap, binary search and min/max. Build comparators with {@link Comparator#comparing} chains rather than by
 * hand, and never compare by subtraction ({@code a - b} overflows for large longs and gives wrong signs).
 * compareTo should be consistent with equals; BigDecimal is the famous exception (1.0 vs 1.00), so be careful when
 * putting BigDecimals in a TreeSet versus a HashSet.
 */
public final class Item14Comparable {
    private Item14Comparable() {
    }

    public record Transaction(String id, LocalDate valueDate, BigDecimal amount) implements Comparable<Transaction> {
        private static final Comparator<Transaction> NATURAL =
                Comparator.comparing(Transaction::valueDate)
                        .thenComparing(Transaction::amount, Comparator.reverseOrder())
                        .thenComparing(Transaction::id);

        @Override
        public int compareTo(Transaction o) {
            return NATURAL.compare(this, o);
        }
    }

    /** BAD: subtraction comparator overflows. */
    public static final Comparator<Long> BROKEN_BY_SUBTRACTION = (a, b) -> (int) (a - b);
    /** GOOD */
    public static final Comparator<Long> SAFE = Long::compare;
}
