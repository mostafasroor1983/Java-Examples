package module01.composition;

import java.math.BigDecimal;

/** A single fraud rule. Rules are composed, not inherited. */
@FunctionalInterface
public interface FraudCheck {
    /** @return a reason string if the payment is rejected, or null if it passes. */
    String reject(String customerId, BigDecimal amount, String country);
}
