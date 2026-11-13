package module06.pitfalls;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Mistakes that pass code review and fail in production. */
public final class StreamPitfalls {
    private StreamPitfalls() {
    }

    /** BAD: double for money. 0.1 + 0.2 != 0.3. */
    public static double totalWithDoubleBad(List<String> amounts) {
        return amounts.stream().mapToDouble(Double::parseDouble).sum();
    }

    /** GOOD */
    public static BigDecimal totalWithBigDecimal(List<String> amounts) {
        return amounts.stream().map(BigDecimal::new).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** BAD: Collectors.toMap with a null value throws NPE (HashMap.merge rejects null). */
    public static Map<String, String> toMapWithNullsBad(List<String> keys) {
        return keys.stream().collect(Collectors.toMap(k -> k, k -> k.startsWith("x") ? null : k));
    }

    /** BAD: Optional.get() without check. GOOD: orElseThrow with message or map/orElse. */
    public static String firstIdBad(List<String> ids) {
        return ids.stream().findFirst().get();
    }

    public static String firstIdGood(List<String> ids) {
        return ids.stream().findFirst().orElseThrow(() -> new IllegalArgumentException("no ids"));
    }

    /** BAD: side effects in map; GOOD: use peek for debugging only, otherwise forEach at the end. */
    public static int sideEffectsInMapBad(List<Integer> values) {
        int[] counter = {0};
        values.stream().map(v -> { counter[0]++; return v; }).count(); // count() may skip map() entirely since Java 9!
        return counter[0];
    }

    /** Streams over a mutable list being modified underneath: undefined; copy first. */
    public static List<Integer> snapshotThenStream(List<Integer> live) {
        return List.copyOf(live).stream().filter(v -> v > 0).collect(Collectors.toList());
    }

    /** Overusing streams for trivial loops hurts readability; this is fine as a loop. */
    public static Optional<Integer> indexOf(List<String> ids, String id) {
        for (int i = 0; i < ids.size(); i++) if (ids.get(i).equals(id)) return Optional.of(i);
        return Optional.empty();
    }
}
