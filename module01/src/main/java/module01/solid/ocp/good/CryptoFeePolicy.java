package module01.solid.ocp.good;

import java.math.BigDecimal;

public final class CryptoFeePolicy implements FeePolicy {
    private final String type;
    private final BigDecimal flatPerCion;

    public CryptoFeePolicy(String type, BigDecimal flatPerCion) {
        this.type = type;
        this.flatPerCion = flatPerCion;
    }

    @Override
    public boolean supports(String t) {
        return type.equals(t);
    }

    @Override
    public BigDecimal fee(BigDecimal amount) {
        return flatPerCion;
    }
}
