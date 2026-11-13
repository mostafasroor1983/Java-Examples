package module01.exercise2;

import java.math.BigDecimal;

/** Strategy: how to compute a fee for an amount. */
@FunctionalInterface
public interface FeeStrategy {
    BigDecimal fee(BigDecimal amount);
}
