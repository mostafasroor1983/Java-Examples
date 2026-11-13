package module07.sealed;

import java.math.BigDecimal;

/** Consumers switch exhaustively over PaymentResult; no instanceof chains, no default branch. */
public final class ResultHandler {
    private ResultHandler() {
    }

    public static String customerMessage(PaymentResult r) {
        return switch (r) {
            case PaymentResult.Success s -> "Paid " + s.settledAmount() + " (auth " + s.authCode() + ")";
            case PaymentResult.Declined d when d.reason() == PaymentResult.DeclineReason.INSUFFICIENT_FUNDS -> "Top up your account and try again";
            case PaymentResult.Declined d -> "Payment declined: " + d.reason();
            case PaymentResult.Pending p -> "Processing, check back after " + p.retryAfter();
            case PaymentResult.Failed f when f.retryable() -> "Temporary problem, retrying";
            case PaymentResult.Failed f -> "Payment failed: " + f.error();
        };
    }

    public static int httpStatus(PaymentResult r) {
        return switch (r) {
            case PaymentResult.Success s -> 200;
            case PaymentResult.Pending p -> 202;
            case PaymentResult.Declined d -> 402;
            case PaymentResult.Failed f -> f.retryable() ? 503 : 500;
        };
    }

    /** Record deconstruction pattern (Java 21). */
    public static BigDecimal settledOrZero(PaymentResult r) {
        if (r instanceof PaymentResult.Success(String id, String auth, BigDecimal amount, var at)) {
            return amount;
        }
        return BigDecimal.ZERO;
    }
}
