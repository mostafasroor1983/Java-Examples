package module01.solid.ocp.good;

import java.math.BigDecimal;
import java.util.List;

/** GOOD: new fee types are new classes; this calculator never changes.
 * Unknown types fail loudly. */
public final class FeeCalculator {
    private final List<FeePolicy> policies;

    public FeeCalculator(List<FeePolicy> policies) {
        this.policies = List.copyOf(policies);
    }

    public BigDecimal fee(String type, BigDecimal amount) {
        return policies.stream()
                .filter(p -> p.supports(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no fee policy for " + type))
                .fee(amount);
    }



    // This is better with open-close and polymorphism principles.
    public BigDecimal fee(FeePolicy policy, BigDecimal amount) {
        return policy.fee(amount);
    }

}
