package module18.ch07;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Item 45 - Use streams judiciously.
 *
 * <p>Streams shine for uniform transformations, filtering, grouping and aggregation. Loops win when you need to
 * modify local variables, return early, throw checked exceptions, or access several stages' values at once. Overusing
 * streams produces unreadable pipelines. Pick whichever reads better, name lambda parameters well, extract helper
 * methods, and never use {@code char} streams for text processing. Both versions below produce identical results.
 */
public final class Item45StreamsJudiciously {
    private Item45StreamsJudiciously() {
    }

    public record CardTxn(String merchant, BigDecimal amount) { }

    /** Stream version: natural for grouping + summing. */
    public static Map<String, BigDecimal> spendByMerchant(List<CardTxn> txns) {
        return txns.stream().collect(Collectors.groupingBy(CardTxn::merchant, TreeMap::new,
                Collectors.reducing(BigDecimal.ZERO, CardTxn::amount, BigDecimal::add)));
    }

    /** Loop version: clearer when you need early exit plus state. */
    public static List<CardTxn> untilBudgetExceeded(List<CardTxn> txns, BigDecimal budget) {
        List<CardTxn> out = new ArrayList<>();
        BigDecimal spent = BigDecimal.ZERO;
        for (CardTxn t : txns) {
            spent = spent.add(t.amount());
            if (spent.compareTo(budget) > 0) break; // early exit + mutable local: loop is the right tool
            out.add(t);
        }
        return out;
    }
}
