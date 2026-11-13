package module08.hierarchy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;

/**
 * Checked exceptions: the caller is expected to handle (file missing, network). Unchecked: programming
 * errors and business rule violations the immediate caller usually cannot fix.
 */
public final class CheckedVsUnchecked {
    private CheckedVsUnchecked() {
    }

    /** Checked: callers must decide what to do when the statement file is unavailable. */
    public static String loadStatement(String path) throws IOException {
        if (path == null || path.isBlank()) throw new IOException("statement path is blank");
        if (path.endsWith(".missing")) throw new java.io.FileNotFoundException(path);
        return "statement:" + path;
    }

    /** Converting checked to unchecked at a boundary (e.g. for use in lambdas). Keep the cause! */
    public static String loadStatementUnchecked(String path) {
        try {
            return loadStatement(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Unchecked: an invalid amount is a programming/validation error, not an expected outcome. */
    public static BigDecimal requirePositive(BigDecimal amount) {
        if (amount == null) throw new NullPointerException("amount");
        if (amount.signum() <= 0) throw new InvalidPaymentException("amount must be positive: " + amount);
        return amount;
    }

    /** Error: do not catch. */
    public static void assertBalanced(BigDecimal debits, BigDecimal credits) {
        if (debits.compareTo(credits) != 0) {
            throw new LedgerCorruptedError("debits " + debits + " != credits " + credits);
        }
    }

    /** Multi-catch and catch ordering: subclasses before superclasses, or it does not compile. */
    public static String classify(Runnable r) {
        try {
            r.run();
            return "ok";
        } catch (InsufficientFundsException | InvalidPaymentException e) {
            return "business:" + e.code();
        } catch (PaymentException e) {
            return "payment:" + e.code();
        } catch (RuntimeException e) {
            return "runtime:" + e.getClass().getSimpleName();
        }
    }
}
