package module01.solid.srp.good;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class InMemoryPaymentRepository implements PaymentRepository {
    private final List<String> rows = new ArrayList<>();

    @Override
    public void save(String customerEmail, BigDecimal amount) {
        rows.add(customerEmail + ":" + amount);
    }

    public List<String> rows() {
        return List.copyOf(rows);
    }
}
