package module01.solid.ocp.good;

import java.math.BigDecimal;

public final class FlatFeePolicy implements FeePolicy {
    private final String type;
    private final BigDecimal flat;

    public FlatFeePolicy(String type, BigDecimal flat) {
        this.type = type;
        this.flat = flat;
    }

    @Override
    public boolean supports(String t) {
        return type.equals(t);
    }

    @Override
    public BigDecimal fee(BigDecimal amount) {
        return flat;
    }
}
