package module01.ddd;

import module01.immutability.Money;

import java.util.Objects;

/**
 * Dual-control ("four eyes") policy for outgoing payments. The thresholds are business configuration,
 * so they live in a value object instead of being hard-coded inside the aggregate.
 *
 * <ul>
 *   <li>amount &lt;= autoApproveLimit: no human approval (straight-through processing)</li>
 *   <li>amount &lt;= dualControlLimit: one independent checker</li>
 *   <li>above dualControlLimit: two independent checkers</li>
 * </ul>
 */
public record ApprovalPolicy(Money autoApproveLimit, Money dualControlLimit) {
    public ApprovalPolicy {
        Objects.requireNonNull(autoApproveLimit, "autoApproveLimit");
        Objects.requireNonNull(dualControlLimit, "dualControlLimit");
        if (autoApproveLimit.isNegative()) throw new IllegalArgumentException("limits must be >= 0");
        if (autoApproveLimit.compareTo(dualControlLimit) > 0) { // also rejects mixed currencies
            throw new IllegalArgumentException("autoApproveLimit must be <= dualControlLimit");
        }
    }

    public int requiredApprovals(Money amount) {
        if (amount.compareTo(autoApproveLimit) <= 0) return 0;
        if (amount.compareTo(dualControlLimit) <= 0) return 1;
        return 2;
    }
}
