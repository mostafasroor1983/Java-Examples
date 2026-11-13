package module01.exercise2;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Context of the Strategy pattern: holds a strategy and applies tier-based discounts.
 * PREMIUM customers get a 50% discount on the computed fee.
 */
public final class CardFeeContext {
    static final BigDecimal PREMIUM_DISCOUNT = new BigDecimal("0.50");

    private FeeStrategy strategy;

    public CardFeeContext(FeeStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    public void setStrategy(FeeStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    public BigDecimal calculateFee(BigDecimal amount, CustomerTier tier) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("amount must be >= 0");
        }
        Objects.requireNonNull(tier, "tier");
        BigDecimal fee = strategy.fee(amount);
        if (tier == CustomerTier.PREMIUM) {
            fee = fee.multiply(PREMIUM_DISCOUNT);
        }
        return fee.setScale(2, RoundingMode.HALF_UP);
    }
}
