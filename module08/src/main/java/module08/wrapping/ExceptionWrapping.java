package module08.wrapping;

import module08.hierarchy.PaymentProviderException;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Wrapping: translate low-level exceptions into domain exceptions at layer boundaries,
 * always preserving the cause chain. Also: suppressed exceptions and the "swallow" anti-pattern.
 */
public final class ExceptionWrapping {
    private ExceptionWrapping() {
    }

    public interface ProviderClient {
        String authorise(String paymentId) throws Exception;
    }

    /** BAD: swallows the exception and returns a sentinel; the failure becomes invisible. */
    public static String authoriseBad(ProviderClient client, String paymentId) {
        try {
            return client.authorise(paymentId);
        } catch (Exception e) {
            return null; // nobody will ever know why
        }
    }

    /** BAD: loses the cause. Stack trace points at this line, root cause gone. */
    public static String authoriseLosesCause(ProviderClient client, String paymentId) {
        try {
            return client.authorise(paymentId);
        } catch (Exception e) {
            throw new PaymentProviderException("provider failed: " + e.getMessage(), null, false);
        }
    }

    /** GOOD: translate, classify retryability, keep the cause. */
    public static String authorise(ProviderClient client, String paymentId) {
        try {
            return client.authorise(paymentId);
        } catch (SocketTimeoutException | TimeoutException e) {
            throw new PaymentProviderException("provider timeout for " + paymentId, e, true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // never swallow interruption
            throw new PaymentProviderException("interrupted while authorising " + paymentId, e, true);
        } catch (RuntimeException e) {
            throw e; // already unchecked and meaningful: do not double-wrap
        } catch (Exception e) {
            throw new PaymentProviderException("provider error for " + paymentId, e, false);
        }
    }

    /** Walk the cause chain to find a specific type (useful in tests and error mappers). */
    public static <T extends Throwable> T findCause(Throwable t, Class<T> type) {
        Throwable cur = t;
        List<Throwable> seen = new ArrayList<>();
        while (cur != null && !seen.contains(cur)) {
            if (type.isInstance(cur)) return type.cast(cur);
            seen.add(cur);
            cur = cur.getCause();
        }
        return null;
    }

    /** Suppressed exceptions: when cleanup fails too, do not lose the primary failure. */
    public static void failWithCleanup(Runnable primary, Runnable cleanup) {
        Throwable primaryFailure = null;
        try {
            primary.run();
        } catch (Throwable t) {
            primaryFailure = t;
            throw t;
        } finally {
            try {
                cleanup.run();
            } catch (Throwable cleanupFailure) {
                if (primaryFailure != null) {
                    primaryFailure.addSuppressed(cleanupFailure);
                } else {
                    throw cleanupFailure;
                }
            }
        }
    }
}
