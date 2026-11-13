package module05.repository;

import java.math.BigDecimal;
import java.util.Objects;

public record Payment(PaymentId id, String customerId, BigDecimal amount, Status status) implements Entity<PaymentId> {
    public enum Status { PENDING, SETTLED, FAILED }

    public Payment {
        Objects.requireNonNull(id);
        Objects.requireNonNull(customerId);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(status);
        if (amount.signum() <= 0) throw new IllegalArgumentException("amount must be positive");
    }

    public Payment withStatus(Status s) {
        return new Payment(id, customerId, amount, s);
    }
}
