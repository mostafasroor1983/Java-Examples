package module07.sealed;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Sealed hierarchy: the set of outcomes is closed, so a switch over it is exhaustive without `default`.
 * Adding a new outcome breaks compilation at every switch - which is exactly what you want.
 */
public sealed interface PaymentResult permits PaymentResult.Success, PaymentResult.Declined, PaymentResult.Pending, PaymentResult.Failed {

    String paymentId();

    record Success(String paymentId, String authCode, BigDecimal settledAmount, Instant settledAt) implements PaymentResult {
        public Success {
            Objects.requireNonNull(paymentId);
            Objects.requireNonNull(authCode);
            Objects.requireNonNull(settledAmount);
            Objects.requireNonNull(settledAt);
        }
    }

    record Declined(String paymentId, DeclineReason reason) implements PaymentResult {
        public Declined {
            Objects.requireNonNull(paymentId);
            Objects.requireNonNull(reason);
        }
    }

    record Pending(String paymentId, Instant retryAfter) implements PaymentResult {
    }

    record Failed(String paymentId, String error, boolean retryable) implements PaymentResult {
    }

    enum DeclineReason { INSUFFICIENT_FUNDS, CARD_BLOCKED, FRAUD_SUSPECTED, LIMIT_EXCEEDED }

    default boolean isFinal() {
        return switch (this) {
            case Success s -> true;
            case Declined d -> true;
            case Pending p -> false;
            case Failed f -> !f.retryable();
        };
    }
}
