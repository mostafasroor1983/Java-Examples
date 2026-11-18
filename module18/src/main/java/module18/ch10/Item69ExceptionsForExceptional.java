package module18.ch10;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Item 69 - Use exceptions only for exceptional conditions.
 *
 * <p>Using exceptions for control flow (looping until ArrayIndexOutOfBounds, catching InsufficientFunds to decide
 * whether to show a button) is slow, hides real bugs (a genuine AIOOBE from a bug is swallowed too) and is unclear.
 * Offer a state-testing method ({@code canDebit}) or a result type ({@code tryDebit} returning Optional) for expected
 * outcomes; reserve exceptions for situations a well-behaved caller cannot anticipate.
 */
public final class Item69ExceptionsForExceptional {
    private Item69ExceptionsForExceptional() {
    }

    /** BAD: terminates the loop via an exception. */
    public static BigDecimal sumBad(List<BigDecimal> amounts) {
        BigDecimal total = BigDecimal.ZERO;
        try {
            int i = 0;
            while (true) total = total.add(amounts.get(i++));
        } catch (IndexOutOfBoundsException e) {
            return total;
        }
    }

    public static final class Wallet {
        private BigDecimal balance;

        public Wallet(BigDecimal balance) {
            this.balance = balance;
        }

        /** State-testing method. */
        public boolean canDebit(BigDecimal amount) {
            return balance.compareTo(amount) >= 0;
        }

        /** Distinguished return value for an expected outcome. */
        public Optional<BigDecimal> tryDebit(BigDecimal amount) {
            if (!canDebit(amount)) return Optional.empty();
            balance = balance.subtract(amount);
            return Optional.of(balance);
        }
    }
}
