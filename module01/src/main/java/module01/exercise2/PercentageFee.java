package module01.exercise2;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PercentageFee implements FeeStrategy {
    private final BigDecimal rate;

    public PercentageFee(BigDecimal rate) {
        if (rate.signum() < 0) {
            throw new IllegalArgumentException("rate must be >= 0");
        }
        this.rate = rate;
    }

    @Override
    public BigDecimal fee(BigDecimal amount) {
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}
