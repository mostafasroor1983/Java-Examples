package explanations.sealed;

import java.util.Objects;

/** Failed payment with a reason and retryability. One top-level type per file (Effective Java item 25). */
record Failure(String paymentId, Failure.Reason reason, boolean retryable) implements PaymentOutcome {
    enum Reason { INSUFFICIENT_FUNDS, FRAUD, PROVIDER_ERROR, INVALID_REQUEST }

    Failure {
        Objects.requireNonNull(paymentId);
        Objects.requireNonNull(reason);
    }
}
