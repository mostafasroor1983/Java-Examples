package module18.ch10;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Item 77 - Don't ignore exceptions.
 *
 * <p>An empty catch block hides failures: a payment that "succeeded" because the error was swallowed. If ignoring is
 * genuinely right (closing a read-only stream after the data has been read), name the variable {@code ignored} and
 * say WHY in a comment, and preferably still record it (a metric or debug log). Otherwise handle, translate or
 * propagate.
 */
public final class Item77DontIgnoreExceptions {
    public static final AtomicInteger SWALLOWED_CLOSE_FAILURES = new AtomicInteger();

    private Item77DontIgnoreExceptions() {
    }

    /** BAD: notification failure vanishes; nobody knows the customer was never told. */
    public static boolean notifyBad(Runnable sender) {
        try {
            sender.run();
        } catch (RuntimeException e) {
        }
        return true;
    }

    /** GOOD: propagate the failure as a result the caller must look at. */
    public static boolean notifyCustomer(Runnable sender) {
        try {
            sender.run();
            return true;
        } catch (RuntimeException e) {
            return false; // caller schedules a retry
        }
    }

    /** Acceptable ignore: documented, named, counted. */
    public static void closeQuietly(AutoCloseable c) {
        try {
            c.close();
        } catch (Exception ignored) {
            // Read-only resource; the data has already been consumed. Count it so a spike is visible.
            SWALLOWED_CLOSE_FAILURES.incrementAndGet();
        }
    }
}
