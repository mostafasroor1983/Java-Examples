package module01.ddd;

import module01.immutability.Money;

import java.time.Instant;

/**
 * Immutable domain events raised by {@link PaymentInstruction}. Sealed so that every consumer
 * (audit log, notification, outbox relay) can switch over them exhaustively.
 * Events carry identifiers and facts, never secrets (no full IBAN, no card number).
 */
public sealed interface PaymentEvent {
    String paymentId();

    Instant occurredAt();

    record Submitted(String paymentId, UserId maker, Money amount, int approvalsRequired, Instant occurredAt)
            implements PaymentEvent {
    }

    record ApprovalRecorded(String paymentId, UserId checker, int approvalsSoFar, Instant occurredAt)
            implements PaymentEvent {
    }

    record Released(String paymentId, Instant occurredAt) implements PaymentEvent {
    }

    record Rejected(String paymentId, UserId checker, String reason, Instant occurredAt) implements PaymentEvent {
    }
}
