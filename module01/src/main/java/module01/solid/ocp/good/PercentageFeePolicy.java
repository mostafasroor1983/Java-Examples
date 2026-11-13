package module01.solid.ocp.good;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PercentageFeePolicy implements FeePolicy {
    private final String type;
    private final BigDecimal rate;

    public PercentageFeePolicy(String type, BigDecimal rate) {
        this.type = type;
        this.rate = rate;
    }

    @Override
    public boolean supports(String t) {
        return type.equals(t);
    }

    @Override
    public BigDecimal fee(BigDecimal amount) {
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}
