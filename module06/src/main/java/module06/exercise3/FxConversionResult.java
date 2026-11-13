package module06.exercise3;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Exercise 3: convert a multi-currency list of balances to a reporting currency.
 * Missing rates are collected as failures instead of aborting the whole batch.
 */
public final class FxConversionResult {
    public record Balance(String accountId, String currency, BigDecimal amount) {
    }

    public record Converted(String accountId, String fromCurrency, BigDecimal original, BigDecimal converted) {
    }

    public record Outcome(List<Converted> converted, List<Balance> failed, BigDecimal total) {
    }

    private final Map<String, BigDecimal> ratesToReporting; // currency -> rate to reporting currency
    private final String reportingCurrency;

    public FxConversionResult(String reportingCurrency, Map<String, BigDecimal> ratesToReporting) {
        this.reportingCurrency = Objects.requireNonNull(reportingCurrency);
        this.ratesToReporting = Map.copyOf(ratesToReporting);
    }

    public Optional<BigDecimal> rate(String currency) {
        if (currency.equals(reportingCurrency)) return Optional.of(BigDecimal.ONE);
        return Optional.ofNullable(ratesToReporting.get(currency));
    }

    public Outcome convertAll(List<Balance> balances) {
        Map<Boolean, List<Balance>> split = balances.stream()
                .collect(Collectors.partitioningBy(b -> rate(b.currency()).isPresent()));

        List<Converted> converted = split.get(true).stream()
                .map(b -> new Converted(b.accountId(), b.currency(), b.amount(),
                        b.amount().multiply(rate(b.currency()).orElseThrow()).setScale(2, RoundingMode.HALF_EVEN)))
                .collect(Collectors.toList());

        BigDecimal total = converted.stream().map(Converted::converted).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Outcome(converted, List.copyOf(split.get(false)), total);
    }

    /** Per-currency subtotal in reporting currency. */
    public Map<String, BigDecimal> totalsByOriginalCurrency(List<Balance> balances) {
        return convertAll(balances).converted().stream()
                .collect(Collectors.groupingBy(Converted::fromCurrency, java.util.TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Converted::converted, BigDecimal::add)));
    }
}
