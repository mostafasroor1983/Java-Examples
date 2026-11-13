package module05.repository;

import java.math.BigDecimal;
import java.util.List;

/** Specialisation adds domain queries on top of the generic base; nothing is duplicated. */
public final class PaymentRepository extends InMemoryRepository<Payment, PaymentId> {

    public List<Payment> findByCustomer(String customerId) {
        return findWhere(p -> p.customerId().equals(customerId));
    }

    public BigDecimal settledTotal() {
        return findWhere(p -> p.status() == Payment.Status.SETTLED).stream()
                .map(Payment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
