package module01.polymorphism;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Card(String maskedPan, String scheme) implements PaymentMethod {
    private static final BigDecimal RATE = new BigDecimal("0.0150");

    @Override
    public BigDecimal fee(BigDecimal amount) {
        return amount.multiply(RATE).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String describe() {
        return scheme + " card " + maskedPan;
    }
}
