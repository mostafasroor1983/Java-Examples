package module06.basics;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** filter / map / flatMap / sorted / distinct / limit / reduce / findFirst / anyMatch. */
public final class StreamBasics {
    private StreamBasics() {
    }

    public static List<Transaction> debitsOver(List<Transaction> txs, BigDecimal threshold) {
        return txs.stream()
                .filter(Transaction::isDebit)
                .filter(t -> t.amount().abs().compareTo(threshold) > 0)
                .sorted(Comparator.comparing(Transaction::amount)) // most negative first
                .collect(Collectors.toList());
    }

    public static Set<String> currenciesUsed(List<Transaction> txs) {
        return txs.stream().map(Transaction::currency).collect(Collectors.toCollection(java.util.TreeSet::new));
    }

    /** reduce with identity: BigDecimal sums must never use double. */
    public static BigDecimal net(List<Transaction> txs) {
        return txs.stream().map(Transaction::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static Optional<Transaction> largestCredit(List<Transaction> txs) {
        return txs.stream().filter(t -> !t.isDebit()).max(Comparator.comparing(Transaction::amount));
    }

    /** flatMap: accounts -> their transactions. */
    public static List<String> allIds(List<List<Transaction>> perAccount) {
        return perAccount.stream().flatMap(List::stream).map(Transaction::id).collect(Collectors.toList());
    }

    public static boolean anyFxToday(List<Transaction> txs, java.time.LocalDate today) {
        return txs.stream().anyMatch(t -> t.category() == Transaction.Category.FX && t.date().equals(today));
    }

    /** Primitive streams avoid boxing. */
    public static int sumOfSquares(int n) {
        return IntStream.rangeClosed(1, n).map(i -> i * i).sum();
    }

    public static List<String> topNAccountsByVolume(List<Transaction> txs, int n) {
        return txs.stream()
                .collect(Collectors.groupingBy(Transaction::accountId,
                        Collectors.reducing(BigDecimal.ZERO, t -> t.amount().abs(), BigDecimal::add)))
                .entrySet().stream()
                .sorted(java.util.Map.Entry.<String, BigDecimal>comparingByValue().reversed()
                        .thenComparing(java.util.Map.Entry.comparingByKey()))
                .limit(n)
                .map(java.util.Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public static Stream<String> idsAsStream(List<Transaction> txs) {
        return txs.stream().map(Transaction::id);
    }
}
