package module18.ch07;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Item 43 - Prefer method references to lambdas.
 *
 * <p>{@code BigDecimal::add} is shorter and clearer than {@code (a, b) -> a.add(b)}. The five kinds: static
 * ({@code Integer::parseInt}), bound ({@code prefix::concat}), unbound ({@code String::strip}), class constructor
 * ({@code ArrayList::new}) and array constructor ({@code int[]::new}). When the lambda is clearer (short parameter
 * names that explain intent, or a long class name), keep the lambda.
 */
public final class Item43MethodReferences {
    private Item43MethodReferences() {
    }

    public record Posting(String account, BigDecimal amount) { }

    public static Map<String, BigDecimal> balances(List<Posting> postings) {
        Map<String, BigDecimal> result = new HashMap<>();
        for (Posting p : postings) result.merge(p.account(), p.amount(), BigDecimal::add);
        return result;
    }

    public static List<String> normalisedRefs(List<String> raw) {
        return raw.stream().map(String::strip).map(String::toUpperCase).filter(s -> !s.isEmpty()).toList();
    }
}
