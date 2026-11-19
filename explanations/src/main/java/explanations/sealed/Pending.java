package explanations.sealed;

import java.time.Instant;
import java.util.Objects;

/** Payment awaiting a final status. One top-level type per file (Effective Java item 25). */
record Pending(String paymentId, Instant checkAfter) implements PaymentOutcome {
    Pending {
        Objects.requireNonNull(paymentId);
        Objects.requireNonNull(checkAfter);
    }
}
