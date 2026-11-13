package module01.solid.srp.good;

import java.math.BigDecimal;

public final class PaymentValidator {
    public void validate(String customerEmail, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (customerEmail == null || !customerEmail.contains("@")) {
            throw new IllegalArgumentException("bad email");
        }
    }
}
