package module18.ch07;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Item 46 - Prefer side-effect-free functions in streams.
 *
 * <p>A pipeline whose real work happens in {@code forEach} mutating an outside map is "iterative code wearing a stream
 * costume": harder to read and broken under parallel execution. Express the result with collectors
 * ({@code groupingBy}, {@code counting}, {@code toMap} with merge function, {@code partitioningBy}). Reserve
 * {@code forEach} for reporting the result, not computing it.
 */
public final class Item46SideEffectFreeStreams {
    private Item46SideEffectFreeStreams() {
    }

    /** BAD: computation inside forEach. */
    public static Map<String, Long> declinesByReasonBad(List<String> reasons) {
        Map<String, Long> counts = new HashMap<>();
        reasons.stream().forEach(r -> counts.merge(r, 1L, Long::sum));
        return counts;
    }

    /** GOOD: the collector is the computation. */
    public static Map<String, Long> declinesByReason(List<String> reasons) {
        return reasons.stream().collect(Collectors.groupingBy(r -> r, Collectors.counting()));
    }

    public static Map<Boolean, List<String>> partitionHardDeclines(List<String> reasons) {
        return reasons.stream().collect(Collectors.partitioningBy(r -> r.startsWith("HARD_")));
    }
}
