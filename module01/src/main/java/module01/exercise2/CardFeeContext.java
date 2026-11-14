package module01.exercise2;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Context of the Strategy pattern: holds a strategy and applies tier-based discounts.
 * PREMIUM customers get a 50% discount on the computed fee.
 */
public final class CardFeeContext {
    final Map<CustomerTier, BigDecimal> map = Map.of(CustomerTier.PLUS, new BigDecimal("1.00"),
            CustomerTier.PREMIUM, new BigDecimal("0.50"), CustomerTier.STANDARD, new BigDecimal("1.00"));
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
        fee = fee.multiply(map.getOrDefault(tier, new BigDecimal("0.30")));
        return fee.setScale(2, RoundingMode.HALF_UP);
    }
}
