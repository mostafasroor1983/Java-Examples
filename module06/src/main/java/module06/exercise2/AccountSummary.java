package module06.exercise2;

import module06.basics.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Exercise 2: monthly statement summary for one account: opening, credits, debits, closing,
 * plus spend per category and the busiest day.
 */
public final class AccountSummary {
    public record Summary(String accountId, YearMonth month, BigDecimal opening, BigDecimal credits, BigDecimal debits,
                          BigDecimal closing, Map<Transaction.Category, BigDecimal> spendByCategory, Optional<LocalDate> busiestDay) {
    }

    private AccountSummary() {
    }

    public static Summary summarise(String accountId, YearMonth month, BigDecimal opening, List<Transaction> allTxs) {
        List<Transaction> txs = allTxs.stream()
                .filter(t -> t.accountId().equals(accountId))
                .filter(t -> YearMonth.from(t.date()).equals(month))
                .collect(Collectors.toList());

        BigDecimal credits = txs.stream().filter(t -> !t.isDebit()).map(Transaction::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal debits = txs.stream().filter(Transaction::isDebit).map(t -> t.amount().abs()).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Transaction.Category, BigDecimal> byCategory = txs.stream()
                .filter(Transaction::isDebit)
                .collect(Collectors.groupingBy(Transaction::category, TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, t -> t.amount().abs(), BigDecimal::add)));

        Optional<LocalDate> busiest = txs.stream()
                .collect(Collectors.groupingBy(Transaction::date, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.<LocalDate, Long>comparingByValue()
                        .thenComparing(Map.Entry::getKey, java.util.Comparator.reverseOrder())) // tie -> earliest day
                .map(Map.Entry::getKey);

        return new Summary(accountId, month, opening, credits, debits, opening.add(credits).subtract(debits), byCategory, busiest);
    }
}
