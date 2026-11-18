package module18.ch08;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Item 51 - Design method signatures carefully.
 *
 * <p>Choose clear names; avoid long parameter lists (four or fewer), especially of identically typed parameters,
 * which callers transpose without the compiler noticing; prefer interfaces over classes for parameter types; prefer
 * two-element enums to booleans ({@code schedule(x, true, false)} is unreadable). Helper classes / records group
 * parameters that travel together.
 */
public final class Item51MethodSignatures {
    private Item51MethodSignatures() {
    }

    /** BAD: six parameters, three Strings in a row, two booleans. */
    public static String scheduleBad(String from, String to, String reference, BigDecimal amount, boolean urgent, boolean notify) {
        return from + "->" + to + ":" + reference + ":" + amount + ":" + urgent + ":" + notify;
    }

    public enum Priority { NORMAL, URGENT }

    public enum Notification { NONE, SMS_AND_EMAIL }

    /** GOOD: a parameter object with named components and enums instead of booleans. */
    public record PaymentSchedule(String fromAccount, String toAccount, BigDecimal amount, LocalDate executionDate,
                                  Priority priority, Notification notification) {
        public PaymentSchedule {
            Objects.requireNonNull(priority);
            Objects.requireNonNull(notification);
        }
    }

    public static String schedule(PaymentSchedule s) {
        return s.fromAccount() + "->" + s.toAccount() + " " + s.amount() + " on " + s.executionDate()
                + " [" + s.priority() + ", " + s.notification() + "]";
    }
}
