package module01.solid.srp.good;

import java.math.BigDecimal;
import java.util.Objects;

/** GOOD: orchestrates collaborators; each collaborator has exactly one reason to change. */
public final class PaymentService {
    private final PaymentValidator validator;
    private final PaymentRepository repository;
    private final Notifier notifier;

    public PaymentService(PaymentValidator validator, PaymentRepository repository, Notifier notifier) {
        this.validator = Objects.requireNonNull(validator);
        this.repository = Objects.requireNonNull(repository);
        this.notifier = Objects.requireNonNull(notifier);
    }

    public void pay(String customerEmail, BigDecimal amount) {
        validator.validate(customerEmail, amount);
        repository.save(customerEmail, amount);
        notifier.paymentReceived(customerEmail, amount);
    }
}
