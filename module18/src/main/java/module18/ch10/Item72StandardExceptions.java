package module18.ch10;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Item 72 - Favor the use of standard exceptions.
 *
 * <p>Reuse the JDK's well-known exceptions so readers understand them instantly:
 * <ul>
 *   <li>{@code IllegalArgumentException} - bad parameter value (negative amount)</li>
 *   <li>{@code IllegalStateException} - object in the wrong state (capture a cancelled payment)</li>
 *   <li>{@code NullPointerException} - null where prohibited</li>
 *   <li>{@code IndexOutOfBoundsException} - index out of range</li>
 *   <li>{@code UnsupportedOperationException} - operation not supported (refund on a cash payment)</li>
 *   <li>{@code ConcurrentModificationException} - detected illegal concurrent modification</li>
 * </ul>
 * Do not throw Exception, RuntimeException, Throwable or Error directly.
 */
public final class Item72StandardExceptions {
    private Item72StandardExceptions() {
    }

    public enum Status { AUTHORISED, CAPTURED, CANCELLED }

    public static final class CardPayment {
        private Status status = Status.AUTHORISED;
        private final BigDecimal amount;

        public CardPayment(BigDecimal amount) {
            Objects.requireNonNull(amount, "amount");
            if (amount.signum() <= 0) throw new IllegalArgumentException("amount must be positive");
            this.amount = amount;
        }

        public void cancel() {
            status = Status.CANCELLED;
        }

        public void capture() {
            if (status != Status.AUTHORISED) throw new IllegalStateException("cannot capture in status " + status);
            status = Status.CAPTURED;
        }

        public BigDecimal amount() {
            return amount;
        }
    }

    public static final class CashPayment {
        public void refund() {
            throw new UnsupportedOperationException("cash payments are refunded at the counter");
        }
    }

    public static String installment(List<String> schedule, int n) {
        return schedule.get(Objects.checkIndex(n, schedule.size()));
    }
}
