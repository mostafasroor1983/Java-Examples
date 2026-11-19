package explanations.sealed;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/** Settled payment. One top-level type per file (Effective Java item 25, see module18). */
record Success(String paymentId, BigDecimal settled, String authCode, Instant at) implements PaymentOutcome {
    Success {
        Objects.requireNonNull(paymentId);
        Objects.requireNonNull(settled);
        Objects.requireNonNull(authCode);
        Objects.requireNonNull(at);
        if (settled.signum() <= 0) throw new IllegalArgumentException("settled amount must be positive");
    }
}
