package module18.ch10;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Item 74 - Document all exceptions thrown by each method.
 *
 * <p>Declare checked exceptions individually (never {@code throws Exception}) and document EVERY exception, checked
 * or unchecked, with {@code @throws} and the condition that triggers it. Do not put unchecked exceptions in the
 * {@code throws} clause; the Javadoc tag is enough. If many methods throw the same exception for the same reason,
 * document it once at class level.
 *
 * <p>Class-level: every method throws {@link NullPointerException} if an argument is null.
 */
public final class Item74DocumentExceptions {
    private Item74DocumentExceptions() {
    }

    /** Thrown when a withdrawal would exceed the daily ATM limit. */
    public static final class DailyLimitExceededException extends Exception {
        @java.io.Serial
        private static final long serialVersionUID = 1L; // Item 86: always explicit

        public DailyLimitExceededException(BigDecimal limit) {
            super("daily ATM limit " + limit + " exceeded");
        }
    }

    /**
     * Authorises an ATM withdrawal.
     *
     * @param requested        amount requested; must be positive and a multiple of 10
     * @param withdrawnToday   amount already withdrawn today; must be {@code >= 0}
     * @param dailyLimit       the customer's daily limit
     * @return the remaining limit after this withdrawal
     * @throws DailyLimitExceededException if {@code withdrawnToday + requested > dailyLimit} (checked: show a message)
     * @throws IllegalArgumentException    if {@code requested} is not positive or not a multiple of 10
     */
    public static BigDecimal authoriseAtm(BigDecimal requested, BigDecimal withdrawnToday, BigDecimal dailyLimit)
            throws DailyLimitExceededException {
        Objects.requireNonNull(requested);
        Objects.requireNonNull(withdrawnToday);
        Objects.requireNonNull(dailyLimit);
        if (requested.signum() <= 0 || requested.remainder(BigDecimal.TEN).signum() != 0) {
            throw new IllegalArgumentException("ATM amounts must be positive multiples of 10");
        }
        BigDecimal after = withdrawnToday.add(requested);
        if (after.compareTo(dailyLimit) > 0) throw new DailyLimitExceededException(dailyLimit);
        return dailyLimit.subtract(after);
    }
}
