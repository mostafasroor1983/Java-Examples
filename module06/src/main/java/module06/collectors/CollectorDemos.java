package module06.collectors;

import module06.basics.Transaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/** groupingBy, partitioningBy, toMap (with merge!), joining, teeing, summarizing, custom Collector. */
public final class CollectorDemos {
    private CollectorDemos() {
    }

    public static Map<Transaction.Category, BigDecimal> spendByCategory(List<Transaction> txs) {
        return txs.stream()
                .filter(Transaction::isDebit)
                .collect(Collectors.groupingBy(Transaction::category, TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, t -> t.amount().abs(), BigDecimal::add)));
    }

    public static Map<Boolean, List<Transaction>> debitsVsCredits(List<Transaction> txs) {
        return txs.stream().collect(Collectors.partitioningBy(Transaction::isDebit));
    }

    /** BAD: toMap without a merge function throws IllegalStateException on duplicate keys. */
    public static Map<String, BigDecimal> amountByAccountBad(List<Transaction> txs) {
        return txs.stream().collect(Collectors.toMap(Transaction::accountId, Transaction::amount));
    }

    /** GOOD: merge duplicates. */
    public static Map<String, BigDecimal> amountByAccount(List<Transaction> txs) {
        return txs.stream().collect(Collectors.toMap(Transaction::accountId, Transaction::amount, BigDecimal::add));
    }

    public static String csvIds(List<Transaction> txs) {
        return txs.stream().map(Transaction::id).collect(Collectors.joining(",", "[", "]"));
    }

    /** teeing (Java 12+): two collectors, one pass. */
    public static Optional<BigDecimal> averageAbsAmount(List<Transaction> txs) {
        return txs.stream().collect(Collectors.teeing(
                Collectors.reducing(BigDecimal.ZERO, t -> t.amount().abs(), BigDecimal::add),
                Collectors.counting(),
                (sum, count) -> count == 0 ? Optional.empty()
                        : Optional.of(sum.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_EVEN))));
    }

    public static Map<String, Long> countByAccount(List<Transaction> txs) {
        return txs.stream().collect(Collectors.groupingBy(Transaction::accountId, Collectors.counting()));
    }

    /** Nested grouping: account -> category -> count. */
    public static Map<String, Map<Transaction.Category, Long>> countByAccountAndCategory(List<Transaction> txs) {
        return txs.stream().collect(Collectors.groupingBy(Transaction::accountId,
                Collectors.groupingBy(Transaction::category, Collectors.counting())));
    }

    /** Custom collector: BigDecimal sum (there is no Collectors.summingBigDecimal). */
    public static <T> Collector<T, ?, BigDecimal> summingBigDecimal(Function<? super T, BigDecimal> mapper) {
        return Collector.of(
                () -> new BigDecimal[]{BigDecimal.ZERO},
                (acc, t) -> acc[0] = acc[0].add(mapper.apply(t)),
                (a, b) -> { a[0] = a[0].add(b[0]); return a; },
                acc -> acc[0]);
    }

    public static BinaryOperator<BigDecimal> max() {
        return BigDecimal::max;
    }
}
