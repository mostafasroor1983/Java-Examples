package module01.composition;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Composition over inheritance: the engine HAS-A list of checks.
 * Adding a rule means adding an object, not a subclass.
 */
public final class FraudEngine {
    private final List<FraudCheck> checks;

    public FraudEngine(List<FraudCheck> checks) {
        this.checks = List.copyOf(Objects.requireNonNull(checks, "checks"));
    }

    public FraudDecision evaluate(String customerId, BigDecimal amount, String country) {
        List<String> reasons = new ArrayList<>();
        for (FraudCheck check : checks) {
            String reason = check.reject(customerId, amount, country);
            if (reason != null) {
                reasons.add(reason);
            }
        }
        return new FraudDecision(reasons.isEmpty(), List.copyOf(reasons));
    }

    public record FraudDecision(boolean approved, List<String> reasons) {
    }
}
