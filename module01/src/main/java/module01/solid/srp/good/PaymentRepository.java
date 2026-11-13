package module01.solid.srp.good;

import java.math.BigDecimal;

public interface PaymentRepository {
    void save(String customerEmail, BigDecimal amount);
}
