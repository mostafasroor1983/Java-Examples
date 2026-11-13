package module01.exercise2;

import java.math.BigDecimal;

public final class FlatFee implements FeeStrategy {
    private final BigDecimal flat;

    public FlatFee(BigDecimal flat) {
        if (flat.signum() < 0) {
            throw new IllegalArgumentException("flat must be >= 0");
        }
        this.flat = flat.setScale(2);
    }

    @Override
    public BigDecimal fee(BigDecimal amount) {
        return flat;
    }
}
