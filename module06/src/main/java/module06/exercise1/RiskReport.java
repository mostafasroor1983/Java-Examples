package module06.exercise1;

import module06.basics.Transaction;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Exercise 1: risk report. For each account: number of debits, largest debit, total debit volume;
 * flag accounts whose debit volume exceeds a threshold, sorted by volume desc.
 */
public final class RiskReport {
    public record AccountRisk(String accountId, long debitCount, BigDecimal largestDebit, BigDecimal debitVolume, boolean flagged) {
    }

    private RiskReport() {
    }

    public static List<AccountRisk> build(List<Transaction> txs, BigDecimal flagThreshold) {
        Map<String, List<Transaction>> byAccount = txs.stream()
                .filter(Transaction::isDebit)
                .collect(Collectors.groupingBy(Transaction::accountId));

        return byAccount.entrySet().stream()
                .map(e -> {
                    List<Transaction> debits = e.getValue();
                    BigDecimal volume = debits.stream().map(t -> t.amount().abs()).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal largest = debits.stream().map(t -> t.amount().abs()).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
                    return new AccountRisk(e.getKey(), debits.size(), largest, volume, volume.compareTo(flagThreshold) > 0);
                })
                .sorted(Comparator.comparing(AccountRisk::debitVolume).reversed().thenComparing(AccountRisk::accountId))
                .collect(Collectors.toList());
    }

    public static List<String> flaggedAccounts(List<Transaction> txs, BigDecimal threshold) {
        return build(txs, threshold).stream().filter(AccountRisk::flagged).map(AccountRisk::accountId).collect(Collectors.toList());
    }
}
