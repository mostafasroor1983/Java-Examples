package module03.exercise2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Exercise 2: count payment event types and expose top-N. Three implementations of the count step
 * show the evolution from get/put to merge().
 */
public final class EventFrequencyAnalyzer {
    private final Map<String, Integer> counts = new HashMap<>();

    /** v1: verbose get/put. */
    public void recordV1(String eventType) {
        Objects.requireNonNull(eventType);
        Integer c = counts.get(eventType);
        if (c == null) {
            counts.put(eventType, 1);
        } else {
            counts.put(eventType, c + 1);
        }
    }

    /** v2: getOrDefault. */
    public void recordV2(String eventType) {
        counts.put(eventType, counts.getOrDefault(eventType, 0) + 1);
    }

    /** v3: merge - one call, atomic on ConcurrentHashMap too. */
    public void record(String eventType) {
        counts.merge(Objects.requireNonNull(eventType), 1, Integer::sum);
    }

    public int count(String eventType) {
        return counts.getOrDefault(eventType, 0);
    }

    /** Top-N by count desc, then name asc for determinism. */
    public List<Map.Entry<String, Integer>> topN(int n) {
        if (n < 0) throw new IllegalArgumentException("n must be >= 0");
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
        entries.sort(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey()));
        return entries.subList(0, Math.min(n, entries.size()));
    }

    /** Sorted by name: TreeMap. */
    public TreeMap<String, Integer> sortedByName() {
        return new TreeMap<>(counts);
    }

    /** Insertion-ordered view: LinkedHashMap preserves first-seen order. */
    public static LinkedHashMap<String, Integer> firstSeenOrder(List<String> events) {
        LinkedHashMap<String, Integer> m = new LinkedHashMap<>();
        for (String e : events) m.merge(e, 1, Integer::sum);
        return m;
    }
}
