package explanations.sealed;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * A sealed hierarchy with exactly three permitted shapes. The compiler knows the full set, so a switch
 * without `default` is checked for exhaustiveness: add a fourth outcome and every switch stops compiling
 * until it is handled. That is the whole point - the type system carries the business rule.
 */
public sealed interface PaymentOutcome permits Success, Failure, Pending {
    String paymentId();
}

record Success(String paymentId, BigDecimal settled, String authCode, Instant at) implements PaymentOutcome {
    Success {
        Objects.requireNonNull(paymentId);
        Objects.requireNonNull(settled);
        Objects.requireNonNull(authCode);
        Objects.requireNonNull(at);
        if (settled.signum() <= 0) throw new IllegalArgumentException("settled amount must be positive");
    }
}

record Failure(String paymentId, Failure.Reason reason, boolean retryable) implements PaymentOutcome {
    enum Reason { INSUFFICIENT_FUNDS, FRAUD, PROVIDER_ERROR, INVALID_REQUEST }

    Failure {
        Objects.requireNonNull(paymentId);
        Objects.requireNonNull(reason);
    }
}

record Pending(String paymentId, Instant checkAfter) implements PaymentOutcome {
    Pending {
        Objects.requireNonNull(paymentId);
        Objects.requireNonNull(checkAfter);
    }
}
