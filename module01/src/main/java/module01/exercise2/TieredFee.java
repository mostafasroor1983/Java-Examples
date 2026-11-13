package module01.exercise2;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Percentage with a floor and a cap - common for card acquiring. */
public final class TieredFee implements FeeStrategy {
    private final BigDecimal rate;
    private final BigDecimal min;
    private final BigDecimal max;

    public TieredFee(BigDecimal rate, BigDecimal min, BigDecimal max) {
        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException("min must be <= max");
        }
        this.rate = rate;
        this.min = min;
        this.max = max;
    }

    @Override
    public BigDecimal fee(BigDecimal amount) {
        BigDecimal raw = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        return raw.max(min).min(max).setScale(2, RoundingMode.HALF_UP);
    }
}
