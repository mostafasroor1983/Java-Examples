package module07.lifecycle;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Card transaction lifecycle as a sealed hierarchy of immutable records. Each state carries exactly the data that
 * is valid in that state (an Initiated transaction has no auth code; a Captured one knows how much was captured),
 * so illegal combinations cannot be represented at all. Compare with one mutable class + status enum + nullable fields.
 *
 * <pre>
 * Initiated -> Authorised -> Captured -> Settled -> (Refunded, partial or full)
 *                 |   \-> Reversed (auth released, nothing captured)
 *                 \-> Failed (from Initiated or Authorised)
 * </pre>
 */
public sealed interface TxState {
    String txId();

    record Initiated(String txId, BigDecimal amount) implements TxState {
        public Initiated {
            Objects.requireNonNull(txId);
            if (amount.signum() <= 0) throw new IllegalArgumentException("amount must be positive");
        }
    }

    record Authorised(String txId, BigDecimal authorised, String authCode) implements TxState { }

    record Captured(String txId, BigDecimal authorised, BigDecimal captured) implements TxState { }

    record Settled(String txId, BigDecimal settled, BigDecimal refunded) implements TxState {
        public BigDecimal refundable() {
            return settled.subtract(refunded);
        }
    }

    record Reversed(String txId) implements TxState { }

    record Failed(String txId, String reason) implements TxState { }
}
