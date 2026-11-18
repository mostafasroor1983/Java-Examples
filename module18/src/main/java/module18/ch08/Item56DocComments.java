package module18.ch08;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Item 56 - Write doc comments for all exposed API elements.
 *
 * <p>Every public class, method and field needs a doc comment describing the CONTRACT (what, not how):
 * preconditions ({@code @param}), postconditions ({@code @return}), side effects, thread safety and every exception
 * ({@code @throws}). Use {@code {@code}} for code, {@code @implSpec} for self-use relevant to subclasses, and keep the
 * first sentence a summary. Java 18+ adds {@code {@snippet ...}} for validated code examples (JEP 413); this module
 * targets 21 so it is used below. Run {@code javadoc -Xdoclint} to catch missing tags.
 *
 * <p>Thread safety: this class is stateless and therefore thread-safe.
 */
public final class Item56DocComments {
    private Item56DocComments() {
    }

    /**
     * Converts an amount with a mid-market rate and applies a spread, rounding the result DOWN so the institution
     * never pays out an unpriced fraction.
     *
     * {@snippet :
     * BigDecimal eur = Item56DocComments.convert(new BigDecimal("100.00"), new BigDecimal("1.1000"), 50); // @highlight substring="50"
     * // eur == 109.45 : 100 x 1.1 x (1 - 0.005), rounded down
     * }
     *
     * @param amount    the source amount; must be positive
     * @param midRate   the mid-market rate, units of target per unit of source; must be positive
     * @param spreadBps the spread in basis points, {@code 0..1000}
     * @return the converted amount, scale 2, never greater than the unspread conversion
     * @throws IllegalArgumentException if any argument is outside its documented range
     * @throws NullPointerException     if {@code amount} or {@code midRate} is null
     */
    public static BigDecimal convert(BigDecimal amount, BigDecimal midRate, int spreadBps) {
        if (amount.signum() <= 0 || midRate.signum() <= 0) throw new IllegalArgumentException("amount and rate must be positive");
        if (spreadBps < 0 || spreadBps > 1000) throw new IllegalArgumentException("spread out of range");
        BigDecimal factor = BigDecimal.ONE.subtract(BigDecimal.valueOf(spreadBps, 4));
        return amount.multiply(midRate).multiply(factor).setScale(2, RoundingMode.DOWN);
    }
}
