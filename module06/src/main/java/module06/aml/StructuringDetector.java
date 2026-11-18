package module06.aml;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Anti-money-laundering rule: "structuring" (smurfing) is splitting cash deposits so each one stays just below a
 * reporting threshold (e.g. 10,000). Flag an account/day when at least {@code minCount} deposits fall in the band
 * [threshold x (1 - band), threshold) AND their total reaches the threshold.
 *
 * <p>Stream techniques: composite grouping key as a record (not string concatenation), {@code filtering} applied
 * before grouping vs after, {@code collectingAndThen}, and {@link Optional} used only as a return type.
 *
 * <p>Real AML engines run such rules over rolling windows across channels and feed a case-management queue.
 * An alert is a signal for a human analyst, not an automatic block, and alert data is sensitive (tipping-off rules).
 */
public final class StructuringDetector {
    public record Deposit(String accountId, LocalDate date, BigDecimal amount, boolean cash) {
        public Deposit {
            Objects.requireNonNull(accountId);
            Objects.requireNonNull(date);
            Objects.requireNonNull(amount);
        }
    }

    public record AccountDay(String accountId, LocalDate date) {
    }

    public record Alert(AccountDay key, long count, BigDecimal total) {
    }

    private final BigDecimal threshold;
    private final BigDecimal lowerBound;
    private final int minCount;

    public StructuringDetector(BigDecimal threshold, BigDecimal band, int minCount) {
        if (threshold.signum() <= 0 || band.signum() <= 0 || band.compareTo(BigDecimal.ONE) >= 0 || minCount < 2) {
            throw new IllegalArgumentException("invalid rule parameters");
        }
        this.threshold = threshold;
        this.lowerBound = threshold.multiply(BigDecimal.ONE.subtract(band));
        this.minCount = minCount;
    }

    private boolean justBelowThreshold(Deposit d) {
        return d.cash() && d.amount().compareTo(lowerBound) >= 0 && d.amount().compareTo(threshold) < 0;
    }

    /** Alerts sorted by total descending (analysts look at the largest first), then by key for stability. */
    public List<Alert> scan(List<Deposit> deposits) {
        Map<AccountDay, List<Deposit>> suspicious = deposits.stream()
                .filter(this::justBelowThreshold)
                .collect(Collectors.groupingBy(d -> new AccountDay(d.accountId(), d.date())));

        return suspicious.entrySet().stream()
                .map(e -> new Alert(e.getKey(), e.getValue().size(),
                        e.getValue().stream().map(Deposit::amount).reduce(BigDecimal.ZERO, BigDecimal::add)))
                .filter(a -> a.count() >= minCount && a.total().compareTo(threshold) >= 0)
                .sorted(Comparator.comparing(Alert::total).reversed()
                        .thenComparing(a -> a.key().accountId())
                        .thenComparing(a -> a.key().date()))
                .toList();
    }

    /** Optional as a return type for "maybe absent" - never as a field or parameter. */
    public Optional<Alert> worstFor(String accountId, List<Deposit> deposits) {
        return scan(deposits).stream().filter(a -> a.key().accountId().equals(accountId)).findFirst();
    }
}
